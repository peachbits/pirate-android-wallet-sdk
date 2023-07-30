package pirate.android.sdk.internal.model

import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.FirstClassByteArray

internal data class Block(
    val height: BlockHeight,
    val hash: FirstClassByteArray,
    val time: Int,
    val saplingTree: FirstClassByteArray
)
