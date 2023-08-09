package pirate.android.sdk.fixture

import pirate.android.sdk.model.Locale

object LocaleFixture {
    const val LANGUAGE = "en"
    const val COUNTRY = "US"
    val VARIANT: String? = null

    fun new(
        language: String = LANGUAGE,
        country: String? = COUNTRY,
        variant: String? = VARIANT
    ) = Locale(language, country, variant)
}
