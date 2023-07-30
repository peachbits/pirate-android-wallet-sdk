package pirate.android.sdk.internal.model

import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.FirstClassByteArray

internal data class CompactBlock(
    val height: BlockHeight,
    val data: FirstClassByteArray
)
