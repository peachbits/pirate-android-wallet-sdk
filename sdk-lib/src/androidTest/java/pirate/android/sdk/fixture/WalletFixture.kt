package pirate.android.sdk.fixture

import cash.z.ecc.android.bip39.Mnemonics
import pirate.android.sdk.internal.deriveUnifiedSpendingKey
import pirate.android.sdk.internal.jni.RustDerivationTool
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.PirateNetwork

object WalletFixture {
    val NETWORK = PirateNetwork.Mainnet
    const val SEED_PHRASE = "kitchen renew wide common vague fold vacuum tilt amazing pear square gossip jewel month" +
        " tree shock scan alpha just spot fluid toilet view dinner"

    suspend fun getUnifiedSpendingKey(
        seed: String = SEED_PHRASE,
        network: PirateNetwork = NETWORK,
        account: Account = Account.DEFAULT
    ) = RustDerivationTool.new().deriveUnifiedSpendingKey(Mnemonics.MnemonicCode(seed).toEntropy(), network, account)
}
