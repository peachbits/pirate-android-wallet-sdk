package pirate.android.sdk.internal

import pirate.android.sdk.type.PirateWalletBirthday
import org.json.JSONObject

// Version is not returned from the server, so version 1 is implied.  A version is declared here
// to structure the parsing to be version-aware in the future.
internal val PirateWalletBirthday.Companion.VERSION_1
    get() = 1
internal val PirateWalletBirthday.Companion.KEY_VERSION
    get() = "version"
internal val PirateWalletBirthday.Companion.KEY_HEIGHT
    get() = "height"
internal val PirateWalletBirthday.Companion.KEY_HASH
    get() = "hash"
internal val PirateWalletBirthday.Companion.KEY_EPOCH_SECONDS
    get() = "time"
internal val PirateWalletBirthday.Companion.KEY_TREE
    get() = "saplingTree"

fun PirateWalletBirthday.Companion.from(jsonString: String) = from(JSONObject(jsonString))

private fun PirateWalletBirthday.Companion.from(jsonObject: JSONObject): PirateWalletBirthday {
    when (val version = jsonObject.optInt(PirateWalletBirthday.KEY_VERSION, PirateWalletBirthday.VERSION_1)) {
        PirateWalletBirthday.VERSION_1 -> {
            val height = jsonObject.getInt(PirateWalletBirthday.KEY_HEIGHT)
            val hash = jsonObject.getString(PirateWalletBirthday.KEY_HASH)
            val epochSeconds = jsonObject.getLong(PirateWalletBirthday.KEY_EPOCH_SECONDS)
            val tree = jsonObject.getString(PirateWalletBirthday.KEY_TREE)

            return PirateWalletBirthday(height, hash, epochSeconds, tree)
        }
        else -> {
            throw IllegalArgumentException("Unsupported version $version")
        }
    }
}
