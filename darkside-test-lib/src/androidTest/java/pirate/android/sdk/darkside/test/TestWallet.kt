package pirate.android.sdk.darkside.test

import androidx.test.platform.app.InstrumentationRegistry
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed

import pirate.android.sdk.SdkSynchronizer
import pirate.android.sdk.Synchronizer
import pirate.android.sdk.internal.Twig
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.Darkside
import pirate.android.sdk.model.WalletBalance
import pirate.android.sdk.model.Arrrtoshi
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.tool.DerivationTool
import pirate.lightwallet.client.model.LightWalletEndpoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.newFixedThreadPoolContext
import kotlinx.coroutines.runBlocking
import java.util.concurrent.TimeoutException

/**
 * A simple wallet that connects to testnet for integration testing. The intention is that it is
 * easy to drive and nice to use.
 */
@OptIn(DelicateCoroutinesApi::class)
class TestWallet(
    val seedPhrase: String,
    val alias: String = "TestWallet",
    val network: PirateNetwork = PirateNetwork.Testnet,
    val endpoint: LightWalletEndpoint = LightWalletEndpoint.Darkside,
    startHeight: BlockHeight? = null
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

    private val account = Account.DEFAULT
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val seed: ByteArray = Mnemonics.MnemonicCode(seedPhrase).toSeed()
    private val shieldedSpendingKey =
        runBlocking { DerivationTool.getInstance().deriveUnifiedSpendingKey(seed, network = network, account) }
    val synchronizer: SdkSynchronizer = Synchronizer.newBlocking(
        context,
        network,
        alias,
        endpoint,
        seed,
        startHeight
    ) as SdkSynchronizer

    val available get() = synchronizer.saplingBalances.value?.available
    val unifiedAddress =
        runBlocking { synchronizer.getUnifiedAddress(account) }
    val transparentAddress =
        runBlocking { synchronizer.getTransparentAddress(account) }
    val birthdayHeight get() = synchronizer.latestBirthdayHeight
    val networkName get() = synchronizer.network.networkName

    suspend fun transparentBalance(): WalletBalance {
        synchronizer.refreshUtxos(account, synchronizer.latestBirthdayHeight)
        return synchronizer.getTransparentBalance(transparentAddress)
    }

    suspend fun sync(timeout: Long = -1): TestWallet {
        val killSwitch = walletScope.launch {
            if (timeout > 0) {
                delay(timeout)
                throw TimeoutException("Failed to sync wallet within ${timeout}ms")
            }
        }

        // block until synced
        synchronizer.status.first { it == Synchronizer.Status.SYNCED }
        killSwitch.cancel()
        return this
    }

    suspend fun send(
        address: String = transparentAddress,
        memo: String = "",
        amount: Arrrtoshi = Arrrtoshi(500L)
    ): TestWallet {
        synchronizer.sendToAddress(shieldedSpendingKey, amount, address, memo)
        return this
    }

    suspend fun rewindToHeight(height: BlockHeight): TestWallet {
        synchronizer.rewindToNearestHeight(height, false)
        return this
    }

    suspend fun shieldFunds(): TestWallet {
        synchronizer.refreshUtxos(Account.DEFAULT, BlockHeight.new(PirateNetwork.Mainnet, 935000)).let { count ->
            Twig.debug { "FOUND $count new UTXOs" }
        }

        synchronizer.getTransparentBalance(transparentAddress).let { walletBalance ->
            if (walletBalance.available.value > 0L) {
                synchronizer.shieldFunds(shieldedSpendingKey)
            }
        }

        return this
    }

    suspend fun join(timeout: Long? = null): TestWallet {
        // block until stopped
        if (timeout != null) {
            walletScope.launch {
                delay(timeout)
                synchronizer.close()
            }
        }
        synchronizer.status.first { it == Synchronizer.Status.STOPPED }
        return this
    }

    companion object {
    }

    enum class Backups(val seedPhrase: String, val testnetBirthday: BlockHeight, val mainnetBirthday: BlockHeight) {
        // TODO [#902]: Get the proper birthday values for test wallets
        // TODO [#902]: https://github.com/zcash/zcash-android-wallet-sdk/issues/902
        DEFAULT(
            "column rhythm acoustic gym cost fit keen maze fence seed mail medal shrimp tell relief clip" +
                " cannon foster soldier shallow refuse lunar parrot banana",
            BlockHeight.new(
                PirateNetwork.Testnet,
                1_355_928
            ),
            BlockHeight.new(PirateNetwork.Mainnet, 1_000_000)
        ),
        SAMPLE_WALLET(
            "input frown warm senior anxiety abuse yard prefer churn reject people glimpse govern glory" +
                " crumble swallow verb laptop switch trophy inform friend permit purpose",
            BlockHeight.new(
                PiratehNetwork.Testnet,
                1_330_190
            ),
            BlockHeight.new(PirateNetwork.Mainnet, 1_000_000)
        ),
        DEV_WALLET(
            "still champion voice habit trend flight survey between bitter process artefact blind carbon" +
                " truly provide dizzy crush flush breeze blouse charge solid fish spread",
            BlockHeight.new(
                PirateNetwork.Testnet,
                1_000_000
            ),
            BlockHeight.new(PirateNetwork.Mainnet, 991645)
        ),
        ALICE(
            "quantum whisper lion route fury lunar pelican image job client hundred sauce chimney barely" +
                " life cliff spirit admit weekend message recipe trumpet impact kitten",
            BlockHeight.new(
                PirateNetwork.Testnet,
                1_330_190
            ),
            BlockHeight.new(PirateNetwork.Mainnet, 1_000_000)
        ),
        BOB(
            "canvas wine sugar acquire garment spy tongue odor hole cage year habit bullet make label" +
                " human unit option top calm neutral try vocal arena",
            BlockHeight.new(
                PirateNetwork.Testnet,
                1_330_190
            ),
            BlockHeight.new(PirateNetwork.Mainnet, 1_000_000)
        )
    }
}
