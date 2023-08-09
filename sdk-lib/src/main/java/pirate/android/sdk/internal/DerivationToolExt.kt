package pirate.android.sdk.internal

import pirate.android.sdk.internal.model.JniUnifiedSpendingKey
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.UnifiedFullViewingKey
import pirate.android.sdk.model.UnifiedSpendingKey
import pirate.android.sdk.model.PirateNetwork

fun Derivation.deriveUnifiedAddress(
    seed: ByteArray,
    network: PirateNetwork,
    account: Account
): String = deriveUnifiedAddress(seed, network.id, account.value)

fun Derivation.deriveUnifiedAddress(
    viewingKey: String,
    network: PirateNetwork,
): String = deriveUnifiedAddress(viewingKey, network.id)

fun Derivation.deriveUnifiedSpendingKey(
    seed: ByteArray,
    network: PirateNetwork,
    account: Account
): UnifiedSpendingKey = UnifiedSpendingKey(deriveUnifiedSpendingKey(seed, network.id, account.value))

fun Derivation.deriveUnifiedFullViewingKey(
    usk: UnifiedSpendingKey,
    network: PirateNetwork
): UnifiedFullViewingKey = UnifiedFullViewingKey(
    deriveUnifiedFullViewingKey(
        JniUnifiedSpendingKey(
            usk.account.value,
            usk.copyBytes()
        ),
        network.id
    )
)

fun Derivation.deriveUnifiedFullViewingKeysTypesafe(
    seed: ByteArray,
    network: PirateNetwork,
    numberOfAccounts: Int
): List<UnifiedFullViewingKey> =
    deriveUnifiedFullViewingKeys(seed, network.id, numberOfAccounts).map { UnifiedFullViewingKey(it) }
