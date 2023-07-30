[zcash-android-wallet-sdk](../../index.md) / [pirate.android.sdk.data](../index.md) / [PirateSdkSynchronizer](index.md) / [activeTransactions](./active-transactions.md)

# activeTransactions

`fun activeTransactions(): ReceiveChannel<`[`Map`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin.collections/-map/index.html)`<`[`ActiveTransaction`](../-active-transaction/index.md)`, `[`TransactionState`](../-transaction-state/index.md)`>>`

Overrides [PirateSynchronizer.activeTransactions](../-synchronizer/active-transactions.md)

A stream of all the wallet transactions, delegated to the [activeTransactionManager](#).

