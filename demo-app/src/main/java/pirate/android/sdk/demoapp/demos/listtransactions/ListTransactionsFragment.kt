package pirate.android.sdk.demoapp.demos.listtransactions

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import pirate.android.sdk.PirateInitializer
import pirate.android.sdk.Synchronizer
import pirate.android.sdk.block.PirateCompactBlockProcessor
import pirate.android.sdk.db.entity.PirateConfirmedTransaction
import pirate.android.sdk.demoapp.BaseDemoFragment
import pirate.android.sdk.demoapp.databinding.FragmentListTransactionsBinding
import pirate.android.sdk.demoapp.ext.requireApplicationContext
import pirate.android.sdk.demoapp.util.fromResources
import pirate.android.sdk.ext.collectWith
import pirate.android.sdk.internal.twig
import pirate.android.sdk.tool.PirateDerivationTool
import pirate.android.sdk.type.PirateNetwork
import kotlinx.coroutines.runBlocking

/**
 * List all transactions related to the given seed, since the given birthday. This begins by
 * downloading any missing blocks and then validating and scanning their contents. Once scan is
 * complete, the transactions are available in the database and can be accessed by any SQL tool.
 * By default, the SDK uses a PiratePagedTransactionRepository to provide transaction contents from the
 * database in a paged format that works natively with RecyclerViews.
 */
class ListTransactionsFragment : BaseDemoFragment<FragmentListTransactionsBinding>() {
    private lateinit var initializer: PirateInitializer
    private lateinit var synchronizer: Synchronizer
    private lateinit var adapter: TransactionAdapter<PirateConfirmedTransaction>
    private lateinit var address: String
    private var status: Synchronizer.PirateStatus? = null
    private val isSynced get() = status == Synchronizer.PirateStatus.SYNCED

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

        initializer = PirateInitializer.newBlocking(
            requireApplicationContext(),
            PirateInitializer.PirateConfig {
                runBlocking {
                    it.importWallet(
                        seed,
                        birthday = null,
                        network = PirateNetwork.fromResources(requireApplicationContext())
                    )
                }
            }
        )
        address = runBlocking {
            PirateDerivationTool.deriveShieldedAddress(
                seed,
                PirateNetwork.fromResources(requireApplicationContext())
            )
        }
        synchronizer = Synchronizer.newBlocking(initializer)
    }

    private fun initTransactionUI() {
        binding.recyclerTransactions.layoutManager =
            LinearLayoutManager(activity, LinearLayoutManager.VERTICAL, false)
        adapter = TransactionAdapter()
        binding.recyclerTransactions.adapter = adapter
    }

    private fun monitorChanges() {
        // the lifecycleScope is used to stop everything when the fragment dies
        synchronizer.status.collectWith(lifecycleScope, ::onStatus)
        synchronizer.processorInfo.collectWith(lifecycleScope, ::onProcessorInfoUpdated)
        synchronizer.progress.collectWith(lifecycleScope, ::onProgress)
        synchronizer.clearedTransactions.collectWith(lifecycleScope, ::onTransactionsUpdated)
    }

    //
    // Change listeners
    //

    private fun onProcessorInfoUpdated(info: PirateCompactBlockProcessor.ProcessorInfo) {
        if (info.isScanning) binding.textInfo.text = "Scanning blocks...${info.scanProgress}%"
    }

    private fun onProgress(i: Int) {
        if (i < 100) binding.textInfo.text = "Downloading blocks...$i%"
    }

    private fun onStatus(status: Synchronizer.PirateStatus) {
        this.status = status
        binding.textStatus.text = "Status: $status"
        if (isSynced) onSyncComplete()
    }

    private fun onSyncComplete() {
        binding.textInfo.visibility = View.INVISIBLE
    }

    private fun onTransactionsUpdated(transactions: List<PirateConfirmedTransaction>) {
        twig("got a new paged list of transactions")
        adapter.submitList(transactions)

        // show message when there are no transactions
        if (isSynced) {
            binding.textInfo.apply {
                if (transactions.isEmpty()) {
                    visibility = View.VISIBLE
                    text =
                        "No transactions found. Try to either change the seed words " +
                        "or send funds to this address (tap the FAB to copy it):\n\n $address"
                } else {
                    visibility = View.INVISIBLE
                    text = ""
                }
            }
        }
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
        initTransactionUI()
    }

    override fun onResume() {
        super.onResume()
        // the lifecycleScope is used to dispose of the synchronizer when the fragment dies
        synchronizer.start(lifecycleScope)
        monitorChanges()
    }

    //
    // Base Fragment overrides
    //

    override fun onActionButtonClicked() {
        if (::address.isInitialized) copyToClipboard(address)
    }

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentListTransactionsBinding =
        FragmentListTransactionsBinding.inflate(layoutInflater)
}
