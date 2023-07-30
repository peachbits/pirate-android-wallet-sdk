package pirate.android.sdk.fixture

import cash.z.ecc.android.bip39.Mnemonics
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.tool.PirateDerivationTool

object WalletFixture {
    val NETWORK = PirateNetwork.Mainnet
    const val SEED_PHRASE =
        "kitchen renew wide common vague fold vacuum tilt amazing pear square gossip jewel month tree shock scan alpha just spot fluid toilet view dinner"

    suspend fun getPirateUnifiedSpendingKey(
        seed: String = SEED_PHRASE,
        network: PirateNetwork = NETWORK,
        account: Account = Account.DEFAULT
    ) = PirateDerivationTool.derivePirateUnifiedSpendingKey(Mnemonics.MnemonicCode(seed).toEntropy(), network, account)
}
