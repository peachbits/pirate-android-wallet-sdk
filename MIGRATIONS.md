Troubleshooting Migrations
==========

Migration to Version 1.11
---------------------------------
The way the SDK is initialized has changed.  The `Initializer` object has been removed and `PirateSynchronizer.new` now takes a longer parameter list which includes the parameters previously passed to `Initializer`.

SDK initialization also now requires access to the seed bytes at two times: 1. during new wallet creation and 2. during upgrade of an existing wallet to SDK 1.10 due to internal data migrations.  To handle case #2, client should wrap `PirateSynchronizer.new()` with a try-catch for `PirateInitializerException.SeedRequired`.  Clients can pass `null` to try to initialize the SDK without the seed, then try again if the exception is thrown to indicate the seed is needed.  This pattern future-proofs initialization, as the seed may be required by future SDK updates.

`PirateSynchronizer.stop()` has been removed.  `PirateSynchronizer.new()` now returns an instance that implements the `Closeable` interface.  This effectively means that calls to `stop()` are replaced with `close()`.  This change also enables greater safety within client applications, as the Closeable interface can be hidden from global synchronizer instances.  For exmaple:
```
val synchronizerFlow: Flow<PirateSynchronizer> = callbackFlow<PirateSynchronizer> {
   val closeablePirateSynchronizer: CloseablePirateSynchronizer = PirateSynchronizer.new(...)

    send(closeablePirateSynchronizer)
    awaitClose {
        closeablePirateSynchronizer.close()
    }
}
```

To improve type safety of the public API, Zcash account indexes are now represented by an `Account` object.  The SDK currently only supports the default account, `Account.DEFAULT`.  Migration will effectively require replacing APIs with an account `0` with `Account.DEFAULT`.

To support Network Upgrade 5, the way keys are generated has changed.

For SDK clients that regenerate the keys from a mnemonic, migration might look like:
 * Replace usage of `PirateUnifiedViewingKey` with `PirateUnifiedFullViewingKey`
 * Replace `PirateDerivationTool.derivePirateUnifiedViewingKeys` with `PirateDerivationTool.derivePirateUnifiedFullViewingKeys`

For SDK clients that store the key separately from the mnemonic, the migration might look like:
 * Replace usage of `PirateUnifiedViewingKey(extfvk: String, extpub: String)` with `PirateUnifiedFullViewingKey(encoding: String)`
 * Replace `PirateDerivationTool.derivePirateUnifiedViewingKeys` with `PirateDerivationTool.derivePirateUnifiedFullViewingKeys`
 * Delete any previously persisted values for `PirateUnifiedViewingKey(extfvk: String, extpub: String)`,
   provided that they can be rederived from the mnemonic.
 * Re-generate the key from the mnemonic using `PirateDerivationTool.derivePirateUnifiedFullViewingKeys`

To support Unified Addresses (UAs), some APIs have been modified.  In particular, `PirateSynchronizer.getUnifiedAddress()` returns the unified address while `PirateSynchronizer.getSaplingAddress()` and `PirateSynchronizer.getTransparentAddress()` return the sapling or transparent components of the unified address.  Due to this change and the derivation of different addresses from UAs, clients may notice that the transparent address returned by this API is different from the transparent address returned by older versions of the SDK.  Note that UA support does not yet encompass orchard addresses.

Due to internal changes in the SDK, the way transactions are queried and represented works differently.  The previous ConfirmedTransaction object has been replaced with `TransactionOverview` which contains less information.  Missing fields, such as memos and recipients, can be queried with `PirateSynchronizer.getMemos(TransactionOverview)` and `PirateSynchronizer.getReceipients(TransactionOverview)`.

Migration to Version 1.9
--------------------------------------
`PirateNetwork` is no longer an enum. The prior enum values are now declared as object properties `PirateNetwork.Mainnet` and `PirateNetwork.Testnet`.  For the most part, this change should have minimal impact.  PirateNetwork was also moved from the package `cash.z.ecc.android.sdk.type` to `cash.z.ecc.android.sdk.model`, which will require a change to your import statements.  The server fields have been removed from `PirateNetwork`, allowing server and network configuration to be done independently.

`LightWalletEndpoint` is a new object to represent server information.  Default values can be obtained from `LightWalletEndpoint.defaultForNetwork(PirateNetwork)`

`PirateSynchronizer` no longer allows changing the endpoint after construction.  Instead, construct a new `PirateSynchronizer` with the desired endpoint.

Migration to Version 1.8 from 1.7
Various APIs used `Int` to represent network block heights.  Those APIs now use a typesafe `BlockHeight` type.  BlockHeight is constructed with a factory method `BlockHeight.new(PirateNetwork, Long)` which uses the network to validate the height is above the network's sapling activation height.

`WalletBirthday` has been renamed to `Checkpoint` and removed from the public API.  Where clients previously passed in a `WalletBirthday` object, now a `BlockHeight` can be passed in instead.

