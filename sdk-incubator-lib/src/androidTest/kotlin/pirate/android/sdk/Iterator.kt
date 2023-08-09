@file:Suppress("ktlint:filename")

package pirate.android.sdk

fun <T> Iterator<T>.count(): Int {
    var count = 0
    forEach { count++ }

    return count
}
