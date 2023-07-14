@file:Suppress("ktlint:filename")

package pirate.android.sdk.model

/*
 * This is a set of extension functions currently, because we expect them to change in the future.
 */

fun LightWalletEndpoint.Companion.defaultForNetwork(zcashNetwork: PirateNetwork): LightWalletEndpoint {
    return when (zcashNetwork.id) {
        PirateNetwork.Mainnet.id -> LightWalletEndpoint.Mainnet
        PirateNetwork.Testnet.id -> LightWalletEndpoint.Testnet
        else -> error("Unknown network id: ${zcashNetwork.id}")
    }
}

/**
 * This is a special localhost value on the Android emulator, which allows it to contact
 * the localhost of the computer running the emulator.
 */
private const val COMPUTER_LOCALHOST = "10.0.2.2"

private const val DEFAULT_PORT = 443

val LightWalletEndpoint.Companion.Mainnet
    get() = LightWalletEndpoint(
        "lightd1.pirate.black",
        DEFAULT_PORT,
        isSecure = true
    )

val LightWalletEndpoint.Companion.Testnet
    get() = LightWalletEndpoint(
        "lightd1.pirate.black",
        DEFAULT_PORT,
        isSecure = true
    )

val LightWalletEndpoint.Companion.Darkside
    get() = LightWalletEndpoint(
        COMPUTER_LOCALHOST,
        DEFAULT_PORT,
        isSecure = false
    )
