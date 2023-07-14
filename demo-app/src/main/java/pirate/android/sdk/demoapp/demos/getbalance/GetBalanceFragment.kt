package pirate.android.sdk.demoapp.demos.getbalance

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import pirate.android.sdk.PirateInitializer
import pirate.android.sdk.Synchronizer
import pirate.android.sdk.block.PirateCompactBlockProcessor
import pirate.android.sdk.demoapp.BaseDemoFragment
import pirate.android.sdk.demoapp.databinding.FragmentGetBalanceBinding
import pirate.android.sdk.demoapp.ext.requireApplicationContext
import pirate.android.sdk.demoapp.util.fromResources
import pirate.android.sdk.ext.collectWith
import pirate.android.sdk.ext.convertArrrtoshiToArrrString
import pirate.android.sdk.model.LightWalletEndpoint
import pirate.android.sdk.model.PirateWalletBalance
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.model.defaultForNetwork
import pirate.android.sdk.tool.PirateDerivationTool
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.runBlocking

/**
 * Displays the available balance && total balance associated with the seed defined by the default config.
 * comments.
 */
class GetBalanceFragment : BaseDemoFragment<FragmentGetBalanceBinding>() {

    private lateinit var synchronizer: Synchronizer

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBalanceBinding =
        FragmentGetBalanceBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setup()
    }

    private fun setup() {
        // defaults to the value of `DemoConfig.seedWords` but can also be set by the user
        val seedPhrase = sharedViewModel.seedPhrase.value

        // Use a BIP-39 library to convert a seed phrase into a byte array. Most wallets already
        // have the seed stored
        val seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()

        // converting seed into viewingKey
        val viewingKey = runBlocking {
            PirateDerivationTool.derivePirateUnifiedViewingKeys(
                seed,
                PirateNetwork.fromResources(requireApplicationContext())
            ).first()
        }

        // using the ViewingKey to initialize
        runBlocking {
            PirateInitializer.new(requireApplicationContext(), null) {
                val network = PirateNetwork.fromResources(requireApplicationContext())
                it.newWallet(
                    viewingKey,
                    network = network,
                    lightWalletEndpoint = LightWalletEndpoint.defaultForNetwork(network)
                )
            }
        }.let { initializer ->
            synchronizer = Synchronizer.newBlocking(initializer)
        }
    }

    override fun onResume() {
        super.onResume()
        // the lifecycleScope is used to dispose of the synchronize when the fragment dies
        synchronizer.start(lifecycleScope)
        monitorChanges()
    }

    private fun monitorChanges() {
        synchronizer.status.collectWith(lifecycleScope, ::onStatus)
        synchronizer.progress.collectWith(lifecycleScope, ::onProgress)
        synchronizer.processorInfo.collectWith(lifecycleScope, ::onProcessorInfoUpdated)
        synchronizer.saplingBalances.filterNotNull().collectWith(lifecycleScope, ::onBalance)
    }

    @Suppress("MagicNumber")
    private fun onBalance(balance: PirateWalletBalance) {
        binding.textBalance.text = """
                Available balance: ${balance.available.convertArrrtoshiToArrrString(12)}
                Total balance: ${balance.total.convertArrrtoshiToArrrString(12)}
        """.trimIndent()
    }

    private fun onStatus(status: Synchronizer.PirateStatus) {
        binding.textStatus.text = "Status: $status"
        val balance: PirateWalletBalance? = synchronizer.saplingBalances.value
        if (null == balance) {
            binding.textBalance.text = "Calculating balance..."
        } else {
            onBalance(balance)
        }
    }

    @Suppress("MagicNumber")
    private fun onProgress(i: Int) {
        if (i < 100) {
            binding.textStatus.text = "Downloading blocks...$i%"
        }
    }

    private fun onProcessorInfoUpdated(info: PirateCompactBlockProcessor.ProcessorInfo) {
        if (info.isScanning) binding.textStatus.text = "Scanning blocks...${info.scanProgress}%"
    }
}
