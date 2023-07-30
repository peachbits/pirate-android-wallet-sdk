package pirate.android.sdk.demoapp.preference

import pirate.android.sdk.demoapp.model.PersistableWallet
import pirate.android.sdk.demoapp.preference.api.PreferenceProvider
import pirate.android.sdk.demoapp.preference.model.entry.Key
import pirate.android.sdk.demoapp.preference.model.entry.PreferenceDefault
import org.json.JSONObject

data class PersistableWalletPreferenceDefault(
    override val key: Key
) : PreferenceDefault<PersistableWallet?> {

    override suspend fun getValue(preferenceProvider: PreferenceProvider) =
        preferenceProvider.getString(key)?.let { PersistableWallet.from(JSONObject(it)) }

    override suspend fun putValue(
        preferenceProvider: PreferenceProvider,
        newValue: PersistableWallet?
    ) = preferenceProvider.putString(key, newValue?.toJson()?.toString())
}
