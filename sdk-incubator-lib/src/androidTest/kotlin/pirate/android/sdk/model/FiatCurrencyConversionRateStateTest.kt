package pirate.android.sdk.model

import androidx.test.filters.SmallTest
import pirate.android.sdk.fixture.CurrencyConversionFixture
import pirate.android.sdk.fixture.LocaleFixture
import pirate.android.sdk.fixture.MonetarySeparatorsFixture
import pirate.android.sdk.fixture.ArrrtoshiFixture
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import org.junit.Test
import kotlin.test.assertIs
import kotlin.time.Duration.Companion.seconds

class FiatCurrencyConversionRateStateTest {
    @Test
    @SmallTest
    fun future_near() {
        val zatoshi = ArrrtoshiFixture.new()

        val frozenClock = FrozenClock(
            CurrencyConversionFixture.TIMESTAMP - FiatCurrencyConversionRateState.FUTURE_CUTOFF_AGE_INCLUSIVE
        )

        val currencyConversion = CurrencyConversionFixture.new()

        val result = zatoshi.toFiatCurrencyState(
            currencyConversion,
            LocaleFixture.new(),
            MonetarySeparatorsFixture.new(),
            frozenClock
        )

        assertIs<FiatCurrencyConversionRateState.Current>(result)
    }

    @Test
    @SmallTest
    fun future_far() {
        val zatoshi = ArrrtoshiFixture.new()

        val frozenClock = FrozenClock(
            CurrencyConversionFixture.TIMESTAMP -
                FiatCurrencyConversionRateState.FUTURE_CUTOFF_AGE_INCLUSIVE -
                1.seconds
        )

        val currencyConversion = CurrencyConversionFixture.new()

        val result = zatoshi.toFiatCurrencyState(
            currencyConversion,
            LocaleFixture.new(),
            MonetarySeparatorsFixture.new(),
            frozenClock
        )

        assertIs<FiatCurrencyConversionRateState.Unavailable>(result)
    }

    @Test
    @SmallTest
    fun current() {
        val zatoshi = ArrrtoshiFixture.new()

        val frozenClock = FrozenClock(CurrencyConversionFixture.TIMESTAMP)

        val currencyConversion = CurrencyConversionFixture.new(
            timestamp = CurrencyConversionFixture.TIMESTAMP - 1.seconds
        )

        val result = zatoshi.toFiatCurrencyState(
            currencyConversion,
            LocaleFixture.new(),
            MonetarySeparatorsFixture.new(),
            frozenClock
        )

        assertIs<FiatCurrencyConversionRateState.Current>(result)
    }

    @Test
    @SmallTest
    fun stale() {
        val zatoshi = ArrrtoshiFixture.new()

        val frozenClock = FrozenClock(CurrencyConversionFixture.TIMESTAMP)

        val currencyConversion = CurrencyConversionFixture.new(
            timestamp = CurrencyConversionFixture.TIMESTAMP -
                FiatCurrencyConversionRateState.CURRENT_CUTOFF_AGE_INCLUSIVE -
                1.seconds
        )

        val result = zatoshi.toFiatCurrencyState(
            currencyConversion,
            LocaleFixture.new(),
            MonetarySeparatorsFixture.new(),
            frozenClock
        )

        assertIs<FiatCurrencyConversionRateState.Stale>(result)
    }

    @Test
    @SmallTest
    fun too_stale() {
        val zatoshi = ArrrtoshiFixture.new()

        val frozenClock = FrozenClock(CurrencyConversionFixture.TIMESTAMP)

        val currencyConversion = CurrencyConversionFixture.new(
            timestamp = CurrencyConversionFixture.TIMESTAMP -
                FiatCurrencyConversionRateState.STALE_CUTOFF_AGE_INCLUSIVE -
                1.seconds
        )

        val result = zatoshi.toFiatCurrencyState(
            currencyConversion,
            LocaleFixture.new(),
            MonetarySeparatorsFixture.new(),
            frozenClock
        )

        assertIs<FiatCurrencyConversionRateState.Unavailable>(result)
    }

    @Test
    @SmallTest
    fun null_conversion_rate() {
        val zatoshi = ArrrtoshiFixture.new()

        val result = zatoshi.toFiatCurrencyState(
            null,
            LocaleFixture.new(),
            MonetarySeparatorsFixture.new()
        )

        assertIs<FiatCurrencyConversionRateState.Unavailable>(result)
    }
}

private class FrozenClock(private val timestamp: Instant) : Clock {
    override fun now() = timestamp
}
