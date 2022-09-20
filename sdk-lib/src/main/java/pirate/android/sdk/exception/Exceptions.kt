package pirate.android.sdk.exception

import pirate.android.sdk.type.PirateNetwork
import cash.z.wallet.sdk.rpc.Service
import io.grpc.Status
import io.grpc.Status.Code.UNAVAILABLE

/**
 * Marker for all custom exceptions from the SDK. Making it an interface would result in more typing
 * so it's a supertype, instead.
 */
open class PirateSdkException(message: String, cause: Throwable?) : RuntimeException(message, cause)

/**
 * Exceptions thrown in the Rust layer of the SDK. We may not always be able to surface details about this
 * exception so it's important for the SDK to provide helpful messages whenever these errors are encountered.
 */
sealed class PirateRustLayerException(message: String, cause: Throwable? = null) : PirateSdkException(message, cause) {
    class PirateBalanceException(cause: Throwable) : PirateRustLayerException(
        "Error while requesting the current balance over " +
            "JNI. This might mean that the database has been corrupted and needs to be rebuilt. Verify that " +
            "blocks are not missing or have not been scanned out of order.",
        cause
    )
}

/**
 * User-facing exceptions thrown by the transaction repository.
 */
sealed class PirateRepositoryException(message: String, cause: Throwable? = null) : PirateSdkException(message, cause) {
    object PirateFalseStart : PirateRepositoryException(
        "The channel is closed. Note that once a repository has stopped it " +
            "cannot be restarted. Verify that the repository is not being restarted."
    )
    object Unprepared : PirateRepositoryException(
        "Unprepared repository: Data cannot be accessed before the repository is prepared." +
            " Ensure that things have been properly initialized. If you see this error it most" +
            " likely means that you are accessing transactions or other data before starting the" +
            " Synchronizer. Previously, this was a silent bug that would cause problems later." +
            " Mostly, during database migrations. Now, we catch this early and explicitly prevent" +
            " it from happening."
    )
}

/**
 * High-level exceptions thrown by the synchronizer, which do not fall within the umbrella of a
 * child component.
 */
sealed class PirateSynchronizerException(message: String, cause: Throwable? = null) : PirateSdkException(message, cause) {
    object PirateFalseStart : PirateSynchronizerException(
        "This synchronizer was already started. Multiple calls to start are not" +
            "allowed and once a synchronizer has stopped it cannot be restarted."
    )
    object NotYetStarted : PirateSynchronizerException(
        "The synchronizer has not yet started. Verify that" +
            " start has been called prior to this operation and that the coroutineScope is not" +
            " being accessed before it is initialized."
    )
}

/**
 * Potentially user-facing exceptions that occur while processing compact blocks.
 */
sealed class PirateCompactBlockProcessorException(message: String, cause: Throwable? = null) : PirateSdkException(message, cause) {
    class PirateDataDbMissing(path: String) : PirateCompactBlockProcessorException(
        "No data db file found at path $path. Verify " +
            "that the data DB has been initialized via `rustBackend.initDataDb(path)`"
    )
    open class PirateConfigurationException(message: String, cause: Throwable?) : PirateCompactBlockProcessorException(message, cause)
    class PirateFileInsteadOfPath(fileName: String) : PirateConfigurationException(
        "Invalid Path: the given path appears to be a" +
            " file name instead of a path: $fileName. The PirateRustBackend expects the absolutePath to the database rather" +
            " than just the database filename because Rust does not access the app Context." +
            " So pass in context.getDatabasePath(dbFileName).absolutePath instead of just dbFileName alone.",
        null
    )
    class PirateFailedReorgRepair(message: String) : PirateCompactBlockProcessorException(message)
    class PirateFailedDownload(cause: Throwable? = null) : PirateCompactBlockProcessorException(
        "Error while downloading blocks. This most " +
            "likely means the server is down or slow to respond. See logs for details.",
        cause
    )
    class PirateFailedScan(cause: Throwable? = null) : PirateCompactBlockProcessorException(
        "Error while scanning blocks. This most " +
            "likely means a block was missed or a reorg was mishandled. See logs for details.",
        cause
    )
    class PirateDisconnected(cause: Throwable? = null) : PirateCompactBlockProcessorException("Disconnected Error. Unable to download blocks due to ${cause?.message}", cause)
    object Uninitialized : PirateCompactBlockProcessorException(
        "Cannot process blocks because the wallet has not been" +
            " initialized. Verify that the seed phrase was properly created or imported. If so, then this problem" +
            " can be fixed by re-importing the wallet."
    )
    object NoAccount : PirateCompactBlockProcessorException(
        "Attempting to scan without an account. This is probably a setup error or a race condition."
    )

