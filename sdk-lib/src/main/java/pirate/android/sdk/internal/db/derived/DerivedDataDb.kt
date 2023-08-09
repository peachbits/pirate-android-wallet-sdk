package pirate.android.sdk.internal.db.derived

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import pirate.android.sdk.internal.Backend
import pirate.android.sdk.internal.NoBackupContextWrapper
import pirate.android.sdk.internal.Twig
import pirate.android.sdk.internal.db.ReadOnlySupportSqliteOpenHelper
import pirate.android.sdk.internal.ext.tryWarn
import pirate.android.sdk.internal.initAccountsTable
import pirate.android.sdk.internal.initBlocksTable
import pirate.android.sdk.internal.model.Checkpoint
import pirate.android.sdk.model.UnifiedFullViewingKey
import pirate.android.sdk.model.PirateNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

internal class DerivedDataDb private constructor(
    zcashNetwork: PirateNetwork,
    private val sqliteDatabase: SupportSQLiteDatabase
) {
    val accountTable = AccountTable(sqliteDatabase)

    val blockTable = BlockTable(zcashNetwork, sqliteDatabase)

    val transactionTable = TransactionTable(zcashNetwork, sqliteDatabase)

    val allTransactionView = AllTransactionView(zcashNetwork, sqliteDatabase)

    val txOutputsView = TxOutputsView(zcashNetwork, sqliteDatabase)

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
            backend: Backend,
            databaseFile: File,
            zcashNetwork: PirateNetwork,
            checkpoint: Checkpoint,
            seed: ByteArray?,
            viewingKeys: List<UnifiedFullViewingKey>
        ): DerivedDataDb {
            backend.initDataDb(seed)

            runCatching {
                // TODO [#681]: consider converting these to typed exceptions in the welding layer
                // TODO [#681]: https://github.com/zcash/zcash-android-wallet-sdk/issues/681
                tryWarn(
                    message = "Did not initialize the blocks table. It probably was already initialized.",
                    ifContains = "table is not empty"
                ) {
                    backend.initBlocksTable(checkpoint)
                }
                tryWarn(
                    message = "Did not initialize the accounts table. It probably was already initialized.",
                    ifContains = "table is not empty"
                ) {
                    backend.initAccountsTable(*viewingKeys.toTypedArray())
                }
            }.onFailure {
                Twig.error { "Failed to init derived data database with $it" }
            }

            val database = ReadOnlySupportSqliteOpenHelper.openExistingDatabaseAsReadOnly(
                NoBackupContextWrapper(
                    context,
                    databaseFile.parentFile!!
                ),
                databaseFile,
                DATABASE_VERSION
            )

            return DerivedDataDb(zcashNetwork, database)
        }
    }
}
