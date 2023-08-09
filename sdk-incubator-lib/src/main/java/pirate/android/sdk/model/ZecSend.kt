package pirate.android.sdk.model

import pirate.android.sdk.Synchronizer

data class ZecSend(val destination: WalletAddress, val amount: Arrrtoshi, val memo: Memo) {
    companion object
}

suspend fun Synchronizer.send(spendingKey: UnifiedSpendingKey, send: ZecSend) = sendToAddress(
    spendingKey,
    send.amount,
    send.destination.address,
    send.memo.value
)