    open class PirateEnhanceTransactionError(message: String, val height: Int, cause: Throwable) : PirateCompactBlockProcessorException(message, cause) {
        class PirateEnhanceTxDownloadError(height: Int, cause: Throwable) : PirateEnhanceTransactionError("Error while attempting to download a transaction to enhance", height, cause)
        class PirateEnhanceTxDecryptError(height: Int, cause: Throwable) : PirateEnhanceTransactionError("Error while attempting to decrypt and store a transaction to enhance", height, cause)
    }

    class PirateMismatchedNetwork(clientNetwork: String?, serverNetwork: String?) : PirateCompactBlockProcessorException(
        "Incompatible server: this client expects a server using $clientNetwork but it was $serverNetwork! Try updating the client or switching servers."
    )

    class PirateMismatchedBranch(clientBranch: String?, serverBranch: String?, networkName: String?) : PirateCompactBlockProcessorException(
        "Incompatible server: this client expects a server following consensus branch $clientBranch on $networkName but it was $serverBranch! Try updating the client or switching servers."
    )
}

/**
 * Exceptions related to the wallet's birthday.
 */
sealed class PirateBirthdayException(message: String, cause: Throwable? = null) : PirateSdkException(message, cause) {
    object UninitializedBirthdayException : PirateBirthdayException(
        "Error the birthday cannot be" +
            " accessed before it is initialized. Verify that the new, import or open functions" +
            " have been called on the initializer."
    )
    class PirateMissingBirthdayFilesException(directory: String) : PirateBirthdayException(
        "Cannot initialize wallet because no birthday files were found in the $directory directory."
    )
    class PirateExactBirthdayNotFoundException(height: Int, nearestMatch: Int? = null) : PirateBirthdayException(
        "Unable to find birthday that exactly matches $height.${
        if (nearestMatch != null)
            " An exact match was request but the nearest match found was $nearestMatch."
        else ""
        }"
    )
    class PirateBirthdayFileNotFoundException(directory: String, height: Int?) : PirateBirthdayException(
        "Unable to find birthday file for $height verify that $directory/$height.json exists."
    )
    class PirateMalformattedBirthdayFilesException(directory: String, file: String) : PirateBirthdayException(
        "Failed to parse file $directory/$file verify that it is formatted as #####.json, " +
            "where the first portion is an Int representing the height of the tree contained in the file"
    )
}

/**
 * Exceptions thrown by the initializer.
 */
sealed class PirateInitializerException(message: String, cause: Throwable? = null) : PirateSdkException(message, cause) {
    class PirateFalseStart(cause: Throwable?) : PirateInitializerException("Failed to initialize accounts due to: $cause", cause)
    class PirateAlreadyInitializedException(cause: Throwable, dbPath: String) : PirateInitializerException(
        "Failed to initialize the blocks table" +
            " because it already exists in $dbPath",
        cause
    )
    object MissingBirthdayException : PirateInitializerException(
        "Expected a birthday for this wallet but failed to find one. This usually means that " +
            "wallet setup did not happen correctly. A workaround might be to interpret the " +
            "birthday,  based on the contents of the wallet data but it is probably better " +
            "not to mask this error because the root issue should be addressed."
    )
    object MissingViewingKeyException : PirateInitializerException(
        "Expected a unified viewingKey for this wallet but failed to find one. This usually means" +
            " that wallet setup happened incorrectly. A workaround might be to derive the" +
            " unified viewingKey from the seed or seedPhrase, if they exist, but it is probably" +
            " better not to mask this error because the root issue should be addressed."
    )
    class PirateMissingAddressException(description: String, cause: Throwable? = null) : PirateInitializerException(
        "Expected a $description address for this wallet but failed to find one. This usually" +
            " means that wallet setup happened incorrectly. If this problem persists, a" +
            " workaround might be to go to settings and WIPE the wallet and rescan. Doing so" +
            " will restore any missing address information. Meanwhile, please report that" +
            " this happened so that the root issue can be uncovered and corrected." +
            if (cause != null) "\nCaused by: $cause" else ""
    )
    object DatabasePathException :
        PirateInitializerException(
            "Critical failure to locate path for storing databases. Perhaps this device prevents" +
                " apps from storing data? We cannot initialize the wallet unless we can store" +
                " data."
        )

