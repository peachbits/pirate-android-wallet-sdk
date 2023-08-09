package pirate.android.sdk.fixture

import pirate.android.sdk.model.CurrencyConversion
import pirate.android.sdk.model.FiatCurrency
import kotlinx.datetime.Instant
import kotlinx.datetime.toInstant

object CurrencyConversionFixture {
    val FIAT_CURRENCY = FiatCurrencyFixture.new()
    val TIMESTAMP = "2022-07-08T11:51:44Z".toInstant()
    const val PRICE_OF_ZEC = 54.98

    fun new(
        fiatCurrency: FiatCurrency = FIAT_CURRENCY,
        timestamp: Instant = TIMESTAMP,
        priceOfZec: Double = PRICE_OF_ZEC
    ) = CurrencyConversion(fiatCurrency, timestamp, priceOfZec)
}
