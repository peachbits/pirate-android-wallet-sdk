package pirate.android.sdk.internal.db.block

import android.content.Context
import androidx.room.RoomDatabase
import pirate.android.sdk.internal.SdkDispatchers
import pirate.android.sdk.internal.SdkExecutors
import pirate.android.sdk.internal.db.commonDatabaseBuilder
import pirate.android.sdk.internal.repository.PirateCompactBlockRepository
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.PirateNetwork
import pirate.wallet.sdk.rpc.CompactFormats
import kotlinx.coroutines.withContext
import java.io.File

/**
 * An implementation of CompactBlockStore that persists information to a database in the given
 * path. This represents the "cache db" or local cache of compact blocks waiting to be scanned.
 */
class DbPirateCompactBlockRepository private constructor(
    private val network: PirateNetwork,
    private val cacheDb: CompactBlockDb
) : PirateCompactBlockRepository {

    private val cacheDao = cacheDb.compactBlockDao()

    override suspend fun getLatestHeight(): BlockHeight? = runCatching {
        BlockHeight.new(network, cacheDao.latestBlockHeight())
    }.getOrNull()

    override suspend fun findCompactBlock(height: BlockHeight): CompactFormats.CompactBlock? =
        cacheDao.findCompactBlock(height.value)?.let { CompactFormats.CompactBlock.parseFrom(it) }

    override suspend fun write(result: Sequence<CompactFormats.CompactBlock>) =
        cacheDao.insert(result.map { PirateCompactBlockEntity(it.height, it.toByteArray()) })

    override suspend fun rewindTo(height: BlockHeight) =
        cacheDao.rewindTo(height.value)

    override suspend fun close() {
        withContext(SdkDispatchers.DATABASE_IO) {
            cacheDb.close()
        }
    }

    companion object {
        /**
         * @param appContext the application context. This is used for creating the database.
         * @property databaseFile the database file.
         */
        fun new(
            appContext: Context,
            zcashNetwork: PirateNetwork,
            databaseFile: File
        ): DbPirateCompactBlockRepository {
            val cacheDb = createCompactBlockCacheDb(appContext.applicationContext, databaseFile)

            return DbPirateCompactBlockRepository(zcashNetwork, cacheDb)
        }

        private fun createCompactBlockCacheDb(
            appContext: Context,
            databaseFile: File
        ): CompactBlockDb {
            return commonDatabaseBuilder(
                appContext,
                CompactBlockDb::class.java,
                databaseFile
            )
                .setJournalMode(RoomDatabase.JournalMode.TRUNCATE)
                // this is a simple cache of blocks. destroying the db should be benign
                .fallbackToDestructiveMigration()
                .setQueryExecutor(SdkExecutors.DATABASE_IO)
                .setTransactionExecutor(SdkExecutors.DATABASE_IO)
                .build()
        }
    }
}
