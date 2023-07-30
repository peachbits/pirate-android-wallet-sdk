@file:Suppress("ktlint:filename")

package pirate.android.sdk.demoapp.util

import android.content.Context
import pirate.android.sdk.demoapp.R
import pirate.android.sdk.model.PirateNetwork
import java.util.Locale

fun PirateNetwork.Companion.fromResources(context: Context): PirateNetwork {
    val networkNameFromResources = context.getString(R.string.network_name).lowercase(Locale.ROOT)
    @Suppress("UseRequire")
    return if (networkNameFromResources == Testnet.networkName) {
        Testnet
    } else if (networkNameFromResources.lowercase(Locale.ROOT) == Mainnet.networkName) {
        Mainnet
    } else {
        throw IllegalArgumentException("Unknown network name: $networkNameFromResources")
    }
}
