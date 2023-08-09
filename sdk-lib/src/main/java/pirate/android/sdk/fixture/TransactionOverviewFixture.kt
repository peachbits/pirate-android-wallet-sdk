package pirate.android.sdk.fixture

import pirate.android.sdk.ext.PirateSdk
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.FirstClassByteArray
import pirate.android.sdk.model.TransactionOverview
import pirate.android.sdk.model.TransactionState
import pirate.android.sdk.model.Arrrtoshi

object TransactionOverviewFixture {
    const val ID: Long = 1
    val RAW_ID: FirstClassByteArray get() = FirstClassByteArray("rawId".toByteArray())
    val MINED_HEIGHT: BlockHeight = BlockHeight(1)
    val EXPIRY_HEIGHT: BlockHeight? = null
    const val INDEX: Long = 2
    val RAW: FirstClassByteArray get() = FirstClassByteArray("raw".toByteArray())
    const val IS_SENT_TRANSACTION: Boolean = false

    @Suppress("MagicNumber")
    val NET_VALUE: Arrrtoshi = Arrrtoshi(10_000)
    val FEE_PAID: Arrrtoshi = PirateSdk.MINERS_FEE
    const val IS_CHANGE: Boolean = false
    const val RECEIVED_NOTE_COUNT: Int = 1
    const val SENT_NOTE_COUNT: Int = 0
    const val MEMO_COUNT: Int = 0
    const val BLOCK_TIME_EPOCH_SECONDS: Long = 1234
    val STATE = TransactionState.Confirmed

    @Suppress("LongParameterList")
    fun new(
        id: Long = ID,
        rawId: FirstClassByteArray = RAW_ID,
        minedHeight: BlockHeight? = MINED_HEIGHT,
        expiryHeight: BlockHeight? = EXPIRY_HEIGHT,
        index: Long = INDEX,
        raw: FirstClassByteArray? = RAW,
        isSentTransaction: Boolean = IS_SENT_TRANSACTION,
        netValue: Arrrtoshi = NET_VALUE,
        feePaid: Arrrtoshi = FEE_PAID,
        isChange: Boolean = IS_CHANGE,
        receivedNoteCount: Int = RECEIVED_NOTE_COUNT,
        sentNoteCount: Int = SENT_NOTE_COUNT,
        memoCount: Int = MEMO_COUNT,
        blockTimeEpochSeconds: Long = BLOCK_TIME_EPOCH_SECONDS,
        transactionState: TransactionState = STATE
    ) = TransactionOverview(
        id,
        rawId,
        minedHeight,
        expiryHeight,
        index,
        raw,
        isSentTransaction,
        netValue,
        feePaid,
        isChange,
        receivedNoteCount,
        sentNoteCount,
        memoCount,
        blockTimeEpochSeconds,
        transactionState
    )
}
