package pirate.wallet.sdk.sample.demoapp.preference.fixture

import pirate.android.sdk.demoapp.preference.model.entry.BooleanPreferenceDefault
import pirate.android.sdk.demoapp.preference.model.entry.Key

object BooleanPreferenceDefaultFixture {
    val KEY = Key("some_boolean_key") // $NON-NLS
    fun newTrue() = BooleanPreferenceDefault(KEY, true)
    fun newFalse() = BooleanPreferenceDefault(KEY, false)
}
