@file:Suppress("ktlint:filename")

package pirate.android.sdk.internal

import java.nio.charset.Charset

private val UTF_8 = Charset.forName("UTF-8")

fun String.sizeInUtf8Bytes() = toByteArray(UTF_8).size
