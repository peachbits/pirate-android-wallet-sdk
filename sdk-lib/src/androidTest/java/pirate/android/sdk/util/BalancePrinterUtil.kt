package pirate.android.sdk.util

import androidx.test.platform.app.InstrumentationRegistry
import pirate.android.sdk.CloseablePirateSynchronizer
import pirate.android.sdk.PirateSynchronizer
import pirate.android.sdk.internal.PirateTroubleshootingTwig
import pirate.android.sdk.internal.Twig
import pirate.android.sdk.internal.ext.deleteSuspend
import pirate.android.sdk.internal.model.Checkpoint
import pirate.android.sdk.internal.twig
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.LightWalletEndpoint
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.model.defaultForNetwork
import pirate.android.sdk.test.readFileLinesInFlow
import pirate.android.sdk.tool.CheckpointTool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Ignore
import org.junit.Test

/**
 * A tool for checking transactions since the given birthday and printing balances. This was useful for the Zcon1 app to
 * ensure that we loaded all the pokerchips correctly.
 */
@ExperimentalCoroutinesApi
class BalancePrinterUtil {

    private val network = PirateNetwork.Mainnet
    private val downloadBatchSize = 9_000
    private val birthdayHeight = BlockHeight.new(network, 523240)

    private val mnemonics = SimpleMnemonics()
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val alias = "BalanceUtil"
//    private val caceDbPath = PirateInitializer.cacheDbPath(context, alias)
//
//    private val downloader = PirateCompactBlockDownloader(
//        PirateLightWalletGrpcService(context, host, port),
//        PirateCompactBlockDbStore(context, caceDbPath)
//    )

//    private val processor = PirateCompactBlockProcessor(downloader)

//    private val rustBackend = PirateRustBackend.init(context, cacheDbName, dataDbName)

    private lateinit var birthday: Checkpoint
    private var synchronizer: CloseablePirateSynchronizer? = null

    @Before
    fun setup() {
        Twig.plant(PirateTroubleshootingTwig())
        cacheBlocks()
        birthday = runBlocking { CheckpointTool.loadNearest(context, network, birthdayHeight) }
    }

    private fun cacheBlocks() = runBlocking {
//        twig("downloading compact blocks...")
//        val latestBlockHeight = downloader.getLatestBlockHeight()
//        val lastDownloaded = downloader.getLastDownloadedHeight()
//        val blockRange = (Math.max(birthday, lastDownloaded))..latestBlockHeight
//        downloadNewBlocks(blockRange)
//        val error = validateNewBlocks(blockRange)
//        twig("validation completed with result $error")
//        assertEquals(-1, error)
    }

    private suspend fun deleteDb(dbName: String) {
        context.getDatabasePath(dbName).absoluteFile.deleteSuspend()
    }

    @Test
    @Ignore("This test is broken")
    fun printBalances() = runBlocking {
        readFileLinesInFlow("/utils/seeds.txt")
            .map { seedPhrase ->
                twig("checking balance for: $seedPhrase")
                mnemonics.toSeed(seedPhrase.toCharArray())
            }.collect { seed ->
                // TODO: clear the dataDb but leave the cacheDb

                /*
            what I need to do right now
            - for each seed
            - I can reuse the cache of blocks... so just like get the cache once
            - I need to scan into a new database
                - I don't really need a new rustbackend
                - I definitely don't need a new grpc connection
            - can I just use a processor and point it to a different DB?
            + so yeah, I think I need to use the processor directly right here and just swap out its pieces
                - perhaps create a new initializer and use that to configure the processor?
                - or maybe just set the data destination for the processor
                - I might need to consider how state is impacting this design
                    - can we be more stateless and thereby improve the flexibility of this code?!!!
                  */
                synchronizer?.close()
                synchronizer = PirateSynchronizer.new(
                    context,
                    network,
                    lightWalletEndpoint = LightWalletEndpoint
                        .defaultForNetwork(network),
                    seed = seed,
                    birthday = birthdayHeight
                )

//            deleteDb(dataDbPath)
//            initWallet(seed)
//            twig("scanning blocks for seed <$seed>")
// //            rustBackend.scanBlocks()
//            twig("done scanning blocks for seed $seed")
// //            val total = rustBackend.getBalance(0)
//            twig("found total: $total")
// //            val available = rustBackend.getVerifiedBalance(0)
//            twig("found available: $available")
//            twig("xrxrx2\t$seed\t$total\t$available")
//            println("xrxrx2\t$seed\t$total\t$available")
            }
    }

//    @Test
//    fun printBalances() = runBlocking {
//        readLines().collect { seed ->
//            deleteDb(dataDbName)
//            initWallet(seed)
//            twig("scanning blocks for seed <$seed>")
//            rustBackend.scanBlocks()
//            twig("done scanning blocks for seed $seed")
//            val total = rustBackend.getBalance(0)
//            twig("found total: $total")
//            val available = rustBackend.getVerifiedBalance(0)
//            twig("found available: $available")
//            twig("xrxrx2\t$seed\t$total\t$available")
//            println("xrxrx2\t$seed\t$total\t$available")
//        }

//        Thread.sleep(5000)
//        assertEquals("foo", "bar")
//    }

//    private fun initWallet(seed: String): Wallet {
//        val spendingKeyProvider = Delegates.notNull<String>()
//        return Wallet(
//            context,
//            rustBackend,
//            PirateSampleSeedProvider(seed),
//            spendingKeyProvider,
//            Wallet.loadBirthdayFromAssets(context, birthday)
//        ).apply {
//            runCatching {
//                initialize()
//            }
//        }
//    }

    private fun downloadNewBlocks(range: IntRange) = runBlocking {
        Twig.sprout("downloading")
        twig("downloading blocks in range $range")

        var downloadedBlockHeight = range.start
        val count = range.last - range.first + 1
        val batches =
            (count / downloadBatchSize + (if (count.rem(downloadBatchSize) == 0) 0 else 1))
        twig("found $count missing blocks, downloading in $batches batches of $downloadBatchSize...")
        for (i in 1..batches) {
            val end = Math.min(range.first + (i * downloadBatchSize), range.last + 1)
            val batchRange = downloadedBlockHeight until end
            twig("downloaded $batchRange (batch $i of $batches)") {
//                downloader.downloadBlockRange(batchRange)
            }
            downloadedBlockHeight = end
        }
        Twig.clip("downloading")
    }

//    private fun validateNewBlocks(range: IntRange?): Int {
// //        val dummyWallet = initWallet("dummySeed")
//        Twig.sprout("validating")
//        twig("validating blocks in range $range")
// //        val result = rustBackend.validateCombinedChain()
//        Twig.clip("validating")
//        return result
//    }
}
