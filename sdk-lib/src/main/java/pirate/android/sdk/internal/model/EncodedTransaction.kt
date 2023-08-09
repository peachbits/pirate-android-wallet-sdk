package pirate.android.sdk.internal.model

import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.FirstClassByteArray

internal data class EncodedTransaction(
    val txId: FirstClassByteArray,
    val raw: FirstClassByteArray,
    val expiryHeight: BlockHeight?
)
