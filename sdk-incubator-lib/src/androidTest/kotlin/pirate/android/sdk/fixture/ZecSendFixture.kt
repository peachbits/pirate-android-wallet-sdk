package pirate.android.sdk.fixture

import pirate.android.sdk.model.Memo
import pirate.android.sdk.model.WalletAddress
import pirate.android.sdk.model.Arrrtoshi
import pirate.android.sdk.model.ZecSend

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
