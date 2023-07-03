package pirate.android.sdk.internal

import androidx.test.filters.SmallTest
import pirate.android.sdk.sdk.type.PirateWalletBirthday
import pirate.fixture.WalletBirthdayFixture
import pirate.fixture.toJson
import org.junit.Assert.assertEquals
import org.junit.Test

class PirateWalletBirthdayTest {
    @Test
    @SmallTest
    fun deserialize() {
        val fixtureBirthday = PirateWalletBirthdayFixture.new()

        val deserialized = PirateWalletBirthday.from(fixtureBirthday.toJson())

        assertEquals(fixtureBirthday, deserialized)
    }

    @Test
    @SmallTest
    fun epoch_seconds_as_long_that_would_overflow_int() {
        val jsonString = PirateWalletBirthdayFixture.new(time = Long.MAX_VALUE).toJson()

        PirateWalletBirthday.from(jsonString).also {
            assertEquals(Long.MAX_VALUE, it.time)
        }
    }
}
