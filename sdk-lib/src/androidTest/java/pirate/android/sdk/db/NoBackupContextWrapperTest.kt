package pirate.android.sdk.db

import android.os.Build
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import pirate.android.sdk.internal.NoBackupContextWrapper
import pirate.android.sdk.test.getAppContext
import pirate.fixture.DatabaseNameFixture
import pirate.fixture.DatabasePathFixture
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

@SdkSuppress(minSdkVersion = Build.VERSION_CODES.O_MR1)
class NoBackupContextWrapperTest {

    private val databaseParentDir = File(DatabasePathFixture.new())
    private val noBackupContextWrapper = NoBackupContextWrapper(getAppContext(), databaseParentDir)

    @Test
    @SmallTest
    fun get_context_test() {
        assertTrue(noBackupContextWrapper.applicationContext is NoBackupContextWrapper)
        assertTrue(noBackupContextWrapper.baseContext is NoBackupContextWrapper)
    }

    @Test
    @SmallTest
    fun get_database_path_test() {
        val testDbPath = File(DatabasePathFixture.new(), DatabaseNameFixture.newDb()).absolutePath
        val testDbFile = noBackupContextWrapper.getDatabasePath(testDbPath)

        testDbFile.absolutePath.also {
            assertTrue(it.isNotEmpty())
            assertTrue(it.contains(DatabaseNameFixture.TEST_DB_NAME))
            assertTrue(it.contains(DatabasePathFixture.NO_BACKUP_DIR_PATH))
            assertTrue(it.contains(DatabasePathFixture.INTERNAL_DATABASE_PATH))
        }
    }
}
