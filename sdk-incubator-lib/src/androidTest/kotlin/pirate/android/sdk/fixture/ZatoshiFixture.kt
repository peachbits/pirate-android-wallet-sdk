package pirate.android.sdk.fixture

import pirate.android.sdk.model.Arrrtoshi

object ArrrtoshiFixture {
    @Suppress("MagicNumber")
    const val ZATOSHI_LONG = 123456789L

    fun new(value: Long = ZATOSHI_LONG) = Arrrtoshi(value)
}
