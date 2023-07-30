package pirate.android.sdk

import android.content.Context
import pirate.android.sdk.PirateSynchronizer.PirateStatus.DISCONNECTED
import pirate.android.sdk.PirateSynchronizer.PirateStatus.DOWNLOADING
import pirate.android.sdk.PirateSynchronizer.PirateStatus.ENHANCING
import pirate.android.sdk.PirateSynchronizer.PirateStatus.SCANNING
import pirate.android.sdk.PirateSynchronizer.PirateStatus.STOPPED
import pirate.android.sdk.PirateSynchronizer.PirateStatus.SYNCED
import pirate.android.sdk.PirateSynchronizer.PirateStatus.VALIDATING
import pirate.android.sdk.block.PirateCompactBlockProcessor
import pirate.android.sdk.block.PirateCompactBlockProcessor.PirateState.Disconnected
import pirate.android.sdk.block.PirateCompactBlockProcessor.PirateState.Downloading
import pirate.android.sdk.block.PirateCompactBlockProcessor.PirateState.Enhancing
import pirate.android.sdk.block.PirateCompactBlockProcessor.PirateState.Initialized
import pirate.android.sdk.block.PirateCompactBlockProcessor.PirateState.Scanned
import pirate.android.sdk.block.PirateCompactBlockProcessor.PirateState.Scanning
import pirate.android.sdk.block.PirateCompactBlockProcessor.PirateState.Stopped
import pirate.android.sdk.block.PirateCompactBlockProcessor.PirateState.Validating
import pirate.android.sdk.ext.PirateConsensusBranchId
import pirate.android.sdk.ext.PirateSdk
import pirate.android.sdk.internal.PirateSaplingParamTool
import pirate.android.sdk.internal.block.PirateCompactBlockDownloader
import pirate.android.sdk.internal.db.DatabaseCoordinator
import pirate.android.sdk.internal.db.block.DbPirateCompactBlockRepository
import pirate.android.sdk.internal.db.derived.DbDerivedDataRepository
import pirate.android.sdk.internal.db.derived.DerivedDataDb
import pirate.android.sdk.internal.ext.toHexReversed
import pirate.android.sdk.internal.ext.tryNull
import pirate.android.sdk.internal.isEmpty
import pirate.android.sdk.internal.model.Checkpoint
import pirate.android.sdk.internal.repository.PirateCompactBlockRepository
import pirate.android.sdk.internal.repository.DerivedDataRepository
import pirate.android.sdk.internal.service.PirateLightWalletGrpcService
import pirate.android.sdk.internal.service.LightWalletService
import pirate.android.sdk.internal.transaction.OutboundTransactionManager
import pirate.android.sdk.internal.transaction.PiratePersistentTransactionManager
import pirate.android.sdk.internal.transaction.TransactionEncoder
import pirate.android.sdk.internal.transaction.PirateWalletTransactionEncoder
import pirate.android.sdk.internal.twig
import pirate.android.sdk.internal.twigTask
import pirate.android.sdk.jni.PirateRustBackend
import pirate.android.sdk.model.Account
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.LightWalletEndpoint
import pirate.android.sdk.model.PendingTransaction
import pirate.android.sdk.model.TransactionOverview
import pirate.android.sdk.model.TransactionRecipient
import pirate.android.sdk.model.PirateUnifiedSpendingKey
import pirate.android.sdk.model.PirateWalletBalance
import pirate.android.sdk.model.Arrrtoshi
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.model.isExpired
import pirate.android.sdk.model.isLongExpired
import pirate.android.sdk.model.isMarkedForDeletion
import pirate.android.sdk.model.isMined
import pirate.android.sdk.model.isSafeToDiscard
import pirate.android.sdk.model.isSubmitSuccess
import pirate.android.sdk.type.PirateAddressType
import pirate.android.sdk.type.PirateAddressType.Shielded
import pirate.android.sdk.type.PirateAddressType.Transparent
import pirate.android.sdk.type.PirateAddressType.Unified
import pirate.android.sdk.type.PirateConsensusMatchType
import pirate.android.sdk.type.PirateUnifiedFullViewingKey
import pirate.wallet.sdk.rpc.Service
import io.grpc.ManagedChannel
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.CoroutineContext

