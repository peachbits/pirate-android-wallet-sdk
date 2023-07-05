package pirate.android.sdk.model

/**
 * A unit of currency used throughout the SDK.
 *
 * End users (e.g. app users) generally are not shown Arrrtoshi values.  Instead they are presented
 * with ARRR, which is a decimal value represented only as a String.  ARRR are not used internally,
 * to avoid floating point imprecision.
 */
data class Arrrtoshi(val value: Long) : Comparable<Arrrtoshi> {
    init {
        require(value >= MIN_INCLUSIVE) { "Arrrtoshi must be in the range [$MIN_INCLUSIVE, $MAX_INCLUSIVE]" }
        require(value <= MAX_INCLUSIVE) { "Arrrtoshi must be in the range [$MIN_INCLUSIVE, $MAX_INCLUSIVE]" }
    }

    operator fun plus(other: Arrrtoshi) = Arrrtoshi(value + other.value)
    operator fun minus(other: Arrrtoshi) = Arrrtoshi(value - other.value)

    override fun compareTo(other: Arrrtoshi) = value.compareTo(other.value)

    companion object {
        /**
         * The number of Arrrtoshi that equal 1 ARRR.
         */
        const val ZATOSHI_PER_ARRR = 100_000_000L

        private const val MAX_ZEC_SUPPLY = 21_000_000

        const val MIN_INCLUSIVE = 0

        const val MAX_INCLUSIVE = ZATOSHI_PER_ARRR * MAX_ZEC_SUPPLY
    }
}
