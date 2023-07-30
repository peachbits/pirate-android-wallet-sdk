[zcash-android-wallet-sdk](../../index.md) / [pirate.android.sdk](../index.md) / [PirateSdkSynchronizer](index.md) / [validateConsensusBranch](./validate-consensus-branch.md)

# validateConsensusBranch

`suspend fun validateConsensusBranch(): `[`ConsensusMatchType`](../../pirate.android.sdk.validate/-consensus-match-type/index.md)

Validate whether the server and this SDK share the same consensus branch. This is
particularly important to check around network updates so that any wallet that's connected to
an incompatible server can surface that information effectively. For the SDK, the consensus
branch is used when creating transactions as each one needs to target a specific branch. This
function compares the server's branch id to this SDK's and returns information that helps
determine whether they match.

**Return**
an instance of [ConsensusMatchType](../../pirate.android.sdk.validate/-consensus-match-type/index.md) that is essentially a wrapper for both branch ids
and provides helper functions for communicating detailed errors to the user.

