package pirate.android.sdk.demoapp.demos.send

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.TextView
import androidx.lifecycle.lifecycleScope
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import pirate.android.sdk.Initializer
import pirate.android.sdk.Synchronizer
import pirate.android.sdk.block.PirateCompactBlockProcessor
import pirate.android.sdk.db.entity.PendingTransaction
import pirate.android.sdk.db.entity.isCreated
import pirate.android.sdk.db.entity.isCreating
import pirate.android.sdk.db.entity.isFailedEncoding
import pirate.android.sdk.db.entity.isFailedSubmit
import pirate.android.sdk.db.entity.isMined
import pirate.android.sdk.db.entity.isSubmitSuccess
import pirate.android.sdk.demoapp.BaseDemoFragment
import pirate.android.sdk.demoapp.DemoConstants
import pirate.android.sdk.demoapp.databinding.FragmentSendBinding
import pirate.android.sdk.demoapp.ext.requireApplicationContext
import pirate.android.sdk.demoapp.util.fromResources
import pirate.android.sdk.demoapp.util.mainActivity
import pirate.android.sdk.ext.collectWith
import pirate.android.sdk.ext.convertZatoshiToArrrString
import pirate.android.sdk.ext.convertArrrToZatoshi
import pirate.android.sdk.ext.toArrrString
import pirate.android.sdk.internal.Twig
import pirate.android.sdk.internal.twig
import pirate.android.sdk.tool.PirateDerivationTool
import pirate.android.sdk.type.PirateWalletBalance
import pirate.android.sdk.type.PirateNetwork
import kotlinx.coroutines.runBlocking

/**
 * Demonstrates sending funds to an address. This is the most complex example that puts all of the
 * pieces of the SDK together, including monitoring transactions for completion. It begins by
 * downloading, validating and scanning any missing blocks. Once that is complete, the wallet is
 * in a SYNCED state and available to send funds. Calling `sendToAddress` produces a flow of
 * PendingTransaction objects which represent the active state of the transaction that was sent.
 * Any time the state of that transaction changes, a new instance will be emitted.
 */
class SendFragment : BaseDemoFragment<FragmentSendBinding>() {
    private lateinit var synchronizer: Synchronizer

    private lateinit var amountInput: TextView
    private lateinit var addressInput: TextView

    // in a normal app, this would be stored securely with the trusted execution environment (TEE)
    // but since this is a demo, we'll derive it on the fly
    private lateinit var spendingKey: String

    /**
     * Initialize the required values that would normally live outside the demo but are repeated
     * here for completeness so that each demo file can serve as a standalone example.
     */
    private fun setup() {
        // defaults to the value of `DemoConfig.seedWords` but can also be set by the user
        var seedPhrase = sharedViewModel.seedPhrase.value

        // Use a BIP-39 library to convert a seed phrase into a byte array. Most wallets already
        // have the seed stored
        val seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()

        runBlocking {
            Initializer.new(requireApplicationContext()) {
                runBlocking { it.importWallet(seed, network = PirateNetwork.fromResources(requireApplicationContext())) }
                it.setNetwork(PirateNetwork.fromResources(requireApplicationContext()))
            }
        }.let { initializer ->
            synchronizer = Synchronizer.newBlocking(initializer)
        }
        spendingKey = runBlocking { PirateDerivationTool.deriveSpendingKeys(seed, PirateNetwork.fromResources(requireApplicationContext())).first() }
    }

    //
    // Observable properties (done without livedata or flows for simplicity)
    //

    private var balance = PirateWalletBalance()
        set(value) {
            field = value
            onUpdateSendButton()
        }
    private var isSending = false
        set(value) {
            field = value
            if (value) Twig.sprout("Sending") else Twig.clip("Sending")
            onUpdateSendButton()
        }
    private var isSyncing = true
        set(value) {
            field = value
            onUpdateSendButton()
        }

    //
    // Private functions
    //

    private fun initSendUi() {
        amountInput = binding.inputAmount.apply {
            setText(DemoConstants.sendAmount.toArrrString())
        }
        addressInput = binding.inputAddress.apply {
            setText(DemoConstants.toAddress)
        }
        binding.buttonSend.setOnClickListener(::onSend)
    }

