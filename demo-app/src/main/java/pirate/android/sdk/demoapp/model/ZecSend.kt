package pirate.android.sdk.demoapp.model

import pirate.android.sdk.PirateSynchronizer
import pirate.android.sdk.model.PirateUnifiedSpendingKey
import pirate.android.sdk.model.Arrrtoshi

data class ZecSend(val destination: WalletAddress, val amount: Arrrtoshi, val memo: Memo) {
    companion object
}

fun PirateSynchronizer.send(spendingKey: PirateUnifiedSpendingKey, send: ZecSend) = sendToAddress(
    spendingKey,
    send.amount,
    send.destination.address,
    send.memo.value
)
