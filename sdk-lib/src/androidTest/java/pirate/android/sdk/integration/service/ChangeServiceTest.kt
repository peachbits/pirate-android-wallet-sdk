package pirate.android.sdk.integration.service

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import pirate.android.sdk.annotation.MaintainedTest
import pirate.android.sdk.annotation.TestPurpose
import pirate.android.sdk.internal.Twig
import pirate.android.sdk.internal.block.CompactBlockDownloader
import pirate.android.sdk.internal.repository.CompactBlockRepository
import pirate.android.sdk.model.Mainnet
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.test.ScopedTest
import pirate.lightwallet.client.LightWalletClient
import pirate.lightwallet.client.model.BlockHeightUnsafe
import pirate.lightwallet.client.model.LightWalletEndpoint
import pirate.lightwallet.client.model.Response
import pirate.lightwallet.client.new
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import kotlin.test.Ignore

@MaintainedTest(TestPurpose.REGRESSION)
@RunWith(AndroidJUnit4::class)
@SmallTest
class ChangeServiceTest : ScopedTest() {

    val network = PirateNetwork.Mainnet
    val lightWalletEndpoint = LightWalletEndpoint.Mainnet
    private val eccEndpoint = LightWalletEndpoint("lightd1.pirate.black", 443, true)

    @Mock
    lateinit var mockBlockStore: CompactBlockRepository
    var mockCloseable: AutoCloseable? = null

    val service = LightWalletClient.new(context, lightWalletEndpoint)

    lateinit var downloader: CompactBlockDownloader
    lateinit var otherService: LightWalletClient

    @Before
    fun setup() {
        initMocks()
        downloader = CompactBlockDownloader(service, mockBlockStore)
        otherService = LightWalletClient.new(context, eccEndpoint)
    }

    @After
    fun tearDown() {
        mockCloseable?.close()
    }

    private fun initMocks() {
        mockCloseable = MockitoAnnotations.openMocks(this)
    }

    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    @Ignore
    fun testSanityCheck() = runTest {
        // Test the result, only if there is no server communication problem.
        runCatching {
            service.getLatestBlockHeight()
        }.onFailure {
            Twig.debug(it) { "Failed to retrieve data" }
        }.onSuccess {
            assertTrue(it is Response.Success<BlockHeightUnsafe>)

            assertTrue(
                (it as Response.Success<BlockHeightUnsafe>).result.value > network.saplingActivationHeight
                    .value
            )
        }
    }
}
