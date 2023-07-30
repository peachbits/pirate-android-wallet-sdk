package pirate.android.sdk.integration

import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import pirate.android.sdk.DefaultPirateSynchronizerFactory
import pirate.android.sdk.internal.SaplingParamTool
import pirate.android.sdk.internal.db.DatabaseCoordinator
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.util.TestWallet
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test

class PirateSynchronizerFactoryTest {

    @Test
    @SmallTest
    fun testFilePaths() {
        val rustBackend = runBlocking {
            DefaultPirateSynchronizerFactory.defaultPirateRustBackend(
                ApplicationProvider.getApplicationContext(),
                PirateNetwork.Testnet,
                "TestWallet",
                TestWallet.Backups.SAMPLE_WALLET.testnetBirthday,
                SaplingParamTool.new(ApplicationProvider.getApplicationContext())
            )
        }
        assertTrue(
            "Invalid DataDB file",
            rustBackend.dataDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_testnet_${DatabaseCoordinator.DB_DATA_NAME}"
            )
        )
        assertTrue(
            "Invalid CacheDB file",
            rustBackend.cacheDbFile.absolutePath.endsWith(
                "no_backup/co.electricoin.zcash/TestWallet_testnet_${DatabaseCoordinator.DB_CACHE_NAME}"
            )
        )
        assertTrue(
            "Invalid CacheDB params dir",
            rustBackend.saplingParamDir.endsWith(
                "no_backup/co.electricoin.zcash"
            )
        )
    }
}
