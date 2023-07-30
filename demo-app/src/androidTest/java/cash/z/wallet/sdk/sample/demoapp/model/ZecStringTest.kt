package cash.z.wallet.sdk.sample.demoapp.model

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.filters.SmallTest
import cash.z.ecc.android.sdk.demoapp.model.fromZecString
import cash.z.ecc.android.sdk.demoapp.model.toArrrString
import cash.z.ecc.android.sdk.model.Arrrtoshi
import cash.z.wallet.sdk.sample.demoapp.fixture.MonetarySeparatorsFixture
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import java.util.Locale
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class ZecStringTest {

    companion object {
        private val EN_US_MONETARY_SEPARATORS = MonetarySeparatorsFixture.new()
        private val context = run {
            val applicationContext = ApplicationProvider.getApplicationContext<Context>()
            val enUsConfiguration = Configuration(applicationContext.resources.configuration).apply {
                setLocale(Locale.US)
            }
            applicationContext.createConfigurationContext(enUsConfiguration)
        }
    }

    @Test
    fun empty_string() {
        val actual = Arrrtoshi.fromZecString(context, "", EN_US_MONETARY_SEPARATORS)
        val expected = null

        assertEquals(expected, actual)
    }

    @Test
    fun decimal_monetary_separator() {
        val actual = Arrrtoshi.fromZecString(context, "1.13", EN_US_MONETARY_SEPARATORS)
        val expected = Arrrtoshi(113000000L)

        assertEquals(expected, actual)
    }

    @Test
    fun comma_grouping_separator() {
        val actual = Arrrtoshi.fromZecString(context, "1,130", EN_US_MONETARY_SEPARATORS)
        val expected = Arrrtoshi(113000000000L)

        assertEquals(expected, actual)
    }

    @Test
    fun decimal_monetary_and() {
        val actual = Arrrtoshi.fromZecString(context, "1,130", EN_US_MONETARY_SEPARATORS)
        val expected = Arrrtoshi(113000000000L)

        assertEquals(expected, actual)
    }

    @Test
    @Ignore("https://github.com/zcash/zcash-android-wallet-sdk/issues/412")
    fun toArrrString() {
        val expected = "1.13000000"
        val actual = Arrrtoshi(113000000).toArrrString()

        assertEquals(expected, actual)
    }

    @Test
    @Ignore("https://github.com/zcash/zcash-android-wallet-sdk/issues/412")
    fun round_trip() {
        val expected = Arrrtoshi(113000000L)
        val actual = Arrrtoshi.fromZecString(context, expected.toArrrString(), EN_US_MONETARY_SEPARATORS)

        assertEquals(expected, actual)
    }

    @Test
    fun parse_bad_string() {
        assertNull(Arrrtoshi.fromZecString(context, "", EN_US_MONETARY_SEPARATORS))
        assertNull(Arrrtoshi.fromZecString(context, "+@#$~^&*=", EN_US_MONETARY_SEPARATORS))
        assertNull(Arrrtoshi.fromZecString(context, "asdf", EN_US_MONETARY_SEPARATORS))
    }

    @Test
    fun parse_invalid_numbers() {
        assertNull(Arrrtoshi.fromZecString(context, "", EN_US_MONETARY_SEPARATORS))
        assertNull(Arrrtoshi.fromZecString(context, "1,2", EN_US_MONETARY_SEPARATORS))
        assertNull(Arrrtoshi.fromZecString(context, "1,23,", EN_US_MONETARY_SEPARATORS))
        assertNull(Arrrtoshi.fromZecString(context, "1,234,", EN_US_MONETARY_SEPARATORS))
    }

    @Test
    @SmallTest
    fun overflow_number_test() {
        assertNotNull(Arrrtoshi.fromZecString(context, "21,000,000", EN_US_MONETARY_SEPARATORS))
        assertNull(Arrrtoshi.fromZecString(context, "21,000,001", EN_US_MONETARY_SEPARATORS))
    }
}
