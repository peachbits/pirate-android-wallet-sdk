@file:Suppress("ktlint:filename")

package pirate.android.sdk.internal.db

import android.database.Cursor

internal fun Cursor.optLong(columnIndex: Int): Long? =
    if (isNull(columnIndex)) {
        null
    } else {
        getLong(columnIndex)
    }

internal fun Cursor.optBlobOrThrow(index: Int): ByteArray? {
    return if (isNull(index)) {
        null
    } else {
        getBlob(index)
    }
}
