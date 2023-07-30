package pirate.android.sdk.demoapp.demos.getbalance

import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import cash.z.ecc.android.bip39.Mnemonics
import cash.z.ecc.android.bip39.toSeed
import pirate.android.sdk.PirateSynchronizer
import pirate.android.sdk.block.PirateCompactBlockProcessor
import pirate.android.sdk.demoapp.BaseDemoFragment
import pirate.android.sdk.demoapp.R
import pirate.android.sdk.demoapp.databinding.FragmentGetBalanceBinding
import pirate.android.sdk.demoapp.ext.requireApplicationContext
import pirate.android.sdk.demoapp.util.SyncBlockchainBenchmarkTrace
import pirate.android.sdk.demoapp.util.fromResources
import pirate.android.sdk.ext.PirateSdk
import pirate.android.sdk.ext.convertArrrtoshiToArrrString
import pirate.android.sdk.internal.twig
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.PirateWalletBalance
import pirate.android.sdk.model.Arrrtoshi
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.tool.PirateDerivationTool
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch

/**
 * Displays the available balance && total balance associated with the seed defined by the default config.
 * comments.
 */
@Suppress("TooManyFunctions")
class GetBalanceFragment : BaseDemoFragment<FragmentGetBalanceBinding>() {

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBalanceBinding =
        FragmentGetBalanceBinding.inflate(layoutInflater)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        reportTraceEvent(SyncBlockchainBenchmarkTrace.Event.BALANCE_SCREEN_START)
        setHasOptionsMenu(true)
    }

    override fun onPrepareOptionsMenu(menu: Menu) {
        super.onPrepareOptionsMenu(menu)
        // We rather hide options menu actions while actively using the PirateSynchronizer
        menu.setGroupVisible(R.id.main_menu_group, false)
    }

    override fun onDestroy() {
        super.onDestroy()
        reportTraceEvent(SyncBlockchainBenchmarkTrace.Event.BALANCE_SCREEN_END)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val seedPhrase = sharedViewModel.seedPhrase.value
        val seed = Mnemonics.MnemonicCode(seedPhrase).toSeed()
        val network = PirateNetwork.fromResources(requireApplicationContext())

        binding.shield.apply {
            setOnClickListener {
                lifecycleScope.launch {
                    sharedViewModel.synchronizerFlow.value?.shieldFunds(
                        PirateDerivationTool.derivePirateUnifiedSpendingKey(
                            seed,
                            network,
                            Account.DEFAULT
                        )
                    )
                }
            }
        }

        monitorChanges()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun monitorChanges() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.status }
                        .collect { onStatus(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.progress }
                        .collect { onProgress(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.processorInfo }
                        .collect { onProcessorInfoUpdated(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.saplingBalances }
                        .collect { onSaplingBalance(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.orchardBalances }
                        .collect { onOrchardBalance(it) }
                }
                launch {
                    sharedViewModel.synchronizerFlow
                        .filterNotNull()
                        .flatMapLatest { it.transparentBalances }
                        .collect { onTransparentBalance(it) }
                }
            }
        }
    }

    private fun onOrchardBalance(
        orchardBalance: PirateWalletBalance?
    ) {
        binding.orchardBalance.apply {
            text = orchardBalance.humanString()
        }
    }

    private fun onSaplingBalance(
        saplingBalance: PirateWalletBalance?
    ) {
        binding.saplingBalance.apply {
            text = saplingBalance.humanString()
        }
    }

    private fun onTransparentBalance(
        transparentBalance: PirateWalletBalance?
    ) {
        binding.transparentBalance.apply {
            text = transparentBalance.humanString()
        }

        binding.shield.apply {
            // TODO [#776]: Support variable fees
            // TODO [#776]: https://github.com/zcash/zcash-android-wallet-sdk/issues/776
            visibility = if ((transparentBalance?.available ?: Arrrtoshi(0)) > PirateSdk.MINERS_FEE) {
                View.VISIBLE
            } else {
                View.GONE
            }
        }
    }

    private fun onStatus(status: PirateSynchronizer.PirateStatus) {
        twig("PirateSynchronizer status: $status")
        // report benchmark event
        val traceEvents = when (status) {
            PirateSynchronizer.PirateStatus.DOWNLOADING -> {
                listOf(
                    SyncBlockchainBenchmarkTrace.Event.BLOCKCHAIN_SYNC_START,
                    SyncBlockchainBenchmarkTrace.Event.DOWNLOAD_START
                )
            }
            PirateSynchronizer.PirateStatus.VALIDATING -> {
                listOf(
                    SyncBlockchainBenchmarkTrace.Event.DOWNLOAD_END,
                    SyncBlockchainBenchmarkTrace.Event.VALIDATION_START
                )
            }
            PirateSynchronizer.PirateStatus.SCANNING -> {
                listOf(
                    SyncBlockchainBenchmarkTrace.Event.VALIDATION_END,
                    SyncBlockchainBenchmarkTrace.Event.SCAN_START
                )
            }
            PirateSynchronizer.PirateStatus.SYNCED -> {
                listOf(
                    SyncBlockchainBenchmarkTrace.Event.SCAN_END,
                    SyncBlockchainBenchmarkTrace.Event.BLOCKCHAIN_SYNC_END
                )
            }
            else -> null
        }
        traceEvents?.forEach { reportTraceEvent(it) }

        binding.textStatus.text = "Status: $status"
        sharedViewModel.synchronizerFlow.value?.let { synchronizer ->
            onOrchardBalance(synchronizer.orchardBalances.value)
            onSaplingBalance(synchronizer.saplingBalances.value)
            onTransparentBalance(synchronizer.transparentBalances.value)
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

@Suppress("MagicNumber")
private fun PirateWalletBalance?.humanString() = if (null == this) {
    "Calculating balance"
} else {
    """
                Pending balance: ${pending.convertArrrtoshiToArrrString(12)}
                Available balance: ${available.convertArrrtoshiToArrrString(12)}
                Total balance: ${total.convertArrrtoshiToArrrString(12)}
    """.trimIndent()
}
