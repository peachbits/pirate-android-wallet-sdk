package pirate.android.sdk.demoapp.demos.getbalance

import android.os.Bundle
import android.view.LayoutInflater
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import pirate.android.sdk.Initializer
import pirate.android.sdk.Synchronizer
import pirate.android.sdk.block.PirateCompactBlockProcessor
import pirate.android.sdk.demoapp.BaseDemoFragment
import pirate.android.sdk.demoapp.databinding.FragmentGetBalanceBinding
import pirate.android.sdk.demoapp.ext.requireApplicationContext
import pirate.android.sdk.demoapp.util.fromResources
import pirate.android.sdk.ext.collectWith
import pirate.android.sdk.ext.convertZatoshiToArrrString
import pirate.android.sdk.tool.PirateDerivationTool
import pirate.android.sdk.type.PirateWalletBalance
import pirate.android.sdk.type.PirateNetwork
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
        val viewingKey = runBlocking { PirateDerivationTool.deriveUnifiedViewingKeys(seed, PirateNetwork.fromResources(requireApplicationContext())).first() }

        // using the ViewingKey to initialize
        runBlocking {
            Initializer.new(requireApplicationContext(), null) {
                it.setNetwork(PirateNetwork.fromResources(requireApplicationContext()))
                it.importWallet(viewingKey, network = PirateNetwork.fromResources(requireApplicationContext()))
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
        synchronizer.saplingBalances.collectWith(lifecycleScope, ::onBalance)
    }

    private fun onBalance(balance: PirateWalletBalance) {
        binding.textBalance.text = """
                Available balance: ${balance.availableZatoshi.convertZatoshiToArrrString(12)}
                Total balance: ${balance.totalZatoshi.convertZatoshiToArrrString(12)}
        """.trimIndent()
    }

    private fun onStatus(status: Synchronizer.Status) {
        binding.textStatus.text = "Status: $status"
        if (PirateWalletBalance().none()) {
            binding.textBalance.text = "Calculating balance..."
        } else {
            onBalance(synchronizer.saplingBalances.value)
        }
    }

    private fun onProgress(i: Int) {
        if (i < 100) {
            binding.textStatus.text = "Downloading blocks...$i%"
        }
    }

    /**
     * Extension function which checks if the balance has been updated or its -1
     */
    private fun PirateWalletBalance.none(): Boolean {
        if (synchronizer.saplingBalances.value.totalZatoshi == -1L &&
            synchronizer.saplingBalances.value.availableZatoshi == -1L
        ) return true
        return false
    }

    private fun onProcessorInfoUpdated(info: PirateCompactBlockProcessor.ProcessorInfo) {
        if (info.isScanning) binding.textStatus.text = "Scanning blocks...${info.scanProgress}%"
    }
}
