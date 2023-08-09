package pirate.android.sdk.internal.model.ext

import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.PirateNetwork
import pirate.lightwallet.client.model.BlockHeightUnsafe

internal fun BlockHeightUnsafe.Companion.from(blockHeight: BlockHeight) =
    BlockHeightUnsafe(blockHeight.value)

internal fun BlockHeightUnsafe.toBlockHeight(zcashNetwork: PirateNetwork) =
    BlockHeight.new(zcashNetwork, value)
