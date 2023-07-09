package pirate.android.sdk.internal

import pirate.android.sdk.model.BlockHeight

internal fun ClosedRange<BlockHeight>?.isEmpty() = this?.isEmpty() ?: true
