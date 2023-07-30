package pirate.android.sdk.integration.service

import android.os.Build
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import pirate.android.sdk.annotation.MaintainedTest
import pirate.android.sdk.annotation.TestPurpose
import pirate.android.sdk.exception.PirateLightWalletException.PirateChangeServerException.PirateChainInfoNotMatching
import pirate.android.sdk.exception.PirateLightWalletException.PirateChangeServerException.PirateStatusException
import pirate.android.sdk.internal.block.PirateCompactBlockDownloader
import pirate.android.sdk.internal.repository.PirateCompactBlockRepository
import pirate.android.sdk.internal.service.PirateLightWalletGrpcService
import pirate.android.sdk.internal.service.LightWalletService
import pirate.android.sdk.internal.twig
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.LightWalletEndpoint
import pirate.android.sdk.model.Mainnet
import pirate.android.sdk.model.Testnet
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.test.ScopedTest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.Spy

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.N)
@MaintainedTest(TestPurpose.REGRESSION)
@RunWith(AndroidJUnit4::class)
@SmallTest
class ChangeServiceTest : ScopedTest() {

    val network = PirateNetwork.Mainnet
    val lightWalletEndpoint = LightWalletEndpoint.Mainnet
    private val eccEndpoint = LightWalletEndpoint("lightd1.pirate.black", 443, true)

    @Mock
    lateinit var mockBlockStore: PirateCompactBlockRepository
    var mockCloseable: AutoCloseable? = null

    @Spy
    val service = PirateLightWalletGrpcService.new(context, lightWalletEndpoint)

    lateinit var downloader: PirateCompactBlockDownloader
    lateinit var otherService: LightWalletService

    @Before
    fun setup() {
        initMocks()
        downloader = PirateCompactBlockDownloader(service, mockBlockStore)
        otherService = PirateLightWalletGrpcService.new(context, eccEndpoint)
    }

    @After
    fun tearDown() {
        mockCloseable?.close()
    }

    private fun initMocks() {
        mockCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    fun testSanityCheck() {
        // Test the result, only if there is no server communication problem.
        val result = runCatching {
            return@runCatching service.getLatestBlockHeight()
        }.onFailure {
            twig(it)
        }.getOrElse { return }

        assertTrue(result > network.saplingActivationHeight)
    }

    @Test
    fun testCleanSwitch() = runBlocking {
        // Test the result, only if there is no server communication problem.
        val result = runCatching {
            downloader.changeService(otherService)
            return@runCatching downloader.downloadBlockRange(
                BlockHeight.new(network, 900_000)..BlockHeight.new(network, 901_000)
            )
        }.onFailure {
            twig(it)
        }.getOrElse { return@runBlocking }

        assertEquals(1_001, result)
    }

    /**
     * Repeatedly connect to servers and download a range of blocks. Switch part way through and
     * verify that the servers change over, even while actively downloading.
     */
    @Test
    @Ignore("This test is broken")
    fun testSwitchWhileActive() = runBlocking {
        val start = BlockHeight.new(PirateNetwork.Mainnet, 900_000)
        val count = 5
        val differentiators = mutableListOf<String>()
        var initialValue = downloader.getServerInfo().buildUser
        val job = testScope.launch {
            repeat(count) {
                differentiators.add(downloader.getServerInfo().buildUser)
                twig("downloading from ${differentiators.last()}")
                downloader.downloadBlockRange(start..(start + 100 * it))
                delay(10L)
            }
        }
        delay(30)
        testScope.launch {
            downloader.changeService(otherService)
        }
        job.join()
        assertTrue(differentiators.count { it == initialValue } < differentiators.size)
        assertEquals(count, differentiators.size)
    }

    @Test
    fun testSwitchToInvalidServer() = runBlocking {
        var caughtException: Throwable? = null

        downloader.changeService(PirateLightWalletGrpcService.new(context, LightWalletEndpoint("invalid.lightwalletd", 9087, true))) {
            caughtException = it
        }

        // the test can continue only if there is no server communication problem
        if (caughtException is StatusException) {
            twig("Server communication problem while testing.")
            return@runBlocking
        }

        assertNotNull("Using an invalid host should generate an exception.", caughtException)
        assertTrue(
            "Exception was of the wrong type.",
            caughtException is PirateStatusException
        )
    }

    @Test
    fun testSwitchToTestnetFails() = runBlocking {
        var caughtException: Throwable? = null

        downloader.changeService(PirateLightWalletGrpcService.new(context, LightWalletEndpoint.Testnet)) {
            caughtException = it
        }

        // the test can continue only if there is no server communication problem
        if (caughtException is StatusException) {
            twig("Server communication problem while testing.")
            return@runBlocking
        }

        assertNotNull("Using an invalid host should generate an exception.", caughtException)
        assertTrue(
            "Exception was of the wrong type. Expected ${PirateChainInfoNotMatching::class.simpleName} but was ${caughtException!!::class.simpleName}",
            caughtException is PirateChainInfoNotMatching
        )
        (caughtException as PirateChainInfoNotMatching).propertyNames.let { props ->
            arrayOf("saplingActivationHeight", "chainName").forEach {
                assertTrue(
                    "$it should be a non-matching property but properties were [$props]",
                    props.contains(it, true)
                )
            }
        }
    }
}
