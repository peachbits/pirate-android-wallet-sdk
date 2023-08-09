package pirate.android.sdk.fixture

import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.PersistableWallet
import pirate.android.sdk.model.SeedPhrase
import pirate.android.sdk.model.PirateNetwork

object PersistableWalletFixture {

    val NETWORK = PirateNetwork.Testnet

    // These came from the mainnet 1500000.json file
    @Suppress("MagicNumber")
    val BIRTHDAY = BlockHeight.new(PirateNetwork.Mainnet, 1500000L)

    val SEED_PHRASE = SeedPhraseFixture.new()

    fun new(
        network: PirateNetwork = NETWORK,
        birthday: BlockHeight = BIRTHDAY,
        seedPhrase: SeedPhrase = SEED_PHRASE
    ) = PersistableWallet(network, birthday, seedPhrase)
}
