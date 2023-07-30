package cash.z.wallet.sdk.sample.demoapp.fixture

import cash.z.ecc.android.sdk.demoapp.fixture.WalletAddressFixture
import cash.z.ecc.android.sdk.demoapp.model.Memo
import cash.z.ecc.android.sdk.demoapp.model.WalletAddress
import cash.z.ecc.android.sdk.demoapp.model.ZecSend
import cash.z.ecc.android.sdk.model.Arrrtoshi

object ZecSendFixture {
    const val ADDRESS: String = WalletAddressFixture.UNIFIED_ADDRESS_STRING

    @Suppress("MagicNumber")
    val AMOUNT = Arrrtoshi(123)
    val MEMO = MemoFixture.new()

    suspend fun new(
        address: String = ADDRESS,
        amount: Arrrtoshi = AMOUNT,
        message: Memo = MEMO
    ) = ZecSend(WalletAddress.Unified.new(address), amount, message)
}
