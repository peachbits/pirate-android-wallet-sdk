package pirate.android.sdk.demoapp.preference

import android.content.Context
import pirate.android.sdk.demoapp.preference.api.PreferenceProvider
import pirate.android.sdk.demoapp.util.SuspendingLazy

object EncryptedPreferenceSingleton {

    private const val PREF_FILENAME = "co.electriccoin.zcash.encrypted"

    private val lazy = SuspendingLazy<Context, PreferenceProvider> {
        AndroidPreferenceProvider.newEncrypted(it, PREF_FILENAME)
    }

    suspend fun getInstance(context: Context) = lazy.getInstance(context)
}
