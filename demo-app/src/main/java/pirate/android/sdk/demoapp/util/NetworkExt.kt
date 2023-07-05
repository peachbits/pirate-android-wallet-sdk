@file:Suppress("ktlint:filename")

package pirate.android.sdk.demoapp.util

import android.content.Context
import pirate.android.sdk.demoapp.R
import pirate.android.sdk.type.PirateNetwork

fun PirateNetwork.Companion.fromResources(context: Context) = PirateNetwork.valueOf(
    context.getString(
        R.string.network_name
    )
)
