[zcash-android-wallet-sdk](../../index.md) / [pirate.android.sdk](../index.md) / [SdkSynchronizer](index.md) / [validateAddress](./validate-address.md)

# validateAddress

`suspend fun validateAddress(address: `[`String`](https://kotlinlang.org/api/latest/jvm/stdlib/kotlin/-string/index.html)`): `[`AddressType`](../../pirate.android.sdk.validate/-address-type/index.md)

Validates the given address, returning information about why it is invalid. This is a
convenience method that combines the behavior of [isValidShieldedAddr](../-synchronizer/is-valid-shielded-addr.md) and
[isValidTransparentAddr](../-synchronizer/is-valid-transparent-addr.md) into one call so that the developer doesn't have to worry about
handling the exceptions that they throw. Rather, exceptions are converted to
[AddressType.Invalid](../../pirate.android.sdk.validate/-address-type/-invalid/index.md) which has a `reason` property describing why it is invalid.

### Parameters

`address` - the address to validate.

**Return**
an instance of [AddressType](../../pirate.android.sdk.validate/-address-type/index.md) providing validation info regarding the given address.

