package pirate.android.sdk.db

import androidx.test.filters.SmallTest
import pirate.android.sdk.internal.AndroidApiVersion
import pirate.android.sdk.internal.db.PendingTransactionDb
import pirate.android.sdk.test.getAppContext
import pirate.fixture.DatabaseNameFixture
import pirate.fixture.DatabasePathFixture
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.io.File

class CommonDatabaseBuilderTest {

    @Test
    @SmallTest
    fun proper_database_name_used_test() {
        val dbDirectory = File(DatabasePathFixture.new())
        val dbFileName = DatabaseNameFixture.newDb(name = DatabaseCoordinator.DB_PENDING_TRANSACTIONS_NAME)
        val dbFile = File(dbDirectory, dbFileName)

        val db = commonDatabaseBuilder(
            getAppContext(),
            PendingTransactionDb::class.java,
            dbFile
        ).build()

        assertNotNull(db)

        val expectedDbName = if (AndroidApiVersion.isAtLeastO_MR1) {
            dbFileName
        } else {
            dbFile.absolutePath
        }

        assertEquals(expectedDbName, db.openHelper.databaseName)
    }
}
