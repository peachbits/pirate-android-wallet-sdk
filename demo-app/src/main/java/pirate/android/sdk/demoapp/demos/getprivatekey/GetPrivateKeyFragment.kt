package pirate.android.sdk.demoapp.demos.getprivatekey

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import pirate.android.sdk.demoapp.BaseDemoFragment
import pirate.android.sdk.demoapp.databinding.FragmentGetPrivateKeyBinding
import pirate.android.sdk.demoapp.ext.requireApplicationContext
import pirate.android.sdk.demoapp.util.fromResources
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.tool.PirateDerivationTool
import kotlinx.coroutines.launch

/**
 * Displays the viewing key and spending key associated with the seed used during the demo. The
 * seedPhrase defaults to the value of`DemoConfig.seedWords` but can be set by the user on the
 * HomeFragment.
 */
class GetPrivateKeyFragment : BaseDemoFragment<FragmentGetPrivateKeyBinding>() {

    private lateinit var seedPhrase: String
    private lateinit var seed: ByteArray

    /**
     * Initialize the required values that would normally live outside the demo but are repeated
     * here for completeness so that each demo file can serve as a standalone example.
     */
    private fun setup() {
        // defaults to the value of `DemoConfig.seedWords` but can also be set by the user
        seedPhrase = sharedViewModel.seedPhrase.value

        // Use a BIP-39 library to convert a seed phrase into a byte array. Most wallets already
        // have the seed stored
        seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()
    }

    private fun displayKeys() {
        // derive the keys from the seed:
        // demonstrate deriving spending keys for five accounts but only take the first one
        lifecycleScope.launchWhenStarted {
            @Suppress("MagicNumber")
            val spendingKey = PirateDerivationTool.deriveSpendingKeys(
                seed,
                PirateNetwork.fromResources(requireApplicationContext()),
                5
            ).first()

            // derive the key that allows you to view but not spend transactions
            val viewingKey = PirateDerivationTool.deriveViewingKey(
                spendingKey,
                PirateNetwork.fromResources(requireApplicationContext())
            )

            // display the keys in the UI
            binding.textInfo.setText("Spending Key:\n$spendingKey\n\nViewing Key:\n$viewingKey")
        }
    }

    //
    // Android Lifecycle overrides
    //

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)
        setup()
        return view
    }

    override fun onResume() {
        super.onResume()
        displayKeys()
    }

    //
    // Base Fragment overrides
    //

    override fun onActionButtonClicked() {
        lifecycleScope.launch {
            copyToClipboard(
                PirateDerivationTool.derivePirateUnifiedViewingKeys(
                    seed,
                    PirateNetwork.fromResources(requireApplicationContext())
                ).first().extpub,
                "ViewingKey copied to clipboard!"
            )
        }
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetPrivateKeyBinding =
        FragmentGetPrivateKeyBinding.inflate(layoutInflater)
}
