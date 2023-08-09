package pirate.android.sdk.fixture

import pirate.android.sdk.model.FiatCurrency

object FiatCurrencyFixture {
    const val USD = "USD"

    fun new(code: String = USD) = FiatCurrency(code)
}
