package pirate.android.sdk

import android.content.Context
import pirate.android.sdk.db.DatabaseCoordinator
import pirate.android.sdk.exception.PirateInitializerException
import pirate.android.sdk.ext.PirateSdk
import pirate.android.sdk.internal.PirateSaplingParamTool
import pirate.android.sdk.internal.model.Checkpoint
import pirate.android.sdk.internal.twig
import pirate.android.sdk.jni.PirateRustBackend
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.LightWalletEndpoint
import pirate.android.sdk.model.PirateNetwork
import pirate.android.sdk.tool.CheckpointTool
import pirate.android.sdk.tool.PirateDerivationTool
import pirate.android.sdk.type.PirateUnifiedViewingKey
import kotlinx.coroutines.runBlocking

/**
 * Simplified PirateInitializer focused on starting from a ViewingKey.
 */
@Suppress("LongParameterList", "unused")
class PirateInitializer private constructor(
    val context: Context,
    internal val rustBackend: PirateRustBackend,
    val network: PirateNetwork,
    val alias: String,
    val lightWalletEndpoint: LightWalletEndpoint,
    val viewingKeys: List<PirateUnifiedViewingKey>,
    val overwriteVks: Boolean,
    internal val checkpoint: Checkpoint,
    internal val saplingParamTool: PirateSaplingParamTool
) {

    suspend fun erase() = erase(context, network, alias)

    @Suppress("TooManyFunctions")
    class PirateConfig private constructor(
        val viewingKeys: MutableList<PirateUnifiedViewingKey> = mutableListOf(),
        var alias: String = PirateSdk.DEFAULT_ALIAS
    ) {
        var birthdayHeight: BlockHeight? = null
            private set

        lateinit var network: PirateNetwork
            private set

        lateinit var lightWalletEndpoint: LightWalletEndpoint
            private set

        /**
         * Determines the default behavior for null birthdays. When null, nothing has been specified
         * so a null birthdayHeight value is an error. When false, null birthdays will be replaced
         * with the most recent checkpoint height available (typically, the latest `*.json` file in
         * `assets/piratesaplingtree/`). When true, null birthdays will be replaced with the oldest
         * reasonable height where a transaction could exist (typically, sapling activation but
         * better approximations could be devised in the future, such as the date when the first
         * BIP-39 zcash wallets came online).
         */
        var defaultToOldestHeight: Boolean? = null
            private set

        var overwriteVks: Boolean = false
            private set

        constructor(block: (PirateConfig) -> Unit) : this() {
            block(this)
        }

        //
        // Birthday functions
        //

        /**
         * Set the birthday height for this configuration. When the height is not known, the wallet
         * can either default to the latest known birthday (in order to sync new wallets faster) or
         * the oldest possible birthday (in order to import a wallet with an unknown birthday
         * without skipping old transactions).
         *
         * @param height nullable birthday height to use for this configuration.
         * @param defaultToOldestHeight determines how a null birthday height will be
         * interpreted. Typically, `false` for new wallets and `true` for restored wallets because
         * new wallets want to load quickly but restored wallets want to find all possible
         * transactions. Again, this value is only considered when [height] is null.
         *
         */
        fun setBirthdayHeight(height: BlockHeight?, defaultToOldestHeight: Boolean): PirateConfig =
            apply {
                this.birthdayHeight = height
                this.defaultToOldestHeight = defaultToOldestHeight
            }

        /**
         * Load the most recent checkpoint available. This is useful for new wallets.
         */
        fun newWalletBirthday(): PirateConfig = apply {
            birthdayHeight = null
            defaultToOldestHeight = false
        }

        /**
         * Load the birthday checkpoint closest to the given wallet birthday. This is useful when
         * importing a pre-existing wallet. It is the same as calling
         * `birthdayHeight = importedHeight`.
         */
        fun importedWalletBirthday(importedHeight: BlockHeight?): PirateConfig = apply {
            birthdayHeight = importedHeight
            defaultToOldestHeight = true
        }

        //
        // Viewing key functions
        //

        /**
         * Add viewing keys to the set of accounts to monitor. Note: Using more than one viewing key
         * is not currently well supported. Consider it an alpha-preview feature that might work but
         * probably has serious bugs.
         */
        fun setViewingKeys(
            vararg unifiedViewingKeys: PirateUnifiedViewingKey,
            overwrite: Boolean = false
        ): PirateConfig = apply {
            overwriteVks = overwrite
            viewingKeys.apply {
                clear()
                addAll(unifiedViewingKeys)
            }
        }

        fun setOverwriteKeys(isOverwrite: Boolean) {
            overwriteVks = isOverwrite
        }

        /**
         * Add viewing key to the set of accounts to monitor. Note: Using more than one viewing key
         * is not currently well supported. Consider it an alpha-preview feature that might work but
         * probably has serious bugs.
         */
        fun addViewingKey(unifiedFullViewingKey: PirateUnifiedViewingKey): PirateConfig = apply {
            viewingKeys.add(unifiedFullViewingKey)
        }

        //
        // Convenience functions
        //

        /**
         * Set the server and the network property at the same time to prevent them from getting out
         * of sync. Ultimately, this determines which host a synchronizer will use in order to
         * connect to lightwalletd.
         *
         * @param network the Zcash network to use. Either testnet or mainnet.
         * @param lightWalletEndpoint the light wallet endpoint to use.
         */
        fun setNetwork(
            network: PirateNetwork,
            lightWalletEndpoint: LightWalletEndpoint
        ): PirateConfig = apply {
            this.network = network
            this.lightWalletEndpoint = lightWalletEndpoint
        }

        /**
         * Import a wallet using the first viewing key derived from the given seed.
         */
        suspend fun importWallet(
            seed: ByteArray,
            birthday: BlockHeight?,
            network: PirateNetwork,
            lightWalletEndpoint: LightWalletEndpoint,
            alias: String = PirateSdk.DEFAULT_ALIAS
        ): PirateConfig =
            importWallet(
                PirateDerivationTool.derivePirateUnifiedViewingKeys(seed, network = network)[0],
                birthday,
                network,
                lightWalletEndpoint,
                alias
            )

        /**
         * Default function for importing a wallet.
         */
        fun importWallet(
            viewingKey: PirateUnifiedViewingKey,
            birthday: BlockHeight?,
            network: PirateNetwork,
            lightWalletEndpoint: LightWalletEndpoint,
            alias: String = PirateSdk.DEFAULT_ALIAS
        ): PirateConfig = apply {
            setViewingKeys(viewingKey)
            setNetwork(network, lightWalletEndpoint)
            importedWalletBirthday(birthday)
            this.alias = alias
        }

        /**
         * Create a new wallet using the first viewing key derived from the given seed.
         */
        suspend fun newWallet(
            seed: ByteArray,
            network: PirateNetwork,
            lightWalletEndpoint: LightWalletEndpoint,
            alias: String = PirateSdk.DEFAULT_ALIAS
        ): PirateConfig = newWallet(
            PirateDerivationTool.derivePirateUnifiedViewingKeys(seed, network)[0],
            network,
            lightWalletEndpoint,
            alias
        )

        /**
         * Default function for creating a new wallet.
         */
        fun newWallet(
            viewingKey: PirateUnifiedViewingKey,
            network: PirateNetwork,
            lightWalletEndpoint: LightWalletEndpoint,
            alias: String = PirateSdk.DEFAULT_ALIAS
        ): PirateConfig = apply {
            setViewingKeys(viewingKey)
            setNetwork(network, lightWalletEndpoint)
            newWalletBirthday()
            this.alias = alias
        }

        /**
         * Convenience method for setting thew viewingKeys from a given seed. This is the same as
         * calling `setViewingKeys` with the keys that match this seed.
         */
        suspend fun setSeed(
            seed: ByteArray,
            network: PirateNetwork,
            numberOfAccounts: Int = 1
        ): PirateConfig =
            apply {
                @Suppress("SpreadOperator")
                setViewingKeys(
                    *PirateDerivationTool.derivePirateUnifiedViewingKeys(
                        seed,
                        network,
                        numberOfAccounts
                    )
                )
            }

        /**
         * Sets the network from a network id, throwing an exception if the id is not recognized.
         *
         * @param networkId the ID of the network corresponding to the [PirateNetwork] enum.
         * Typically, it is 0 for testnet and 1 for mainnet.
         */
        fun setNetworkId(networkId: Int): PirateConfig = apply {
            network = PirateNetwork.from(networkId)
        }

        //
        // Validation helpers
        //

        fun validate(): PirateConfig = apply {
            validateAlias(alias)
            validateViewingKeys()
            validateBirthday()
        }

        private fun validateBirthday() {
            // if birthday is missing then we need to know how to interpret it
            // so defaultToOldestHeight ought to be set, in that case
            if (birthdayHeight == null && defaultToOldestHeight == null) {
                throw PirateInitializerException.PirateMissingDefaultBirthdayException
            }
            // allow either null or a value greater than the activation height
            if (
                (birthdayHeight?.value ?: network.saplingActivationHeight.value)
                < network.saplingActivationHeight.value
            ) {
                throw PirateInitializerException.PirateInvalidBirthdayHeightException(birthdayHeight, network)
            }
        }

        private fun validateViewingKeys() {
            require(viewingKeys.isNotEmpty()) {
                "Unified Viewing keys are required. Ensure that the unified viewing keys or seed" +
                    " have been set on this PirateInitializer."
            }
            viewingKeys.forEach {
                PirateDerivationTool.validatePirateUnifiedViewingKey(it)
            }
        }

        companion object
    }

    companion object : PirateSdkSynchronizer.Erasable {

        suspend fun new(appContext: Context, config: PirateConfig) = new(appContext, null, config)

        fun newBlocking(appContext: Context, config: PirateConfig) = runBlocking {
            new(
                appContext,
                null,
                config
            )
        }

        suspend fun new(
            appContext: Context,
            onCriticalErrorHandler: ((Throwable?) -> Boolean)? = null,
            block: (PirateConfig) -> Unit
        ) = new(appContext, onCriticalErrorHandler, PirateConfig(block))

        @Suppress("UNUSED_PARAMETER")
        suspend fun new(
            context: Context,
            onCriticalErrorHandler: ((Throwable?) -> Boolean)?,
            config: PirateConfig
        ): PirateInitializer {
            config.validate()

            val loadedCheckpoint = run {
                val height = config.birthdayHeight
                    ?: if (config.defaultToOldestHeight == true) {
                        config.network.saplingActivationHeight
                    } else {
                        null
                    }

                CheckpointTool.loadNearest(
                    context,
                    config.network,
                    height
                )
            }

            val saplingParamTool = PirateSaplingParamTool.new(context.applicationContext)

            val rustBackend =
                initPirateRustBackend(context, config.network, config.alias, loadedCheckpoint.height, saplingParamTool)

            return PirateInitializer(
                context.applicationContext,
                rustBackend,
                config.network,
                config.alias,
                config.lightWalletEndpoint,
                config.viewingKeys,
                config.overwriteVks,
                loadedCheckpoint,
                saplingParamTool
            )
        }

        private fun onCriticalError(onCriticalErrorHandler: ((Throwable?) -> Boolean)?, error: Throwable) {
            twig("********")
            twig("********  INITIALIZER ERROR: $error")
            if (error.cause != null) twig("******** caused by ${error.cause}")
            if (error.cause?.cause != null) twig("******** caused by ${error.cause?.cause}")
            twig("********")
            twig(error)

            if (onCriticalErrorHandler == null) {
                twig(
                    "WARNING: a critical error occurred on the PirateInitializer but no callback is " +
                        "registered to be notified of critical errors! THIS IS PROBABLY A MISTAKE. To " +
                        "respond to these errors (perhaps to update the UI or alert the user) set " +
                        "initializer.onCriticalErrorHandler to a non-null value or use the secondary " +
                        "constructor: PirateInitializer(context, handler) { ... }. Note that the synchronizer " +
                        "and initializer BOTH have error handlers and since the initializer exists " +
                        "before the synchronizer, it needs its error handler set separately."
                )
            }

            onCriticalErrorHandler?.invoke(error)
        }

        private suspend fun initPirateRustBackend(
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

        /**
         * Delete the databases associated with this wallet. This removes all compact blocks and
         * data derived from those blocks. For most wallets, this should not result in a loss of
         * funds because the seed and spending keys are stored separately. This call just removes
         * the associated data but not the seed or spending key, themselves, because those are
         * managed separately by the wallet.
         *
         * @param appContext the application context.
         * @param network the network associated with the data to be erased.
         * @param alias the alias used to create the local data.
         *
         * @return true when one of the associated files was found. False most likely indicates
         * that the wrong alias was provided.
         */
        override suspend fun erase(
            appContext: Context,
            network: PirateNetwork,
            alias: String
        ): Boolean = DatabaseCoordinator.getInstance(appContext).deleteDatabases(network, alias)
    }
}

/**
 * Validate that the alias doesn't contain malicious characters by enforcing simple rules which
 * permit the alias to be used as part of a file name for the preferences and databases. This
 * enables multiple wallets to exist on one device, which is also helpful for sweeping funds.
 *
 * @param alias the alias to validate.
 *
 * @throws IllegalArgumentException whenever the alias is not less than 100 characters or
 * contains something other than alphanumeric characters. Underscores are allowed but aliases
 * must start with a letter.
 */
internal fun validateAlias(alias: String) {
    require(
    
        alias.length in PirateSdk.ALIAS_MIN_LENGTH..PirateSdk.ALIAS_MAX_LENGTH && alias[0].isLetter() &&
            alias.all { it.isLetterOrDigit() || it == '_' }
    ) {
        "ERROR: Invalid alias ($alias). For security, the alias must be shorter than 100 " +
            "characters and only contain letters, digits or underscores and start with a letter."
    }
}
