package pirate.android.sdk.type

import pirate.android.sdk.model.BlockHeight

enum class PirateNetwork(
    val id: Int,
    val networkName: String,
    val saplingActivationHeight: BlockHeight,
    val defaultHost: String,
    val defaultPort: Int
) {
    Testnet(0, "testnet", BlockHeight(280_000), "testlightd.pirate.black", 443),
    Mainnet(1, "mainnet", BlockHeight(152_855), "lightd1.pirate.black", 443);

    companion object {
        fun from(id: Int) = values().first { it.id == id }
    }
}
