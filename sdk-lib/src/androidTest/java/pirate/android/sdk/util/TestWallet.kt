package pirate.android.sdk.util

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import pirate.android.sdk.PirateInitializer
import pirate.android.sdk.PirateSdkSynchronizer
import pirate.android.sdk.Synchronizer
import pirate.android.sdk.db.entity.isPending
import pirate.android.sdk.internal.Twig
import pirate.android.sdk.internal.service.PirateLightWalletGrpcService
import pirate.android.sdk.internal.twig
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.PirateWalletBalance
import pirate.android.sdk.model.Arrrtoshi
import pirate.android.sdk.tool.PirateDerivationTool
import pirate.android.sdk.type.PirateNetwork
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeoutException

/**
 * A simple wallet that connects to testnet for integration testing. The intention is that it is
 * easy to drive and nice to use.
 */
class TestWallet(
    val seedPhrase: String,
    val alias: String = "TestWallet",
    val network: PirateNetwork = PirateNetwork.Testnet,
    val host: String = network.defaultHost,
    startHeight: BlockHeight? = null,
    val port: Int = network.defaultPort
) {
    constructor(
        backup: Backups,
        network: PirateNetwork = PirateNetwork.Testnet,
        alias: String = "TestWallet"
    ) : this(
        backup.seedPhrase,
        network = network,
        startHeight = if (network == PirateNetwork.Mainnet) backup.mainnetBirthday else backup.testnetBirthday,
        alias = alias
    )

    val walletScope = CoroutineScope(
        SupervisorJob() + newFixedThreadPoolContext(3, this.javaClass.simpleName)
    )

    // Although runBlocking isn't great, this usage is OK because this is only used within the
    // automated tests

    private val context = InstrumentationRegistry.getInstrumentation().context
    private val seed: ByteArray = Mnemonics.MnemonicCode(seedPhrase).toSeed()
    private val shieldedSpendingKey =
        runBlocking { PirateDerivationTool.deriveSpendingKeys(seed, network = network)[0] }
    private val transparentSecretKey =
        runBlocking { PirateDerivationTool.deriveTransparentSecretKey(seed, network = network) }
    val initializer = runBlocking {
        PirateInitializer.new(context) { config ->
            runBlocking { config.importWallet(seed, startHeight, network, host, alias = alias) }
        }
    }
    val synchronizer: PirateSdkSynchronizer = Synchronizer.newBlocking(initializer) as PirateSdkSynchronizer
    val service = (synchronizer.processor.downloader.lightWalletService as PirateLightWalletGrpcService)

    val available get() = synchronizer.saplingBalances.value?.available
    val shieldedAddress =
        runBlocking { PirateDerivationTool.deriveShieldedAddress(seed, network = network) }
    val transparentAddress =
        runBlocking { PirateDerivationTool.deriveTransparentAddress(seed, network = network) }
    val birthdayHeight get() = synchronizer.latestBirthdayHeight
    val networkName get() = synchronizer.network.networkName
    val connectionInfo get() = service.connectionInfo.toString()

    suspend fun transparentBalance(): PirateWalletBalance {
        synchronizer.refreshUtxos(transparentAddress, synchronizer.latestBirthdayHeight)
        return synchronizer.getTransparentBalance(transparentAddress)
    }

    suspend fun sync(timeout: Long = -1): TestWallet {
        val killSwitch = walletScope.launch {
            if (timeout > 0) {
                delay(timeout)
                throw TimeoutException("Failed to sync wallet within ${timeout}ms")
            }
        }
        if (!synchronizer.isStarted) {
            twig("Starting sync")
            synchronizer.start(walletScope)
        } else {
            twig("Awaiting next SYNCED status")
        }

        // block until synced
        synchronizer.status.first { it == Synchronizer.PirateStatus.SYNCED }
        killSwitch.cancel()
        twig("Synced!")
        return this
    }

    suspend fun send(address: String = transparentAddress, memo: String = "", amount: Arrrtoshi = Arrrtoshi(500L), fromAccountIndex: Int = 0): TestWallet {
        Twig.sprout("$alias sending")
        synchronizer.sendToAddress(shieldedSpendingKey, amount, address, memo, fromAccountIndex)
            .takeWhile { it.isPending(null) }
            .collect {
                twig("Updated transaction: $it")
            }
        Twig.clip("$alias sending")
        return this
    }

    suspend fun rewindToHeight(height: BlockHeight): TestWallet {
        synchronizer.rewindToNearestHeight(height, false)
        return this
    }

    suspend fun shieldFunds(): TestWallet {
        twig("checking $transparentAddress for transactions!")
        synchronizer.refreshUtxos(transparentAddress, BlockHeight.new(PirateNetwork.Mainnet, 935000)).let { count ->
            twig("FOUND $count new UTXOs")
        }

        synchronizer.getTransparentBalance(transparentAddress).let { walletBalance ->
            twig("FOUND utxo balance of total: ${walletBalance.total}  available: ${walletBalance.available}")

            if (walletBalance.available.value > 0L) {
                synchronizer.shieldFunds(shieldedSpendingKey, transparentSecretKey)
                    .onCompletion { twig("done shielding funds") }
                    .catch { twig("Failed with $it") }
                    .collect()
            }
        }

        return this
    }

    suspend fun join(timeout: Long? = null): TestWallet {
        // block until stopped
        twig("Staying alive until synchronizer is stopped!")
        if (timeout != null) {
            twig("Scheduling a stop in ${timeout}ms")
            walletScope.launch {
                delay(timeout)
                synchronizer.stop()
            }
        }
        synchronizer.status.first { it == Synchronizer.PirateStatus.STOPPED }
        twig("Stopped!")
        return this
    }

    companion object {
        init {
            Twig.enabled(true)
        }
    }

    enum class Backups(val seedPhrase: String, val testnetBirthday: BlockHeight, val mainnetBirthday: BlockHeight) {
        // TODO: get the proper birthday values for these wallets
        DEFAULT("column rhythm acoustic gym cost fit keen maze fence seed mail medal shrimp tell relief clip cannon foster soldier shallow refuse lunar parrot banana", BlockHeight.new(PirateNetwork.Testnet, 1_355_928), BlockHeight.new(PirateNetwork.Mainnet, 1_000_000)),
        SAMPLE_WALLET("input frown warm senior anxiety abuse yard prefer churn reject people glimpse govern glory crumble swallow verb laptop switch trophy inform friend permit purpose", BlockHeight.new(PirateNetwork.Testnet, 1_330_190), BlockHeight.new(PirateNetwork.Mainnet, 1_000_000)),
        DEV_WALLET("still champion voice habit trend flight survey between bitter process artefact blind carbon truly provide dizzy crush flush breeze blouse charge solid fish spread", BlockHeight.new(PirateNetwork.Testnet, 1_000_000), BlockHeight.new(PirateNetwork.Mainnet, 991645)),
        ALICE("quantum whisper lion route fury lunar pelican image job client hundred sauce chimney barely life cliff spirit admit weekend message recipe trumpet impact kitten", BlockHeight.new(PirateNetwork.Testnet, 1_330_190), BlockHeight.new(PirateNetwork.Mainnet, 1_000_000)),
        BOB("canvas wine sugar acquire garment spy tongue odor hole cage year habit bullet make label human unit option top calm neutral try vocal arena", BlockHeight.new(PirateNetwork.Testnet, 1_330_190), BlockHeight.new(PirateNetwork.Mainnet, 1_000_000)),
        ;
    }
}