/**
 * A PirateSynchronizer that attempts to remain operational, despite any number of errors that can occur.
 * It acts as the glue that ties all the pieces of the SDK together. Each component of the SDK is
 * designed for the potential of stand-alone usage but coordinating all the interactions is non-
 * trivial. So the PirateSynchronizer facilitates this, acting as reference that demonstrates how all the
 * pieces can be tied together. Its goal is to allow a developer to focus on their app rather than
 * the nuances of how Zcash works.
 *
 * @property synchronizerKey Identifies the synchronizer's on-disk state
 * @property storage exposes flows of wallet transaction information.
 * @property txManager manages and tracks outbound transactions.
 * @property processor saves the downloaded compact blocks to the cache and then scans those blocks for
 * data related to this wallet.
 */
@Suppress("TooManyFunctions")
class PirateSdkSynchronizer private constructor(
    private val synchronizerKey: PirateSynchronizerKey,
    private val storage: DerivedDataRepository,
    private val txManager: OutboundTransactionManager,
    val processor: PirateCompactBlockProcessor,
    private val rustBackend: PirateRustBackend
) : CloseablePirateSynchronizer {

    companion object {
        private sealed class InstanceState {
            object Active : InstanceState()
            data class ShuttingDown(val job: Job) : InstanceState()
        }

        private val instances: MutableMap<PirateSynchronizerKey, InstanceState> =
            ConcurrentHashMap<PirateSynchronizerKey, InstanceState>()

        /**
         * @throws IllegalStateException If multiple instances of synchronizer with the same network+alias are
         * active at the same time.  Call `close` to finish one synchronizer before starting another one with the same
         * network+alias.
         */
        @Suppress("LongParameterList")
        internal suspend fun new(
            zcashNetwork: PirateNetwork,
            alias: String,
            repository: DerivedDataRepository,
            txManager: OutboundTransactionManager,
            processor: PirateCompactBlockProcessor,
            rustBackend: PirateRustBackend
        ): CloseablePirateSynchronizer {
            val synchronizerKey = PirateSynchronizerKey(zcashNetwork, alias)

            waitForShutdown(synchronizerKey)
            checkForExistingPirateSynchronizers(synchronizerKey)

            return PirateSdkSynchronizer(
                synchronizerKey,
                repository,
                txManager,
                processor,
                rustBackend
            ).apply {
                instances[synchronizerKey] = InstanceState.Active

                start()
            }
        }

        private suspend fun waitForShutdown(synchronizerKey: PirateSynchronizerKey) {
            instances[synchronizerKey]?.let {
                if (it is InstanceState.ShuttingDown) {
                    twig("Waiting for prior synchronizer instance to shut down") // $NON-NLS-1$
                    it.job.join()
                }
            }
        }

        private fun checkForExistingPirateSynchronizers(synchronizerKey: PirateSynchronizerKey) {
            check(!instances.containsKey(synchronizerKey)) {
                "Another synchronizer with $synchronizerKey is currently active" // $NON-NLS-1$
            }
        }

        internal suspend fun erase(
            appContext: Context,
            network: PirateNetwork,
            alias: String
        ): Boolean {
            val key = PirateSynchronizerKey(network, alias)

            waitForShutdown(key)
            checkForExistingPirateSynchronizers(key)

            return DatabaseCoordinator.getInstance(appContext).deleteDatabases(network, alias)
        }
    }

    // pools
    private val _orchardBalances = MutableStateFlow<PirateWalletBalance?>(null)
    private val _saplingBalances = MutableStateFlow<PirateWalletBalance?>(null)
    private val _transparentBalances = MutableStateFlow<PirateWalletBalance?>(null)

    private val _status = MutableStateFlow<PirateSynchronizer.PirateStatus>(DISCONNECTED)

    var coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    /**
     * The channel that this PirateSynchronizer uses to communicate with lightwalletd. In most cases, this
     * should not be needed or used. Instead, APIs should be added to the synchronizer to
     * enable the desired behavior. In the rare case, such as testing, it can be helpful to share
     * the underlying channel to connect to the same service, and use other APIs
     * (such as darksidewalletd) because channels are heavyweight.
     */
    val channel: ManagedChannel get() = (processor.downloader.lightWalletService as PirateLightWalletGrpcService).channel

    //
    // Balances
    //

    override val orchardBalances = _orchardBalances.asStateFlow()
    override val saplingBalances = _saplingBalances.asStateFlow()
    override val transparentBalances = _transparentBalances.asStateFlow()

    //
    // Transactions
    //

    override val clearedTransactions get() = storage.allTransactions
    override val pendingTransactions = txManager.getAll()
    override val sentTransactions get() = storage.sentTransactions
    override val receivedTransactions get() = storage.receivedTransactions

    //
    // PirateStatus
    //

    override val network: PirateNetwork get() = processor.network

    /**
     * Indicates the status of this PirateSynchronizer. This implementation basically simplifies the
     * status of the processor to focus only on the high level states that matter most. Whenever the
     * processor is finished scanning, the synchronizer updates transaction and balance info and
     * then emits a [SYNCED] status.
     */
    override val status = _status.asStateFlow()

    /**
     * Indicates the download progress of the PirateSynchronizer. When progress reaches 100, that
     * signals that the PirateSynchronizer is in sync with the network. Balances should be considered
     * inaccurate and outbound transactions should be prevented until this sync is complete. It is
     * a simplified version of [processorInfo].
     */
    override val progress: Flow<Int> = processor.progress

    /**
     * Indicates the latest information about the blocks that have been processed by the SDK. This
     * is very helpful for conveying detailed progress and status to the user.
     */
    override val processorInfo: Flow<PirateCompactBlockProcessor.ProcessorInfo> = processor.processorInfo

    /**
     * The latest height seen on the network while processing blocks. This may differ from the
     * latest height scanned and is useful for determining block confirmations and expiration.
     */
    override val networkHeight: StateFlow<BlockHeight?> = processor.networkHeight

    //
    // Error Handling
    //

    /**
     * A callback to invoke whenever an uncaught error is encountered. By definition, the return
     * value of the function is ignored because this error is unrecoverable. The only reason the
     * function has a return value is so that all error handlers work with the same signature which
     * allows one function to handle all errors in simple apps. This callback is not called on the
     * main thread so any UI work would need to switch context to the main thread.
     */
    override var onCriticalErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a processor error is encountered. Returning true signals that
     * the error was handled and a retry attempt should be made, if possible. This callback is not
     * called on the main thread so any UI work would need to switch context to the main thread.
     */
    override var onProcessorErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a server error is encountered while submitting a transaction to
     * lightwalletd. Returning true signals that the error was handled and a retry attempt should be
     * made, if possible. This callback is not called on the main thread so any UI work would need
     * to switch context to the main thread.
     */
    override var onSubmissionErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a processor is not setup correctly. Returning true signals that
     * the invalid setup should be ignored. If no handler is set, then any setup error will result
     * in a critical error. This callback is not called on the main thread so any UI work would need
     * to switch context to the main thread.
     */
    override var onSetupErrorHandler: ((Throwable?) -> Boolean)? = null

    /**
     * A callback to invoke whenever a chain error is encountered. These occur whenever the
     * processor detects a missing or non-chain-sequential block (i.e. a reorg).
     */
    override var onChainErrorHandler: ((errorHeight: BlockHeight, rewindHeight: BlockHeight) -> Any)? = null

    //
    // Public API
    //

    /**
     * Convenience function for the latest height. Specifically, this value represents the last
     * height that the synchronizer has observed from the lightwalletd server. Instead of using
     * this, a wallet will more likely want to consume the flow of processor info using
     * [processorInfo].
     */
    override val latestHeight
        get() = processor.currentInfo.networkBlockHeight

    override val latestBirthdayHeight
        get() = processor.birthdayHeight

    internal fun start() {
        coroutineScope.onReady()
    }

    override fun close() {
        // Note that stopping will continue asynchronously.  Race conditions with starting a new synchronizer are
        // avoided with a delay during startup.

        val shutdownJob = coroutineScope.launch {
            // log everything to help troubleshoot shutdowns that aren't graceful
            twig("PirateSynchronizer::stop: STARTING")
            twig("PirateSynchronizer::stop: processor.stop()")
            processor.stop()
        }

        instances[synchronizerKey] = InstanceState.ShuttingDown(shutdownJob)

        shutdownJob.invokeOnCompletion {
            twig("PirateSynchronizer::stop: coroutineScope.cancel()")
            coroutineScope.cancel()
            twig("PirateSynchronizer::stop: _status.cancel()")
            _status.value = STOPPED
            twig("PirateSynchronizer::stop: COMPLETE")

            instances.remove(synchronizerKey)
        }
    }

    /**
     * Convenience function that exposes the underlying server information, like its name and
     * consensus branch id. Most wallets should already have a different source of truth for the
     * server(s) with which they operate.
     */
    override suspend fun getServerInfo(): Service.LightdInfo = processor.downloader.getServerInfo()

    override suspend fun getNearestRewindHeight(height: BlockHeight): BlockHeight =
        processor.getNearestRewindHeight(height)

    override suspend fun rewindToNearestHeight(height: BlockHeight, alsoClearBlockCache: Boolean) {
        processor.rewindToNearestHeight(height, alsoClearBlockCache)
    }

    override suspend fun quickRewind() {
        processor.quickRewind()
    }

    override fun getMemos(transactionOverview: TransactionOverview): Flow<String> {
        return when (transactionOverview.isSentTransaction) {
            true -> {
                val sentNoteIds = storage.getSentNoteIds(transactionOverview.id)

                sentNoteIds.map { rustBackend.getSentMemoAsUtf8(it) }.filterNotNull()
            }
            false -> {
                val receivedNoteIds = storage.getReceivedNoteIds(transactionOverview.id)

                receivedNoteIds.map { rustBackend.getReceivedMemoAsUtf8(it) }.filterNotNull()
            }
        }
    }

    override fun getRecipients(transactionOverview: TransactionOverview): Flow<TransactionRecipient> {
        require(transactionOverview.isSentTransaction) { "Recipients can only be queried for sent transactions" }

        return storage.getRecipients(transactionOverview.id)
    }

    //
    // Storage APIs
    //

    // TODO [#682]: turn this section into the data access API. For now, just aggregate all the things that we want
    //  to do with the underlying data
    // TODO [#682]: https://github.com/zcash/zcash-android-wallet-sdk/issues/682

    suspend fun findBlockHash(height: BlockHeight): ByteArray? {
        return storage.findBlockHash(height)
    }

    suspend fun findBlockHashAsHex(height: BlockHeight): String? {
        return findBlockHash(height)?.toHexReversed()
    }

    suspend fun getTransactionCount(): Int {
        return storage.getTransactionCount().toInt()
    }

    fun refreshTransactions() {
        storage.invalidate()
    }

    //
    // Private API
    //

    suspend fun refreshUtxos() {
        twig("refreshing utxos", -1)
        refreshUtxos(getTransparentAddress())
    }

    /**
     * Calculate the latest balance, based on the blocks that have been scanned and transmit this
     * information into the flow of [balances].
     */
    suspend fun refreshAllBalances() {
        refreshSaplingBalance()
        refreshTransparentBalance()
        // TODO [#682]: refresh orchard balance
        // TODO [#682]: https://github.com/zcash/zcash-android-wallet-sdk/issues/682
        twig("Warning: Orchard balance does not yet refresh. Only some of the plumbing is in place.")
    }

    suspend fun refreshSaplingBalance() {
        twig("refreshing sapling balance")
        _saplingBalances.value = processor.getBalanceInfo(Account.DEFAULT)
    }

    suspend fun refreshTransparentBalance() {
        twig("refreshing transparent balance")
        _transparentBalances.value = processor.getUtxoCacheBalance(getTransparentAddress())
    }

    suspend fun isValidAddress(address: String): Boolean {
        return !validateAddress(address).isNotValid
    }

    private fun CoroutineScope.onReady() = launch(CoroutineExceptionHandler(::onCriticalError)) {
        twig("Preparing to start...")

        twig("PirateSynchronizer (${this@PirateSdkSynchronizer}) Ready. Starting processor!")
        var lastScanTime = 0L
        processor.onProcessorErrorListener = ::onProcessorError
        processor.onSetupErrorListener = ::onSetupError
        processor.onChainErrorListener = ::onChainError
        processor.state.onEach {
            when (it) {
                is Scanned -> {
                    val now = System.currentTimeMillis()
                    // do a bit of housekeeping and then report synced status
                    onScanComplete(it.scannedRange, now - lastScanTime)
                    lastScanTime = now
                    SYNCED
                }
                is Stopped -> STOPPED
                is Disconnected -> DISCONNECTED
                is Downloading, Initialized -> DOWNLOADING
                is Validating -> VALIDATING
                is Scanning -> SCANNING
                is Enhancing -> ENHANCING
            }.let { synchronizerStatus ->
                //  ignore enhancing status for now
                // TODO [#682]: clean this up and handle enhancing gracefully
                // TODO [#682]: https://github.com/zcash/zcash-android-wallet-sdk/issues/682
                if (synchronizerStatus != ENHANCING) _status.value = synchronizerStatus
            }
        }.launchIn(this)
        processor.start()
        twig("PirateSynchronizer onReady complete. Processor start has exited!")
    }

    @Suppress("UNUSED_PARAMETER")
    private fun onCriticalError(unused: CoroutineContext?, error: Throwable) {
        twig("********")
        twig("********  ERROR: $error")
        twig(error)
        if (error.cause != null) twig("******** caused by ${error.cause}")
        if (error.cause?.cause != null) twig("******** caused by ${error.cause?.cause}")
        twig("********")

        if (onCriticalErrorHandler == null) {
            twig(
                "WARNING: a critical error occurred but no callback is registered to be notified " +
                    "of critical errors! THIS IS PROBABLY A MISTAKE. To respond to these " +
                    "errors (perhaps to update the UI or alert the user) set " +
                    "synchronizer.onCriticalErrorHandler to a non-null value."
            )
        }

        onCriticalErrorHandler?.invoke(error)
    }

    private fun onProcessorError(error: Throwable): Boolean {
        twig("ERROR while processing data: $error")
        if (onProcessorErrorHandler == null) {
            twig(
                "WARNING: falling back to the default behavior for processor errors. To add" +
                    " custom behavior, set synchronizer.onProcessorErrorHandler to" +
                    " a non-null value"
            )
            return true
        }
        return onProcessorErrorHandler?.invoke(error)?.also {
            twig(
                "processor error handler signaled that we should " +
                    "${if (it) "try again" else "abort"}!"
            )
        } == true
    }

    private fun onSetupError(error: Throwable): Boolean {
        if (onSetupErrorHandler == null) {
            twig(
                "WARNING: falling back to the default behavior for setup errors. To add custom" +
                    " behavior, set synchronizer.onSetupErrorHandler to a non-null value"
            )
            return false
        }
        return onSetupErrorHandler?.invoke(error) == true
    }

    private fun onChainError(errorHeight: BlockHeight, rewindHeight: BlockHeight) {
        twig("Chain error detected at height: $errorHeight. Rewinding to: $rewindHeight")
        if (onChainErrorHandler == null) {
            twig(
                "WARNING: a chain error occurred but no callback is registered to be notified of " +
                    "chain errors. To respond to these errors (perhaps to update the UI or alert the" +
                    " user) set synchronizer.onChainErrorHandler to a non-null value"
            )
        }
        onChainErrorHandler?.invoke(errorHeight, rewindHeight)
    }

    /**
     * @param elapsedMillis the amount of time that passed since the last scan
     */
    private suspend fun onScanComplete(scannedRange: ClosedRange<BlockHeight>?, elapsedMillis: Long) {
        // We don't need to update anything if there have been no blocks
        // refresh anyway if:
        // - if it's the first time we finished scanning
        // - if we check for blocks 5 times and find nothing was mined
        @Suppress("MagicNumber")
        val shouldRefresh = !scannedRange.isEmpty() || elapsedMillis > (PirateSdk.POLL_INTERVAL * 5)
        val reason = if (scannedRange.isEmpty()) "it's been a while" else "new blocks were scanned"

        // TRICKY:
        // Keep an eye on this section because there is a potential for concurrent DB
        // modification. A change in transactions means a change in balance. Calculating the
        // balance requires touching transactions. If both are done in separate threads, the
        // database can have issues. On Android, this would manifest as a false positive for a
        // "malformed database" exception when the database is not actually corrupt but rather
        // locked (i.e. it's a bad error message).
        // The balance refresh is done first because it is coroutine-based and will fully
        // complete by the time the function returns.
        // Ultimately, refreshing the transactions just invalidates views of data that
        // already exists and it completes on another thread so it should come after the
        // balance refresh is complete.
        if (shouldRefresh) {
            twigTask("Triggering utxo refresh since $reason!", -1) {
                refreshUtxos()
            }
            twigTask("Triggering balance refresh since $reason!", -1) {
                refreshAllBalances()
            }
            twigTask("Triggering pending transaction refresh since $reason!", -1) {
                refreshPendingTransactions()
            }
            twigTask("Triggering transaction refresh since $reason!") {
                refreshTransactions()
            }
        }
    }

    @Suppress("LongMethod", "ComplexMethod")
    private suspend fun refreshPendingTransactions() {
        twig("[cleanup] beginning to refresh and clean up pending transactions")
        // TODO [#682]: this would be the place to clear out any stale pending transactions. Remove filter logic and
        //  then delete any pending transaction with sufficient confirmations (all in one db transaction).
        // TODO [#682]: https://github.com/zcash/zcash-android-wallet-sdk/issues/682
        val allPendingTxs = txManager.getAll().first()
        val lastScannedHeight = storage.lastScannedHeight()

        allPendingTxs.filter { it.isSubmitSuccess() && !it.isMined() }
            .forEach { pendingTx ->
                twig("checking for updates on pendingTx id: ${pendingTx.id}")
                pendingTx.rawTransactionId?.let { rawId ->
                    storage.findMinedHeight(rawId.byteArray)?.let { minedHeight ->
                        twig(
                            "found matching transaction for pending transaction with id" +
                                " ${pendingTx.id} mined at height $minedHeight!"
                        )
                        txManager.applyMinedHeight(pendingTx, minedHeight)
                    }
                }
            }

        twig("[cleanup] beginning to cleanup expired transactions", -1)
        // Experimental: cleanup expired transactions
        // note: don't delete the pendingTx until the related data has been scrubbed, or else you
        // lose the thing that identifies the other data as invalid
        // so we first mark the data for deletion, during the previous "cleanup" step, by removing
        // the thing that we're trying to preserve to signal we no longer need it
        // sometimes apps crash or things go wrong and we get an orphaned pendingTx that we'll poll
        // forever, so maybe just get rid of all of them after a long while
        allPendingTxs.filter {
            (
                it.isExpired(
                    lastScannedHeight,
                    network.saplingActivationHeight
                ) && it.isMarkedForDeletion()
                ) ||
                it.isLongExpired(
                    lastScannedHeight,
                    network.saplingActivationHeight
                ) || it.isSafeToDiscard()
        }.forEach {
            val result = txManager.abort(it)
            twig(
                "[cleanup] FOUND EXPIRED pendingTX (lastScanHeight: $lastScannedHeight " +
                    " expiryHeight: ${it.expiryHeight}): and ${it.id} " +
                    "${if (result > 0) "successfully removed" else "failed to remove"} it"
            )
        }

        twig("[cleanup] done refreshing and cleaning up pending transactions", -1)
    }

    //
    // Account management
    //

    // Not ready to be a public API; internal for testing only
    internal suspend fun createAccount(seed: ByteArray): PirateUnifiedSpendingKey =
        processor.createAccount(seed)

    /**
     * Returns the current Unified Address for this account.
     */
    override suspend fun getUnifiedAddress(account: Account): String =
        processor.getCurrentAddress(account)

    /**
     * Returns the legacy Sapling address corresponding to the current Unified Address for this account.
     */
    override suspend fun getSaplingAddress(account: Account): String =
        processor.getLegacySaplingAddress(account)

    /**
     * Returns the legacy transparent address corresponding to the current Unified Address for this account.
     */
    override suspend fun getTransparentAddress(account: Account): String =
        processor.getTransparentAddress(account)

    override fun sendToAddress(
        usk: PirateUnifiedSpendingKey,
        amount: Arrrtoshi,
        toAddress: String,
        memo: String
    ): Flow<PendingTransaction> {
        // Using a job to ensure that even if the flow is collected multiple times, the transaction is only submitted
        // once
        val deferred = coroutineScope.async {
            // Emit the placeholder transaction, then switch to monitoring the database
            val placeHolderTx = txManager.initSpend(amount, TransactionRecipient.Address(toAddress), memo, usk.account)

            txManager.encode(usk, placeHolderTx).let { encodedTx ->
                txManager.submit(encodedTx)
            }

            placeHolderTx.id
        }

        return flow<PendingTransaction> {
            val placeHolderTxId = deferred.await()
            emitAll(txManager.monitorById(placeHolderTxId))
        }
    }

    override fun shieldFunds(
        usk: PirateUnifiedSpendingKey,
        memo: String
    ): Flow<PendingTransaction> {
        twig("Initializing shielding transaction")
        val deferred = coroutineScope.async {
            val tAddr = processor.getTransparentAddress(usk.account)
            val tBalance = processor.getUtxoCacheBalance(tAddr)

            // Emit the placeholder transaction, then switch to monitoring the database
            val placeHolderTx = txManager.initSpend(
                tBalance.available,
                TransactionRecipient.Account(usk.account),
                memo,
                usk.account
            )
            val encodedTx = txManager.encode("", usk, placeHolderTx)
            txManager.submit(encodedTx)

            placeHolderTx.id
        }

        return flow<PendingTransaction> {
            val placeHolderTxId = deferred.await()
            emitAll(txManager.monitorById(placeHolderTxId))
        }
    }

    override suspend fun refreshUtxos(tAddr: String, since: BlockHeight): Int? {
        return processor.refreshUtxos(tAddr, since)
    }

    override suspend fun getTransparentBalance(tAddr: String): PirateWalletBalance {
        return processor.getUtxoCacheBalance(tAddr)
    }

    override suspend fun isValidShieldedAddr(address: String) =
        txManager.isValidShieldedAddress(address)

    override suspend fun isValidTransparentAddr(address: String) =
        txManager.isValidTransparentAddress(address)

    override suspend fun isValidUnifiedAddr(address: String) =
        txManager.isValidUnifiedAddress(address)

    override suspend fun validateAddress(address: String): PirateAddressType {
        @Suppress("TooGenericExceptionCaught")
        return try {
            if (isValidShieldedAddr(address)) {
                Shielded
            } else if (isValidTransparentAddr(address)) {
                Transparent
            } else if (isValidUnifiedAddr(address)) {
                Unified
            } else {
                PirateAddressType.Invalid("Not a Zcash address")
            }
        } catch (@Suppress("TooGenericExceptionCaught") error: Throwable) {
            PirateAddressType.Invalid(error.message ?: "Invalid")
        }
    }

    override suspend fun validateConsensusBranch(): PirateConsensusMatchType {
        val serverBranchId = tryNull { processor.downloader.getServerInfo().consensusBranchId }
        val sdkBranchId = tryNull {
            (txManager as PiratePersistentTransactionManager).encoder.getConsensusBranchId()
        }
        return PirateConsensusMatchType(
            sdkBranchId?.let { PirateConsensusBranchId.fromId(it) },
            serverBranchId?.let { PirateConsensusBranchId.fromHex(it) }
        )
    }
}

