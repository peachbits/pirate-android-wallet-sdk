[zcash-android-wallet-sdk](../../index.md) / [pirate.android.sdk](../index.md) / [PirateSynchronizer](index.md) / [onCriticalErrorHandler](./on-critical-error-handler.md)

# onCriticalErrorHandler

`abstract var onCriticalErrorHandler: ((`[`Throwable`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-throwable/index.html)`?) -> `[`Boolean`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-boolean/index.html)`)?`

Gets or sets a global error handler. This is a useful hook for handling unexpected critical
errors.

**Return**
true when the error has been handled and the PirateSynchronizer should attempt to continue.
False when the error is unrecoverable and the PirateSynchronizer should [stop](stop.md).

