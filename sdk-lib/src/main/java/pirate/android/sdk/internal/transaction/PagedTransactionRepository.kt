package pirate.android.sdk.internal.transaction

import android.content.Context
import androidx.paging.PagedList
import androidx.room.RoomDatabase
import pirate.android.sdk.db.commonDatabaseBuilder
import pirate.android.sdk.db.entity.PirateConfirmedTransaction
import pirate.android.sdk.ext.PirateSdk
import pirate.android.sdk.internal.SdkDispatchers
import pirate.android.sdk.internal.SdkExecutors
import pirate.android.sdk.internal.db.PirateDerivedDataDb
import pirate.android.sdk.internal.ext.android.toFlowPagedList
import pirate.android.sdk.internal.ext.android.toRefreshable
import pirate.android.sdk.internal.ext.tryWarn
import pirate.android.sdk.internal.model.Checkpoint
import pirate.android.sdk.internal.twig
import pirate.android.sdk.jni.PirateRustBackend
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.type.PirateUnifiedViewingKey
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Example of a repository that leverages the Room paging library to return a [PagedList] of
 * transactions. Consumers can register as a page listener and receive an interface that allows for
 * efficiently paging data.
 *
 * @param pageSize transactions per page. This influences pre-fetch and memory configuration.
 */
@Suppress("TooManyFunctions")
internal class PiratePagedTransactionRepository private constructor(
    private val zcashNetwork: PirateNetwork,
    private val db: PirateDerivedDataDb,
    private val pageSize: Int
) : TransactionRepository {

    // DAOs
    private val blocks = db.blockDao()
    private val accounts = db.accountDao()
    private val transactions = db.transactionDao()

    // Transaction Flows
    private val allTransactionsFactory = transactions.getAllTransactions().toRefreshable()

    override val receivedTransactions
        get() = flow<List<PirateConfirmedTransaction>> {
            emitAll(
                transactions.getReceivedTransactions().toRefreshable().toFlowPagedList(pageSize)
            )
        }
    override val sentTransactions
        get() = flow<List<PirateConfirmedTransaction>> {
            emitAll(transactions.getSentTransactions().toRefreshable().toFlowPagedList(pageSize))
        }
    override val allTransactions
        get() = flow<List<PirateConfirmedTransaction>> {
            emitAll(allTransactionsFactory.toFlowPagedList(pageSize))
        }

    //
    // TransactionRepository API
    //

    override fun invalidate() = allTransactionsFactory.refresh()

    override suspend fun lastScannedHeight() = BlockHeight.new(zcashNetwork, blocks.lastScannedHeight())

    override suspend fun firstScannedHeight() = BlockHeight.new(zcashNetwork, blocks.firstScannedHeight())

    override suspend fun isInitialized() = blocks.count() > 0

    override suspend fun findEncodedTransactionById(txId: Long) =
        transactions.findEncodedTransactionById(txId)

    override suspend fun findNewTransactions(blockHeightRange: ClosedRange<BlockHeight>): List<PirateConfirmedTransaction> =
        transactions.findAllTransactionsByRange(blockHeightRange.start.value, blockHeightRange.endInclusive.value)

    override suspend fun findMinedHeight(rawTransactionId: ByteArray) =
        transactions.findMinedHeight(rawTransactionId)?.let { BlockHeight.new(zcashNetwork, it) }

    override suspend fun findMatchingTransactionId(rawTransactionId: ByteArray): Long? =
        transactions.findMatchingTransactionId(rawTransactionId)

    override suspend fun cleanupCancelledTx(rawTransactionId: ByteArray) =
        transactions.cleanupCancelledTx(rawTransactionId)

    // let expired transactions linger in the UI for a little while
    override suspend fun deleteExpired(lastScannedHeight: BlockHeight) =
        transactions.deleteExpired(lastScannedHeight.value - (PirateSdk.EXPIRY_OFFSET / 2))

    override suspend fun count() = transactions.count()

    override suspend fun getAccount(accountId: Int) = accounts.findAccountById(accountId)

    override suspend fun getAccountCount() = accounts.count()

    /**
     * Close the underlying database.
     */
    suspend fun close() {
        withContext(SdkDispatchers.DATABASE_IO) {
            db.close()
        }
    }

    // TODO [#681]: begin converting these into Data Access API. For now, just collect the desired
    //  operations and iterate/refactor, later
    // TODO [#681]: https://github.com/zcash/zcash-android-wallet-sdk/issues/681
    suspend fun findBlockHash(height: BlockHeight): ByteArray? = blocks.findHashByHeight(height.value)
    suspend fun getTransactionCount(): Int = transactions.count()

    // TODO [#681]: convert this into a wallet repository rather than "transaction repository"
    // TODO [#681]: https://github.com/zcash/zcash-android-wallet-sdk/issues/681

    companion object {

        @Suppress("LongParameterList")
        internal suspend fun new(
            appContext: Context,
            zcashNetwork: PirateNetwork,
            pageSize: Int = 10,
            rustBackend: PirateRustBackend,
            birthday: Checkpoint,
            viewingKeys: List<PirateUnifiedViewingKey>,
            overwriteVks: Boolean = false
        ): PiratePagedTransactionRepository {
            initMissingDatabases(rustBackend, birthday, viewingKeys)

            val db = buildDatabase(appContext.applicationContext, rustBackend.dataDbFile)
            applyKeyMigrations(rustBackend, overwriteVks, viewingKeys)

            return PiratePagedTransactionRepository(zcashNetwork, db, pageSize)
        }

        /**
         * Build the database and apply migrations.
         */
        private suspend fun buildDatabase(context: Context, databaseFile: File): PirateDerivedDataDb {
            twig("Building dataDb and applying migrations")
            return commonDatabaseBuilder(
                context,
                PirateDerivedDataDb::class.java,
                databaseFile
            )
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                .setQueryExecutor(SdkExecutors.DATABASE_IO)
                .setTransactionExecutor(SdkExecutors.DATABASE_IO)
                .addMigrations(PirateDerivedDataDb.MIGRATION_3_4)
                .addMigrations(PirateDerivedDataDb.MIGRATION_4_3)
                .addMigrations(PirateDerivedDataDb.MIGRATION_4_5)
                .addMigrations(PirateDerivedDataDb.MIGRATION_5_6)
                .addMigrations(PirateDerivedDataDb.MIGRATION_6_7)
                .build().also {
                    // TODO [#681]: document why we do this. My guess is to catch database issues early or to trigger
                    //  migrations--I forget why it was added but there was a good reason?
                    // TODO [#681]: https://github.com/zcash/zcash-android-wallet-sdk/issues/681
                    withContext(SdkDispatchers.DATABASE_IO) {
                        // TODO [#649]: StrictMode policy violation: LeakedClosableViolation
                        // TODO [#649]: https://github.com/zcash/zcash-android-wallet-sdk/issues/649
                        it.openHelper.writableDatabase.beginTransaction()
                        it.openHelper.writableDatabase.endTransaction()
                    }
                }
        }

        /**
         * Create any databases that don't already exist via Rust. Originally, this was done on the Rust
         * side because Rust was intended to own the "dataDb" and Kotlin just reads from it. Since then,
         * it has been more clear that Kotlin should own the data and just let Rust use it.
         */
        private suspend fun initMissingDatabases(
            rustBackend: PirateRustBackend,
            birthday: Checkpoint,
            viewingKeys: List<PirateUnifiedViewingKey>
        ) {
            maybeCreateDataDb(rustBackend)
            maybeInitBlocksTable(rustBackend, birthday)
            maybeInitAccountsTable(rustBackend, viewingKeys)
        }

        /**
         * Create the dataDb and its table, if it doesn't exist.
         */
        private suspend fun maybeCreateDataDb(rustBackend: PirateRustBackend) {
            tryWarn("Warning: did not create dataDb. It probably already exists.") {
                rustBackend.initDataDb()
                twig("Initialized wallet for first run file: ${rustBackend.dataDbFile}")
            }
        }

        /**
         * Initialize the blocks table with the given birthday, if needed.
         */
        private suspend fun maybeInitBlocksTable(
            rustBackend: PirateRustBackend,
            checkpoint: Checkpoint
        ) {
            // TODO [#681]: consider converting these to typed exceptions in the welding layer
            // TODO [#681]: https://github.com/zcash/zcash-android-wallet-sdk/issues/681
            tryWarn(
                "Warning: did not initialize the blocks table. It probably was already initialized.",
                ifContains = "table is not empty"
            ) {
                rustBackend.initBlocksTable(checkpoint)
                twig("seeded the database with sapling tree at height ${checkpoint.height}")
            }
            twig("database file: ${rustBackend.dataDbFile}")
        }

        /**
         * Initialize the accounts table with the given viewing keys.
         */
        private suspend fun maybeInitAccountsTable(
            rustBackend: PirateRustBackend,
            viewingKeys: List<PirateUnifiedViewingKey>
        ) {
            // TODO [#681]: consider converting these to typed exceptions in the welding layer
            // TODO [#681]: https://github.com/zcash/zcash-android-wallet-sdk/issues/681
            tryWarn(
                "Warning: did not initialize the accounts table. It probably was already initialized.",
                ifContains = "table is not empty"
            ) {
                @Suppress("SpreadOperator")
                rustBackend.initAccountsTable(*viewingKeys.toTypedArray())
                twig("Initialized the accounts table with ${viewingKeys.size} viewingKey(s)")
            }
        }

        private suspend fun applyKeyMigrations(
            rustBackend: PirateRustBackend,
            overwriteVks: Boolean,
            viewingKeys: List<PirateUnifiedViewingKey>
        ) {
            if (overwriteVks) {
                twig("applying key migrations . . .")
                maybeInitAccountsTable(rustBackend, viewingKeys)
            }
        }
    }
}