Migration to Version 1.7 from 1.6
--------------------------------------
Various APIs used `Long` value to represent Arrrtoshi currency amounts.  Those APIs now use a typesafe `Arrrtoshi` class.  When passing amounts, simply wrap Long values with the Arrrtoshi constructor `Arrrtoshi(Long)`.  When receiving values, simply unwrap Long values with `Arrrtoshi.value`.

`PirateWalletBalance` no longer has uninitialized default values.  This means that `PirateSynchronizer` fields that expose a PirateWalletBalance now use `null` to signal an uninitialized value.  Specifically this means `PirateSynchronizer.orchardBalances`, `Synchronzier.saplingBalances`, and `PirateSynchronizer.transparentBalances` have nullable values now.

`PirateWalletBalance` has been moved from the package `cash.z.ecc.android.sdk.type` to `cash.z.ecc.android.sdk.model`

`PirateSdk.ARRRTOSHI_PER_ARRR` has been moved to `Arrrtoshi.ARRRTOSHI_PER_ARRR`.

`PirateSdk.MINERS_FEE_ZATOSHI` has been renamed to `PirateSdk.MINERS_FEE` and the type has changed from `Long` to `Arrrtoshi`.

Migrating to Version 1.4.* from 1.3.*
--------------------------------------
The main entrypoint to the SDK has changed.

Previously, a PirateSynchronizer was initialized with `PirateSynchronizer(initializer)` and now it is initialized with `PirateSynchronizer.new(initializer)` which is also now a suspending function.  Helper methods `PirateSynchronizer.newBlocking()` and `PirateInitializer.newBlocking()` can be used to ease the transition.

For clients needing more complex initialization, the previous default method arguments for `PirateSynchronizer()` were moved to `DefaultPirateSynchronizerFactory`.

The minimum Android version supported is now API 19.

Migrating to Version 1.3.0-beta18 from 1.3.0-beta19
--------------------------------------
Various APIs that have always been considered private have been moved into a new package called `internal`.  While this should not be a breaking change, clients that might have relied on these internal classes should stop doing so.  If necessary, these calls can be migrated by changing the import to the new `internal` package name.

A number of methods have been converted to suspending functions, because they were performing slow or blocking calls (e.g. disk IO) internally.  This is a breaking change.

Migrating to Version 1.3.* from 1.2.*
--------------------------------------
The biggest breaking changes in 1.3 that inspired incrementing the minor version number was simplifying down to one "network aware" library rather than two separate libraries, each dedicated to either testnet or mainnet. This greatly simplifies the gradle configuration and has lots of other benefits. Wallets can now set a network with code similar to the following:

```kotlin
// Simple example
val network: PirateNetwork = if (testMode) PirateNetwork.Testnet else PirateNetwork.Mainnet

// Dependency Injection example
@Provides @Singleton fun provideNetwork(): PirateNetwork = PirateNetwork.Mainnet
```
1.3 also adds a runtime check for wallets that are accessing properties before the synchronizer has started. By introducing a `prepare` step, we are now able to catch these errors proactively rather than allowing them to turn into subtle bugs that only surface later. We found this when code was accessing properties before database migrations completed, causing undefined results. Developers do not need to make any changes to enable these checks, they happen automatically and result in detailed error messages.

| Error                           | Issue                               | Fix                      |
| ------------------------------- | ----------------------------------- | ------------------------ |
| No value passed for parameter 'network' | Many functions are now network-aware | pass an instance of PirateNetwork, which is typically set during initialization |
| Unresolved reference: validate  | The `validate` package was removed  | instead of `pirate.android.sdk.validate.PirateAddressType`<br/>import `pirate.android.sdk.type.PirateAddressType`  |
| Unresolved reference: PirateWalletBalance | PirateWalletBalance was moved out of `PirateCompactBlockProcessor` and up to the `type` package  | instead of `pirate.android.sdk.PirateCompactBlockProcessor.PirateWalletBalance`<br/>import `pirate.android.sdk.type.PirateWalletBalance`  |
| Unresolved reference: server  | This was replaced by `setNetwork` | instead of `config.server(host, port)`<br/>use `config.setNetwork(network, host, port)` |
| Unresolved reference: balances  | 3 types of balances are now exposed | change `balances` to `saplingBalances` |
| Unresolved reference: latestBalance  | There are now multiple balance types so this convenience function was removed in favor of forcing wallets to think about which balances they want to show.  | In most cases, just use `synchronizer.saplingBalances.value` directly, instead |
| Type mismatch: inferred type is String but PirateNetwork was expected  | This function is now network aware | use `PirateInitializer.erase(context, network, alias)` |
| Type mismatch: inferred type is Int? but PirateNetwork was expected | This function is now network aware | use `WalletBirthdayTool.loadNearest(context, network, height)` instead |
| None of the following functions can be called with the arguments supplied: <br/>public open fun deriveShieldedAddress(seed: ByteArray, network: PirateNetwork, accountIndex: Int = ...): String defined in pirate.android.sdk.tool.PirateDerivationTool.Companion<br/>public open fun deriveShieldedAddress(viewingKey: String, network: PirateNetwork): String defined in pirate.android.sdk.tool.PirateDerivationTool.Companion | This function is now network aware | use `deriveShieldedAddress(seed, network)`|
