package pirate.android.sdk.internal.transaction

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.SmallTest
import pirate.android.sdk.annotation.MaintainedTest
import pirate.android.sdk.annotation.TestPurpose
import pirate.android.sdk.internal.TroubleshootingTwig
import pirate.android.sdk.internal.Twig
import pirate.android.sdk.internal.db.commonDatabaseBuilder
import pirate.android.sdk.internal.db.pending.PiratePendingTransactionDb
import pirate.android.sdk.internal.model.PirateEncodedTransaction
import pirate.android.sdk.internal.service.LightWalletService
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.FirstClassByteArray
import pirate.android.sdk.model.PendingTransaction
import pirate.android.sdk.model.TransactionRecipient
import pirate.android.sdk.model.Arrrtoshi
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.test.ScopedTest
import pirate.android.sdk.test.getAppContext
import pirate.fixture.DatabaseNameFixture
import pirate.fixture.DatabasePathFixture
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.stub
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import java.io.File
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

@MaintainedTest(TestPurpose.REGRESSION)
@RunWith(AndroidJUnit4::class)
@SmallTest
class PiratePersistentTransactionManagerTest : ScopedTest() {

    @Mock
    internal lateinit var mockEncoder: TransactionEncoder

    @Mock
    lateinit var mockService: LightWalletService

    private val pendingDbFile = File(
        DatabasePathFixture.new(),
        DatabaseNameFixture.newDb(name = "PersistentTxMgrTest_Pending.db")
    ).apply {
        assertTrue(parentFile != null)
        parentFile!!.mkdirs()
        assertTrue(parentFile!!.exists())
        createNewFile()
        assertTrue(exists())
    }
    private lateinit var manager: OutboundTransactionManager

    @Before
    fun setup() {
        initMocks()
        deleteDb()
        val db = commonDatabaseBuilder(
            getAppContext(),
            PiratePendingTransactionDb::class.java,
            pendingDbFile
        ).build()
        manager = PiratePersistentTransactionManager(db, PirateNetwork.Mainnet, mockEncoder, mockService)
    }

    private fun deleteDb() {
        pendingDbFile.deleteRecursively()
    }

    private fun initMocks() {
        MockitoAnnotations.openMocks(this)
        mockEncoder.stub {
            onBlocking {
                createTransaction(any(), any(), any(), any())
            }.thenAnswer {
                runBlocking {
                    delay(200)
                    PirateEncodedTransaction(
                        FirstClassByteArray(byteArrayOf(1, 2, 3)),
                        FirstClassByteArray(
                            byteArrayOf(
                                8,
                                9
                            )
                        ),
                        BlockHeight.new(PirateNetwork.Mainnet, 5_000_000)
                    )
                }
            }
        }
    }

    @Test
    fun testAbort() = runBlocking {
        var tx: PiratePendingTransaction? = manager.initSpend(
            Arrrtoshi(1234),
            TransactionRecipient.Address("a"),
            "b",
            Account.DEFAULT
        )
        assertNotNull(tx)
        manager.abort(tx)
        tx = manager.findById(tx.id)
        assertNull(tx, "Transaction was not removed from the DB")
    }

    companion object {
        @BeforeClass
        fun init() {
            Twig.plant(PirateTroubleshootingTwig())
        }
    }
}