    private fun monitorChanges() {
        synchronizer.status.collectWith(lifecycleScope, ::onStatus)
        synchronizer.progress.collectWith(lifecycleScope, ::onProgress)
        synchronizer.processorInfo.collectWith(lifecycleScope, ::onProcessorInfoUpdated)
        synchronizer.saplingBalances.collectWith(lifecycleScope, ::onBalance)
    }

    //
    // Change listeners
    //

    private fun onStatus(status: Synchronizer.Status) {
        binding.textStatus.text = "Status: $status"
        isSyncing = status != Synchronizer.Status.SYNCED
        if (status == Synchronizer.Status.SCANNING) {
            binding.textBalance.text = "Calculating balance..."
        } else {
            if (!isSyncing) onBalance(balance)
        }
    }

    private fun onProgress(i: Int) {
        if (i < 100) {
            binding.textStatus.text = "Downloading blocks...$i%"
            binding.textBalance.visibility = View.INVISIBLE
        } else {
            binding.textBalance.visibility = View.VISIBLE
        }
    }

    private fun onProcessorInfoUpdated(info: PirateCompactBlockProcessor.ProcessorInfo) {
        if (info.isScanning) binding.textStatus.text = "Scanning blocks...${info.scanProgress}%"
    }

    private fun onBalance(balance: PirateWalletBalance) {
        this.balance = balance
        if (!isSyncing) {
            binding.textBalance.text = """
                Available balance: ${balance.availableZatoshi.convertZatoshiToArrrString(12)}
                Total balance: ${balance.totalZatoshi.convertZatoshiToArrrString(12)}
            """.trimIndent()
        }
    }

    private fun onSend(unused: View) {
        isSending = true
        val amount = amountInput.text.toString().toDouble().convertArrrToZatoshi()
        val toAddress = addressInput.text.toString().trim()
        synchronizer.sendToAddress(
            spendingKey,
            amount,
            toAddress,
            "Funds from Demo App"
        ).collectWith(lifecycleScope, ::onPendingTxUpdated)
        mainActivity()?.hideKeyboard()
    }

    private fun onPendingTxUpdated(pendingTransaction: PendingTransaction?) {
        val id = pendingTransaction?.id ?: -1
        val message = when {
            pendingTransaction == null -> "Transaction not found"
            pendingTransaction.isMined() -> "Transaction Mined (id: $id)!\n\nSEND COMPLETE".also { isSending = false }
            pendingTransaction.isSubmitSuccess() -> "Successfully submitted transaction!\nAwaiting confirmation..."
            pendingTransaction.isFailedEncoding() -> "ERROR: failed to encode transaction! (id: $id)".also { isSending = false }
            pendingTransaction.isFailedSubmit() -> "ERROR: failed to submit transaction! (id: $id)".also { isSending = false }
            pendingTransaction.isCreated() -> "Transaction creation complete! (id: $id)"
            pendingTransaction.isCreating() -> "Creating transaction!".also { onResetInfo() }
            else -> "Transaction updated!".also { twig("Unhandled TX state: $pendingTransaction") }
        }
        twig("Pending TX Updated: $message")
        binding.textInfo.apply {
            text = "$text\n$message"
        }
    }

    private fun onUpdateSendButton() {
        with(binding.buttonSend) {
            when {
                isSending -> {
                    text = "➡ sending"
                    isEnabled = false
                }
                isSyncing -> {
                    text = "⌛ syncing"
                    isEnabled = false
                }
                balance.availableZatoshi <= 0 -> isEnabled = false
                else -> {
                    text = "send"
                    isEnabled = true
                }
            }
        }
    }

    private fun onResetInfo() {
        binding.textInfo.text = "Active Transaction:"
    }

    //
    // Android Lifecycle overrides
    //

    override fun onActivityCreated(savedInstanceState: Bundle?) {
        super.onActivityCreated(savedInstanceState)
        setup()
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initSendUi()
    }

    override fun onResume() {
        super.onResume()
        // the lifecycleScope is used to dispose of the synchronizer when the fragment dies
        synchronizer.start(lifecycleScope)
        monitorChanges()
    }

    //
    // BaseDemoFragment overrides
    //

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentSendBinding =
        FragmentSendBinding.inflate(layoutInflater)
}
