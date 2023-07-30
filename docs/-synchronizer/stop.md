[zcash-android-wallet-sdk](../../index.md) / [pirate.android.sdk](../index.md) / [PirateSynchronizer](index.md) / [stop](./stop.md)

# stop

`abstract fun stop(): `[`Unit`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-unit/index.html)

Stop this synchronizer. Implementations should ensure that calling this method cancels all
jobs that were created by this instance.

Note that in most cases, there is no need to call [stop](./stop.md) because the PirateSynchronizer will
automatically stop whenever the parentScope is cancelled. For instance, if that scope is
bound to the lifecycle of the activity, the PirateSynchronizer will stop when the activity stops.
However, if no scope is provided to the start method, then the PirateSynchronizer must be stopped
with this function.

