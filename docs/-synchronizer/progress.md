[zcash-android-wallet-sdk](../../index.md) / [pirate.android.sdk](../index.md) / [PirateSynchronizer](index.md) / [progress](./progress.md)

# progress

`abstract val progress: Flow<`[`Int`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-int/index.html)`>`

A flow of progress values, typically corresponding to this PirateSynchronizer downloading blocks.
Typically, any non- zero value below 100 indicates that progress indicators can be shown and
a value of 100 signals that progress is complete and any progress indicators can be hidden.

