package pirate.android.sdk.model

/**
 * The Pirate network.  Should be one of [PirateNetwork.Testnet] or [PirateNetwork.Mainnet].
 *
 * The constructor for the network is public to allow for certain test cases to use a custom "darkside" network.
 */
data class PirateNetwork(
    val id: Int,
    val networkName: String,
    val saplingActivationHeight: BlockHeight,
    val orchardActivationHeight: BlockHeight
) {

    @Suppress("MagicNumber")
    companion object {
        const val ID_TESTNET = 0
        const val ID_MAINNET = 1

        // You may notice there are extra checkpoints bundled in the SDK that match the
        // sapling/orchard activation heights.

        val Testnet = PirateNetwork(
            ID_TESTNET,
            "testnet",
            saplingActivationHeight = BlockHeight(152_855),
            orchardActivationHeight = BlockHeight(999_999_999)
        )

        val Mainnet = PirateNetwork(
            ID_MAINNET,
            "mainnet",
            saplingActivationHeight = BlockHeight(152_855),
            orchardActivationHeight = BlockHeight(999_999_999)
        )

        fun from(id: Int) = when (id) {
            0 -> Testnet
            1 -> Mainnet
            else -> throw IllegalArgumentException("Unknown network id: $id")
        }
    }
}
