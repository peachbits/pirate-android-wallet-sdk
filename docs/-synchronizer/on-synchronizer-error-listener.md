[zcash-android-wallet-sdk](../../index.md) / [pirate.android.sdk.data](../index.md) / [PirateSynchronizer](index.md) / [onPirateSynchronizerErrorListener](./on-synchronizer-error-listener.md)

# onPirateSynchronizerErrorListener

`abstract var onPirateSynchronizerErrorListener: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`

Gets or sets a global error listener. This is a useful hook for handling unexpected critical errors.

**Return**
true when the error has been handled and the PirateSynchronizer should continue. False when the error is
unrecoverable and the PirateSynchronizer should [stop](stop.md).

