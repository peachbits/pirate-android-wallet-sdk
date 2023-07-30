package cash.z.wallet.sdk.sample.demoapp.fixture

import cash.z.ecc.android.sdk.demoapp.model.PersistableWallet
import cash.z.ecc.android.sdk.demoapp.model.SeedPhrase
import cash.z.ecc.android.sdk.model.BlockHeight
import cash.z.ecc.android.sdk.model.PirateNetwork

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
