package pirate.android.sdk.demoapp.fixture

import pirate.android.sdk.PirateSynchronizer
import pirate.android.sdk.block.PirateCompactBlockProcessor
import pirate.android.sdk.demoapp.model.PercentDecimal
import pirate.android.sdk.demoapp.ui.screen.home.viewmodel.PirateSynchronizerError
import pirate.android.sdk.demoapp.ui.screen.home.viewmodel.WalletSnapshot
import pirate.android.sdk.model.PirateWalletBalance
import pirate.android.sdk.model.Arrrtoshi

@Suppress("MagicNumber")
object WalletSnapshotFixture {

    val STATUS = PirateSynchronizer.PirateStatus.SYNCED
    val PROGRESS = PercentDecimal.ZERO_PERCENT
    val TRANSPARENT_BALANCE: PirateWalletBalance = PirateWalletBalance(Arrrtoshi(8), Arrrtoshi(1))
    val ORCHARD_BALANCE: PirateWalletBalance = PirateWalletBalance(Arrrtoshi(5), Arrrtoshi(2))
    val SAPLING_BALANCE: PirateWalletBalance = PirateWalletBalance(Arrrtoshi(4), Arrrtoshi(4))

    // Should fill in with non-empty values for better example values in tests and UI previews
    @Suppress("LongParameterList")
    fun new(
        status: PirateSynchronizer.PirateStatus = STATUS,
        processorInfo: PirateCompactBlockProcessor.ProcessorInfo = PirateCompactBlockProcessor.ProcessorInfo(
            null,
            null,
            null,
            null,
            null
        ),
        orchardBalance: PirateWalletBalance = ORCHARD_BALANCE,
        saplingBalance: PirateWalletBalance = SAPLING_BALANCE,
        transparentBalance: PirateWalletBalance = TRANSPARENT_BALANCE,
        pendingCount: Int = 0,
        progress: PercentDecimal = PROGRESS,
        synchronizerError: PirateSynchronizerError? = null
    ) = WalletSnapshot(
        status,
        processorInfo,
        orchardBalance,
        saplingBalance,
        transparentBalance,
        pendingCount,
        progress,
        synchronizerError
    )
}
