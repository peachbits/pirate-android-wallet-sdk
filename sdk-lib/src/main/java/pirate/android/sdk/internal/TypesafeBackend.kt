package pirate.android.sdk.internal

import pirate.android.sdk.internal.model.Checkpoint
import pirate.android.sdk.internal.model.JniBlockMeta
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.UnifiedFullViewingKey
import pirate.android.sdk.model.WalletBalance
import pirate.android.sdk.model.Arrrtoshi

@Suppress("TooManyFunctions")
internal interface TypesafeBackend {

    suspend fun initAccountsTable(vararg keys: UnifiedFullViewingKey)

    suspend fun initAccountsTable(
        seed: ByteArray,
        numberOfAccounts: Int
    ): List<UnifiedFullViewingKey>

    suspend fun initBlocksTable(checkpoint: Checkpoint)

    suspend fun getCurrentAddress(account: Account): String

    suspend fun listTransparentReceivers(account: Account): List<String>

    suspend fun getBalance(account: Account): Arrrtoshi

    fun getBranchIdForHeight(height: BlockHeight): Long

    suspend fun getVerifiedBalance(account: Account): Arrrtoshi

    suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight

    suspend fun rewindToHeight(height: BlockHeight)

    suspend fun getLatestBlockHeight(): BlockHeight?

    suspend fun findBlockMetadata(height: BlockHeight): JniBlockMeta?

    suspend fun rewindBlockMetadataToHeight(height: BlockHeight)

    /**
     * @param limit The limit provides an efficient way how to restrict the portion of blocks, which will be validated.
     * @return Null if successful. If an error occurs, the height will be the height where the error was detected.
     */
    suspend fun validateCombinedChainOrErrorBlockHeight(limit: Long?): BlockHeight?

    suspend fun getDownloadedUtxoBalance(address: String): WalletBalance
}
