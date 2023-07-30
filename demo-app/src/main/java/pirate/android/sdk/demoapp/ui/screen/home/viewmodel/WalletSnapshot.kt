package pirate.android.sdk.demoapp.ui.screen.home.viewmodel

import pirate.android.sdk.PirateSynchronizer
import pirate.android.sdk.block.PirateCompactBlockProcessor
import pirate.android.sdk.demoapp.model.PercentDecimal
import pirate.android.sdk.ext.PirateSdk
import pirate.android.sdk.model.PirateWalletBalance
import pirate.android.sdk.model.Arrrtoshi

data class WalletSnapshot(
    val status: PirateSynchronizer.PirateStatus,
    val processorInfo: PirateCompactBlockProcessor.ProcessorInfo,
    val orchardBalance: PirateWalletBalance,
    val saplingBalance: PirateWalletBalance,
    val transparentBalance: PirateWalletBalance,
    val pendingCount: Int,
    val progress: PercentDecimal,
    val synchronizerError: PirateSynchronizerError?
) {
    // Note: the wallet is effectively empty if it cannot cover the miner's fee
    val hasFunds = saplingBalance.available.value >
        (PirateSdk.MINERS_FEE.value.toDouble() / Arrrtoshi.ARRRTOSHI_PER_ARRR) // 0.00001
    val hasSaplingBalance = saplingBalance.total.value > 0

    val isSendEnabled: Boolean get() = status == PirateSynchronizer.PirateStatus.SYNCED && hasFunds
}

fun WalletSnapshot.totalBalance() = orchardBalance.total + saplingBalance.total + transparentBalance.total

// Note that considering both to be spendable is subject to change.
// The user experience could be confusing, and in the future we might prefer to ask users
// to transfer their balance to the latest balance type to make it spendable.
fun WalletSnapshot.spendableBalance() = orchardBalance.available + saplingBalance.available
