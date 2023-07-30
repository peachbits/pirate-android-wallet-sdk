package pirate.android.sdk.jni

import pirate.android.sdk.internal.model.Checkpoint
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.PirateUnifiedSpendingKey
import pirate.android.sdk.model.PirateWalletBalance
import pirate.android.sdk.model.Arrrtoshi
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.type.PirateUnifiedFullViewingKey
import java.io.File

/**
 * Contract defining the exposed capabilities of the Rust backend.
 * This is what welds the SDK to the Rust layer.
 * It is not documented because it is not intended to be used, directly.
 * Instead, use the synchronizer or one of its subcomponents.
 */
@Suppress("TooManyFunctions")
internal interface PirateRustBackendWelding {

    val network: PirateNetwork

    val saplingParamDir: File

    @Suppress("LongParameterList")
    suspend fun createToAddress(
        usk: PirateUnifiedSpendingKey,
        to: String,
        value: Long,
        memo: ByteArray? = byteArrayOf()
    ): Long

    suspend fun shieldToAddress(
        usk: PirateUnifiedSpendingKey,
        memo: ByteArray? = byteArrayOf()
    ): Long

    suspend fun decryptAndStoreTransaction(tx: ByteArray)

    suspend fun initAccountsTable(seed: ByteArray, numberOfAccounts: Int): Array<PirateUnifiedFullViewingKey>

    suspend fun initAccountsTable(vararg keys: PirateUnifiedFullViewingKey): Boolean

    suspend fun initBlocksTable(checkpoint: Checkpoint): Boolean

    suspend fun initDataDb(seed: ByteArray?): Int

    suspend fun createAccount(seed: ByteArray): PirateUnifiedSpendingKey

    fun isValidShieldedAddr(addr: String): Boolean

    fun isValidTransparentAddr(addr: String): Boolean

    fun isValidUnifiedAddr(addr: String): Boolean

    suspend fun getCurrentAddress(account: Int = 0): String

    fun getTransparentReceiver(ua: String): String?

    fun getSaplingReceiver(ua: String): String?

    suspend fun getBalance(account: Int = 0): Arrrtoshi

    fun getBranchIdForHeight(height: BlockHeight): Long

    suspend fun getReceivedMemoAsUtf8(idNote: Long): String?

    suspend fun getSentMemoAsUtf8(idNote: Long): String?

    suspend fun getVerifiedBalance(account: Int = 0): Arrrtoshi

//    fun parseTransactionDataList(tdl: LocalRpcTypes.TransactionDataList): LocalRpcTypes.TransparentTransactionList

    suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight

    suspend fun rewindToHeight(height: BlockHeight): Boolean

    suspend fun scanBlocks(limit: Int = -1): Boolean

    /**
     * @return Null if successful. If an error occurs, the height will be the height where the error was detected.
     */
    suspend fun validateCombinedChain(): BlockHeight?

    @Suppress("LongParameterList")
    suspend fun putUtxo(
        tAddress: String,
        txId: ByteArray,
        index: Int,
        script: ByteArray,
        value: Long,
        height: BlockHeight
    ): Boolean

    suspend fun getDownloadedUtxoBalance(address: String): PirateWalletBalance

    // Implemented by `PirateDerivationTool`
    interface Derivation {
        suspend fun derivePirateUnifiedAddress(
            viewingKey: String,
            network: PirateNetwork
        ): String

        suspend fun derivePirateUnifiedAddress(
            seed: ByteArray,
            network: PirateNetwork,
            account: Account
        ): String

        suspend fun derivePirateUnifiedSpendingKey(
            seed: ByteArray,
            network: PirateNetwork,
            account: Account
        ): PirateUnifiedSpendingKey

        suspend fun derivePirateUnifiedFullViewingKey(
            usk: PirateUnifiedSpendingKey,
            network: PirateNetwork
        ): PirateUnifiedFullViewingKey

        suspend fun derivePirateUnifiedFullViewingKeys(
            seed: ByteArray,
            network: PirateNetwork,
            numberOfAccounts: Int = 1
        ): Array<PirateUnifiedFullViewingKey>
    }
}
