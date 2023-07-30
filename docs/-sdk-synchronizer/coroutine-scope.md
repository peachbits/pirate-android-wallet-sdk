[zcash-android-wallet-sdk](../../index.md) / [pirate.android.sdk](../index.md) / [PirateSdkSynchronizer](index.md) / [coroutineScope](./coroutine-scope.md)

# coroutineScope

`var coroutineScope: CoroutineScope`

The lifespan of this PirateSynchronizer. This scope is initialized once the PirateSynchronizer starts
because it will be a child of the parentScope that gets passed into the [start](start.md) function.
Everything launched by this PirateSynchronizer will be cancelled once the PirateSynchronizer or its
parentScope stops. This coordinates with [isStarted](is-started.md) so that it fails early
rather than silently, whenever the scope is used before the PirateSynchronizer has been started.

