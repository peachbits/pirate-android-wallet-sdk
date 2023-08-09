package pirate.android.sdk.fixture

import pirate.android.sdk.model.Memo

object MemoFixture {
    const val MEMO_STRING = "Thanks for lunch"

    fun new(memoString: String = MEMO_STRING) = Memo(memoString)
}