/**
 * Provides a way of constructing a synchronizer where dependencies are injected in.
 *
 * See the helper methods for generating default values.
 */
internal object DefaultPirateSynchronizerFactory {

    internal suspend fun defaultPirateRustBackend(
        context: Context,
        network: PirateNetwork,
        alias: String,
        blockHeight: BlockHeight,
        saplingParamTool: PirateSaplingParamTool
    ): PirateRustBackend {
        val coordinator = DatabaseCoordinator.getInstance(context)

        return PirateRustBackend.init(
            coordinator.cacheDbFile(network, alias),
            coordinator.dataDbFile(network, alias),
            saplingParamTool.properties.paramsDirectory,
            network,
            blockHeight
        )
    }

    @Suppress("LongParameterList")
    internal suspend fun defaultDerivedDataRepository(
        context: Context,
        rustBackend: PirateRustBackend,
        zcashNetwork: PirateNetwork,
        checkpoint: Checkpoint,
        seed: ByteArray?,
        viewingKeys: List<PirateUnifiedFullViewingKey>
    ): DerivedDataRepository =
        DbDerivedDataRepository(DerivedDataDb.new(context, rustBackend, zcashNetwork, checkpoint, seed, viewingKeys))

    internal fun defaultPirateCompactBlockRepository(context: Context, cacheDbFile: File, zcashNetwork: PirateNetwork):
        PirateCompactBlockRepository =
        DbPirateCompactBlockRepository.new(
            context,
            zcashNetwork,
            cacheDbFile
        )

