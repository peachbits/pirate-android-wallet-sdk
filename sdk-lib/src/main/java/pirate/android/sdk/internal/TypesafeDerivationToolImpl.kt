package pirate.android.sdk.internal

import pirate.android.sdk.model.Account
import pirate.android.sdk.model.UnifiedFullViewingKey
import pirate.android.sdk.model.UnifiedSpendingKey
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.tool.DerivationTool

internal class TypesafeDerivationToolImpl(private val derivation: Derivation) : DerivationTool {

    override suspend fun deriveUnifiedFullViewingKeys(
        seed: ByteArray,
        network: PirateNetwork,
        numberOfAccounts: Int
    ): List<UnifiedFullViewingKey> = derivation.deriveUnifiedFullViewingKeysTypesafe(seed, network, numberOfAccounts)

    override suspend fun deriveUnifiedFullViewingKey(
        usk: UnifiedSpendingKey,
        network: PirateNetwork
    ): UnifiedFullViewingKey = derivation.deriveUnifiedFullViewingKey(usk, network)

    override suspend fun deriveUnifiedSpendingKey(
        seed: ByteArray,
        network: PirateNetwork,
        account: Account
    ): UnifiedSpendingKey = derivation.deriveUnifiedSpendingKey(seed, network, account)

    override suspend fun deriveUnifiedAddress(
        seed: ByteArray,
        network: PirateNetwork,
        account: Account
    ): String = derivation.deriveUnifiedAddress(seed, network, account)

    override suspend fun deriveUnifiedAddress(
        viewingKey: String,
        network: PirateNetwork,
    ): String = derivation.deriveUnifiedAddress(viewingKey, network)
}
