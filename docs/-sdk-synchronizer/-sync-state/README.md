[zcash-android-wallet-sdk](../../../index.md) / [pirate.android.sdk.data](../../index.md) / [PirateSdkSynchronizer](../index.md) / [SyncState](./index.md)

# SyncState

`sealed class SyncState`

Represents the initial state of the PirateSynchronizer.

### Types

| Name | Summary |
|---|---|
| [CacheOnly](-cache-only/index.md) | `class CacheOnly : `[`PirateSdkSynchronizer.SyncState`](./index.md)<br>State for when compact blocks have been downloaded but not scanned. This state is typically achieved when the app was previously started but killed before the first scan took place. In this case, we do not need to download compact blocks that we already have. |
| [FirstRun](-first-run.md) | `object FirstRun : `[`PirateSdkSynchronizer.SyncState`](./index.md)<br>State for the first run of the PirateSynchronizer, when the database has not been initialized. |
| [ReadyToProcess](-ready-to-process/index.md) | `class ReadyToProcess : `[`PirateSdkSynchronizer.SyncState`](./index.md)<br>The final state of the PirateSynchronizer, when all initialization is complete and the starting block is known. |

### Inheritors

| Name | Summary |
|---|---|
| [CacheOnly](-cache-only/index.md) | `class CacheOnly : `[`PirateSdkSynchronizer.SyncState`](./index.md)<br>State for when compact blocks have been downloaded but not scanned. This state is typically achieved when the app was previously started but killed before the first scan took place. In this case, we do not need to download compact blocks that we already have. |
| [FirstRun](-first-run.md) | `object FirstRun : `[`PirateSdkSynchronizer.SyncState`](./index.md)<br>State for the first run of the PirateSynchronizer, when the database has not been initialized. |
| [ReadyToProcess](-ready-to-process/index.md) | `class ReadyToProcess : `[`PirateSdkSynchronizer.SyncState`](./index.md)<br>The final state of the PirateSynchronizer, when all initialization is complete and the starting block is known. |
