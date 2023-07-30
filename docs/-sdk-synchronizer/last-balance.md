[zcash-android-wallet-sdk](../../index.md) / [pirate.android.sdk.data](../index.md) / [PirateSdkSynchronizer](index.md) / [lastBalance](./last-balance.md)

# lastBalance

`fun lastBalance(): `[`Wallet.PirateWalletBalance`](../../pirate.android.sdk.secure/-wallet/-wallet-balance/index.md)

Overrides [PirateSynchronizer.lastBalance](../-synchronizer/last-balance.md)

Holds the most recent value that was transmitted through the [balances](../-synchronizer/balances.md) channel. Typically, if the
underlying channel is a BroadcastChannel (and it should be), then this value is simply [balanceChannel.value](#)

