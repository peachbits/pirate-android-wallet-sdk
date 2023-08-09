package pirate.android.sdk.demoapp.fixture

import pirate.android.sdk.Synchronizer
import pirate.android.sdk.block.CompactBlockProcessor
import pirate.android.sdk.demoapp.ui.screen.home.viewmodel.SynchronizerError
import pirate.android.sdk.demoapp.ui.screen.home.viewmodel.WalletSnapshot
import pirate.android.sdk.model.PercentDecimal
import pirate.android.sdk.model.WalletBalance
import pirate.android.sdk.model.Arrrtoshi

@Suppress("MagicNumber")
object WalletSnapshotFixture {

    val STATUS = Synchronizer.Status.SYNCED
    val PROGRESS = PercentDecimal.ZERO_PERCENT
    val TRANSPARENT_BALANCE: WalletBalance = WalletBalance(Arrrtoshi(8), Arrrtoshi(1))
    val ORCHARD_BALANCE: WalletBalance = WalletBalance(Arrrtoshi(5), Arrrtoshi(2))
    val SAPLING_BALANCE: WalletBalance = WalletBalance(Arrrtoshi(4), Arrrtoshi(4))

    // Should fill in with non-empty values for better example values in tests and UI previews
    @Suppress("LongParameterList")
    fun new(
        status: Synchronizer.Status = STATUS,
        processorInfo: CompactBlockProcessor.ProcessorInfo = CompactBlockProcessor.ProcessorInfo(
            null,
            null,
            null,
            null
        ),
        orchardBalance: WalletBalance = ORCHARD_BALANCE,
        saplingBalance: WalletBalance = SAPLING_BALANCE,
        transparentBalance: WalletBalance = TRANSPARENT_BALANCE,
        progress: PercentDecimal = PROGRESS,
        synchronizerError: SynchronizerError? = null
    ) = WalletSnapshot(
        status,
        processorInfo,
        orchardBalance,
        saplingBalance,
        transparentBalance,
        progress,
        synchronizerError
    )
}
