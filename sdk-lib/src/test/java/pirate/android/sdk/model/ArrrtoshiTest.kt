package cash.z.ecc.android.sdk.model

import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class ArrrtoshiTest {
    @Test
    fun minValue() {
        assertFailsWith<IllegalArgumentException> {
            Arrrtoshi(Arrrtoshi.MIN_INCLUSIVE - 1L)
        }
    }

    @Test
    fun maxValue() {
        assertFailsWith<IllegalArgumentException> {
            Arrrtoshi(Arrrtoshi.MAX_INCLUSIVE + 1)
        }
    }

    @Test
    fun plus() {
        assertEquals(Arrrtoshi(4), Arrrtoshi(1) + Arrrtoshi(3))
    }

    @Test
    fun minus() {
        assertEquals(Arrrtoshi(3), Arrrtoshi(4) - Arrrtoshi(1))
    }

    @Test
    fun compare_equal() {
        assertEquals(0, Arrrtoshi(1).compareTo(Arrrtoshi(1)))
    }

    @Test
    fun compare_greater() {
        assertTrue(Arrrtoshi(2) > Arrrtoshi(1))
    }

    @Test
    fun compare_less() {
        assertTrue(Arrrtoshi(1) < Arrrtoshi(2))
    }

    @Test
    fun minus_fail() {
        assertFailsWith<IllegalArgumentException> {
            Arrrtoshi(5) - Arrrtoshi(6)
        }
    }
}