    fun defaultService(context: Context, lightWalletEndpoint: LightWalletEndpoint): LightWalletService =
        PirateLightWalletGrpcService.new(context, lightWalletEndpoint)

    internal fun defaultEncoder(
        rustBackend: PirateRustBackend,
        saplingParamTool: PirateSaplingParamTool,
        repository: DerivedDataRepository
    ): TransactionEncoder = PirateWalletTransactionEncoder(rustBackend, saplingParamTool, repository)

    fun defaultDownloader(
        service: LightWalletService,
        blockStore: PirateCompactBlockRepository
    ): PirateCompactBlockDownloader = PirateCompactBlockDownloader(service, blockStore)

    internal suspend fun defaultTxManager(
        context: Context,
        zcashNetwork: PirateNetwork,
        alias: String,
        encoder: TransactionEncoder,
        service: LightWalletService
    ): OutboundTransactionManager {
        val databaseFile = DatabaseCoordinator.getInstance(context).pendingTransactionsDbFile(
            zcashNetwork,
            alias
        )

        return PiratePersistentTransactionManager.new(
            context,
            zcashNetwork,
            encoder,
            service,
            databaseFile
        )
    }

    internal fun defaultProcessor(
        rustBackend: PirateRustBackend,
        downloader: PirateCompactBlockDownloader,
        repository: DerivedDataRepository
    ): PirateCompactBlockProcessor = PirateCompactBlockProcessor(
        downloader,
        repository,
        rustBackend,
        rustBackend.birthdayHeight
    )
}

internal data class PirateSynchronizerKey(val zcashNetwork: PirateNetwork, val alias: String)
