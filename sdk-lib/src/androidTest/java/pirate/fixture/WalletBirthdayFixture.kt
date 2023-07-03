package pirate.fixture

import pirate.android.sdk.internal.KEY_EPOCH_SECONDS
import pirate.android.sdk.internal.KEY_HASH
import pirate.android.sdk.internal.KEY_HEIGHT
import pirate.android.sdk.internal.KEY_TREE
import pirate.android.sdk.internal.KEY_VERSION
import pirate.android.sdk.internal.VERSION_1
import pirate.android.sdk.type.PirateWalletBirthday
import org.json.JSONObject

object PirateWalletBirthdayFixture {

    // These came from the mainnet 1500000.json file
    const val HEIGHT = 1500000
    const val HASH = "0000000003966e23bb72162a84d2ba0bd4ca5dbf12c4201e79629719725bc05e"
    const val EPOCH_SECONDS = 1627759610L
    @Suppress("MaxLineLength")
    const val TREE = "0144100f5faa5cbcdabbe74821dfd1d0b716e98021f5d5eaeb7d2cfd763dbc5f73001500000102227f31cbcb16b4611e2c5c45ed633743bfc8430093e43ba52b601ec0b3320e01930902c400064d82b1c9aa906c827885840727bf03f395306b01e73cfd28ae54014085cff22927eaa9def1218a0b264d98d28e634edc47b71b84bf9f07077a8b5a00000101a4283d2566498b6d65fb38580c00fdc24ce74107b88147a7d8d6ffe1dd285400000000019ae0f3d14066a5a528c1be988420c6addf38e2eab361ccae22cc5656490af963000144e5aec64759542511e20aa979154a4d9832b939232faa7f5f7e9835bf53ad3500016f0a754e300002fa274c75542010d56b4c241b1ba5e6a78fc64cfa01ac4f746d014cc750cbb900d03a781cb4b4b716012c04848e636a009aff471746c6dd295a1d000001c03877ecdd98378b321250640a1885604d675aaa50380e49da8cfa6ff7deaf15"

    fun new(
        height: Int = HEIGHT,
        hash: String = HASH,
        time: Long = EPOCH_SECONDS,
        tree: String = TREE
    ) = PirateWalletBirthday(height = height, hash = hash, time = time, tree = tree)
}

fun PirateWalletBirthday.toJson() = JSONObject().apply {
    put(PirateWalletBirthday.KEY_VERSION, PirateWalletBirthday.VERSION_1)
    put(PirateWalletBirthday.KEY_HEIGHT, height)
    put(PirateWalletBirthday.KEY_HASH, hash)
    put(PirateWalletBirthday.KEY_EPOCH_SECONDS, time)
    put(PirateWalletBirthday.KEY_TREE, tree)
}.toString()