    class PirateInvalidBirthdayHeightException(height: Int?, network: PirateNetwork) : PirateInitializerException(
        "Invalid birthday height of $height. The birthday height must be at least the height of" +
            " Sapling activation on ${network.networkName} (${network.saplingActivationHeight})."
    )

    object PirateMissingDefaultBirthdayException : PirateInitializerException(
        "The birthday height is missing and it is unclear which value to use as a default."
    )
}

/**
 * Exceptions thrown while interacting with lightwalletd.
 */
sealed class PirateLightWalletException(message: String, cause: Throwable? = null) : PirateSdkException(message, cause) {
    object InsecureConnection : PirateLightWalletException(
        "Error: attempted to connect to lightwalletd" +
            " with an insecure connection! Plaintext connections are only allowed when the" +
            " resource value for 'R.bool.lightwalletd_allow_very_insecure_connections' is true" +
            " because this choice should be explicit."
    )
    class PirateConsensusBranchException(sdkBranch: String, lwdBranch: String) :
        PirateLightWalletException(
            "Error: the lightwalletd server is using a consensus branch" +
                " (branch: $lwdBranch) that does not match the transactions being created" +
                " (branch: $sdkBranch). This probably means the SDK and Server are on two" +
                " different chains, most likely because of a recent network upgrade (NU). Either" +
                " update the SDK to match lightwalletd or use a lightwalletd that matches the SDK."
        )

    open class PirateChangeServerException(message: String, cause: Throwable? = null) : PirateSdkException(message, cause) {
        class PirateChainInfoNotMatching(val propertyNames: String, val expectedInfo: Service.LightdInfo, val actualInfo: Service.LightdInfo) : PirateChangeServerException(
            "Server change error: the $propertyNames values did not match."
        )
        class PirateStatusException(val status: Status, cause: Throwable? = null) : PirateSdkException(status.toMessage(), cause) {
            companion object {
                private fun Status.toMessage(): String {
                    return when (this.code) {
                        UNAVAILABLE -> {
                            "Error: the new server is unavailable. Verify that the host and port are correct. Failed with $this"
                        }
                        else -> "Changing servers failed with status $this"
                    }
                }
            }
        }
    }
}

/**
 * Potentially user-facing exceptions thrown while encoding transactions.
 */
sealed class PirateTransactionEncoderException(message: String, cause: Throwable? = null) : PirateSdkException(message, cause) {
    class PirateFetchParamsException(message: String) : PirateTransactionEncoderException("Failed to fetch params due to: $message")
    object MissingParamsException : PirateTransactionEncoderException(
        "Cannot send funds due to missing spend or output params and attempting to download them failed."
    )
    class PirateTransactionNotFoundException(transactionId: Long) : PirateTransactionEncoderException(
        "Unable to find transactionId " +
            "$transactionId in the repository. This means the wallet created a transaction and then returned a row ID " +
            "that does not actually exist. This is a scenario where the wallet should have thrown an exception but failed " +
            "to do so."
    )
    class PirateTransactionNotEncodedException(transactionId: Long) : PirateTransactionEncoderException(
        "The transaction returned by the wallet," +
            " with id $transactionId, does not have any raw data. This is a scenario where the wallet should have thrown" +
            " an exception but failed to do so."
    )
    class PirateIncompleteScanException(lastScannedHeight: Int) : PirateTransactionEncoderException(
        "Cannot" +
            " create spending transaction because scanning is incomplete. We must scan up to the" +
            " latest height to know which consensus rules to apply. However, the last scanned" +
            " height was $lastScannedHeight."
    )
}
