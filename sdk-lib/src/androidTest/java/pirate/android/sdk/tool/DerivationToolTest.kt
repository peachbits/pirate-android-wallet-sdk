package pirate.android.sdk.tool

import cash.z.ecc.android.bip39.Mnemonics
import pirate.android.sdk.fixture.WalletFixture
import pirate.android.sdk.model.Account
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Test
import kotlin.test.assertContentEquals

class PirateDerivationToolTest {
    @Test
    @OptIn(ExperimentalCoroutinesApi::class)
    fun create_spending_key_does_not_mutate_passed_bytes() = runTest {
        val bytesOne = Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy()
        val bytesTwo = Mnemonics.MnemonicCode(WalletFixture.SEED_PHRASE).toEntropy()

        PirateDerivationTool.derivePirateUnifiedSpendingKey(bytesOne, WalletFixture.NETWORK, Account.DEFAULT)

        assertContentEquals(bytesTwo, bytesOne)
    }
}
