package pirate.android.sdk.demoapp.ui.screen.home.viewmodel

import pirate.android.sdk.Synchronizer
import pirate.android.sdk.block.CompactBlockProcessor
import pirate.android.sdk.ext.PirateSdk
import pirate.android.sdk.model.PercentDecimal
import pirate.android.sdk.model.WalletBalance
import pirate.android.sdk.model.Arrrtoshi

data class WalletSnapshot(
    val status: Synchronizer.Status,
    val processorInfo: CompactBlockProcessor.ProcessorInfo,
    val orchardBalance: WalletBalance,
    val saplingBalance: WalletBalance,
    val transparentBalance: WalletBalance,
    val progress: PercentDecimal,
    val synchronizerError: SynchronizerError?
) {
    // Note: the wallet is effectively empty if it cannot cover the miner's fee
    val hasFunds = saplingBalance.available.value >
        (PirateSdk.MINERS_FEE.value.toDouble() / Arrrtoshi.ARRRTOSHI_PER_ARRR) // 0.00001
    val hasSaplingBalance = saplingBalance.total.value > 0

    val isSendEnabled: Boolean get() = status == Synchronizer.Status.SYNCED && hasFunds
}

fun WalletSnapshot.totalBalance() = orchardBalance.total + saplingBalance.total + transparentBalance.total

// Note that considering both to be spendable is subject to change.
// The user experience could be confusing, and in the future we might prefer to ask users
// to transfer their balance to the latest balance type to make it spendable.
fun WalletSnapshot.spendableBalance() = orchardBalance.available + saplingBalance.available
