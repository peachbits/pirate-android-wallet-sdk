package pirate.android.sdk.internal.db.derived

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import pirate.android.sdk.internal.NoBackupContextWrapper
import pirate.android.sdk.internal.db.ReadOnlySupportSqliteOpenHelper
import pirate.android.sdk.internal.ext.tryWarn
import pirate.android.sdk.internal.model.Checkpoint
import pirate.android.sdk.jni.PirateRustBackend
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.type.PirateUnifiedFullViewingKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class DerivedDataDb private constructor(
    zcashNetwork: PirateNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    val accountTable = AccountTable(sqliteDatabase)

    val blockTable = BlockTable(zcashNetwork, sqliteDatabase)

    val transactionTable = TransactionTable(zcashNetwork, sqliteDatabase)

    val allTransactionView = AllTransactionView(zcashNetwork, sqliteDatabase)

    val sentTransactionView = SentTransactionView(zcashNetwork, sqliteDatabase)

    val receivedTransactionView = ReceivedTransactionView(zcashNetwork, sqliteDatabase)

    val sentNotesTable = SentNoteTable(zcashNetwork, sqliteDatabase)

    val receivedNotesTable = ReceivedNoteTable(zcashNetwork, sqliteDatabase)

    suspend fun close() {
        withContext(Dispatchers.IO) {
            sqliteDatabase.close()
        }
    }

    companion object {
        // Database migrations are managed by librustzcash.  This is a hard-coded value to ensure that Android's
        // SqliteOpenHelper is happy
        private const val DATABASE_VERSION = 8

        @Suppress("LongParameterList", "SpreadOperator")
        suspend fun new(
            context: Context,
            rustBackend: PirateRustBackend,
            zcashNetwork: PirateNetwork,
            checkpoint: Checkpoint,
            seed: ByteArray?,
            viewingKeys: List<PirateUnifiedFullViewingKey>
        ): DerivedDataDb {
            rustBackend.initDataDb(seed)

            // TODO [#681]: consider converting these to typed exceptions in the welding layer
            // TODO [#681]: https://github.com/zcash/zcash-android-wallet-sdk/issues/681
            tryWarn(
                "Did not initialize the blocks table. It probably was already initialized.",
                ifContains = "table is not empty"
            ) {
                rustBackend.initBlocksTable(checkpoint)
            }

            tryWarn(
                "Did not initialize the accounts table. It probably was already initialized.",
                ifContains = "table is not empty"
            ) {
                rustBackend.initAccountsTable(*viewingKeys.toTypedArray())
            }

            val database = ReadOnlySupportSqliteOpenHelper.openExistingDatabaseAsReadOnly(
                NoBackupContextWrapper(
                    context,
                    rustBackend.dataDbFile.parentFile!!
                ),
                rustBackend.dataDbFile,
                DATABASE_VERSION
            )

            return DerivedDataDb(zcashNetwork, database)
        }
    }
}
