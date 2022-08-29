package pirate.android.sdk.jni

import pirate.android.sdk.type.UnifiedViewingKey
import pirate.android.sdk.type.WalletBalance
import pirate.android.sdk.type.PirateNetwork

/**
 * Contract defining the exposed capabilities of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 * It is not documented because it is not intended to be used, directly.
 * Instead, use the synchronizer or one of its subcomponents.
 */
interface RustBackendWelding {

    val network: PirateNetwork

    suspend fun createToAddress(
        consensusBranchId: Long,
        account: Int,
        extsk: String,
        to: String,
        value: Long,
        memo: ByteArray? = byteArrayOf()
    ): Long

    suspend fun shieldToAddress(
        extsk: String,
        tsk: String,
        memo: ByteArray? = byteArrayOf()
    ): Long

    suspend fun decryptAndStoreTransaction(tx: ByteArray)

    suspend fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<UnifiedViewingKey>

    suspend fun initAccountsTable(vararg keys: UnifiedViewingKey): Boolean

    suspend fun initBlocksTable(height: Int, hash: String, time: Long, saplingTree: String): Boolean

    suspend fun initDataDb(): Boolean

    fun isValidShieldedAddr(addr: String): Boolean

    fun isValidTransparentAddr(addr: String): Boolean

    suspend fun getShieldedAddress(account: Int = 0): String

    suspend fun getTransparentAddress(account: Int = 0, index: Int = 0): String

    suspend fun getBalance(account: Int = 0): Long

    fun getBranchIdForHeight(height: Int): Long

    suspend fun getReceivedMemoAsUtf8(idNote: Long): String

    suspend fun getSentMemoAsUtf8(idNote: Long): String

    suspend fun getVerifiedBalance(account: Int = 0): Long

//    fun parseTransactionDataList(tdl: LocalRpcTypes.TransactionDataList): LocalRpcTypes.TransparentTransactionList

    suspend fun getNearestRewindHeight(height: Int): Int

    suspend fun rewindToHeight(height: Int): Boolean

    suspend fun scanBlocks(limit: Int = -1): Boolean

    suspend fun validateCombinedChain(): Int

    suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: Int
    ): Boolean

    suspend fun clearUtxos(tAddress: String, aboveHeight: Int = network.saplingActivationHeight - 1): Boolean

    suspend fun getDownloadedUtxoBalance(address: String): WalletBalance

    // Implemented by `DerivationTool`
    interface Derivation {
        suspend fun deriveShieldedAddress(
            viewingKey: String,
            network: PirateNetwork
        ): String

        suspend fun deriveShieldedAddress(
            seed: ByteArray,
            network: PirateNetwork,
            accountIndex: Int = 0,
        ): String

        suspend fun deriveSpendingKeys(
            seed: ByteArray,
            network: PirateNetwork,
            numberOfAccounts: Int = 1,
        ): Array<String>

        suspend fun deriveTransparentAddress(
            seed: ByteArray,
            network: PirateNetwork,
            account: Int = 0,
            index: Int = 0,
        ): String

        suspend fun deriveTransparentAddressFromPublicKey(
            publicKey: String,
            network: PirateNetwork
        ): String

        suspend fun deriveTransparentAddressFromPrivateKey(
            privateKey: String,
            network: PirateNetwork
        ): String

        suspend fun deriveTransparentSecretKey(
            seed: ByteArray,
            network: PirateNetwork,
            account: Int = 0,
            index: Int = 0,
        ): String

        suspend fun deriveViewingKey(
            spendingKey: String,
            network: PirateNetwork
        ): String

        suspend fun deriveUnifiedViewingKeys(
            seed: ByteArray,
            network: PirateNetwork,
            numberOfAccounts: Int = 1,
        ): Array<UnifiedViewingKey>
    }
}
