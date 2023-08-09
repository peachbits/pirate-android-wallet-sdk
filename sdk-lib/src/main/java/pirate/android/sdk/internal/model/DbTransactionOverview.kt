package pirate.android.sdk.internal.model

import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.FirstClassByteArray
import pirate.android.sdk.model.Arrrtoshi

internal data class DbTransactionOverview internal constructor(
    val id: Long,
    val rawId: FirstClassByteArray,
    val minedHeight: BlockHeight?,
    val expiryHeight: BlockHeight?,
    val index: Long,
    val raw: FirstClassByteArray?,
    val isSentTransaction: Boolean,
    val netValue: Arrrtoshi,
    val feePaid: Arrrtoshi,
    val isChange: Boolean,
    val receivedNoteCount: Int,
    val sentNoteCount: Int,
    val memoCount: Int,
    val blockTimeEpochSeconds: Long
) {
    override fun toString() = "DbTransactionOverview"
}
