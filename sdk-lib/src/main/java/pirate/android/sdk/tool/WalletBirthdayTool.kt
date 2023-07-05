package pirate.android.sdk.tool

import android.content.Context
import androidx.annotation.VisibleForTesting
import pirate.android.sdk.exception.PirateBirthdayException
import pirate.android.sdk.internal.from
import pirate.android.sdk.internal.twig
import pirate.android.sdk.type.PirateWalletBirthday
import pirate.android.sdk.type.PirateNetwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.util.*

/**
 * Tool for loading checkpoints for the wallet, based on the height at which the wallet was born.
 */
object PirateWalletBirthdayTool {

    // Behavior change implemented as a fix for issue #270.  Temporarily adding a boolean
    // that allows the change to be rolled back quickly if needed, although long-term
    // this flag should be removed.
    @VisibleForTesting
    internal val IS_FALLBACK_ON_FAILURE = true

    /**
     * Load the nearest checkpoint to the given birthday height. If null is given, then this
     * will load the most recent checkpoint available.
     */
    suspend fun loadNearest(
        context: Context,
        network: PirateNetwork,
        birthdayHeight: Int? = null
    ): PirateWalletBirthday {
        // TODO: potentially pull from shared preferences first
        return loadBirthdayFromAssets(context, network, birthdayHeight)
    }

    /**
     * Useful for when an exact checkpoint is needed, like for SAPLING_ACTIVATION_HEIGHT. In
     * most cases, loading the nearest checkpoint is preferred for privacy reasons.
     */
    suspend fun loadExact(context: Context, network: PirateNetwork, birthdayHeight: Int) =
        loadNearest(context, network, birthdayHeight).also {
            if (it.height != birthdayHeight)
                throw PirateBirthdayException.PirateExactBirthdayNotFoundException(
                    birthdayHeight,
                    it.height
                )
        }

    // Converting this to suspending will then propagate
    @Throws(IOException::class)
    internal suspend fun listBirthdayDirectoryContents(context: Context, directory: String) =
        withContext(Dispatchers.IO) {
            context.assets.list(directory)
        }

    /**
     * Returns the directory within the assets folder where birthday data
     * (i.e. sapling trees for a given height) can be found.
     */
    @VisibleForTesting
    internal fun birthdayDirectory(network: PirateNetwork) =
        "piratesaplingtree/${(network.networkName as java.lang.String).toLowerCase(Locale.ROOT)}"

    internal fun birthdayHeight(fileName: String) = fileName.split('.').first().toInt()

    private fun Array<String>.sortDescending() =
        apply { sortByDescending { birthdayHeight(it) } }

    /**
     * Load the given birthday file from the assets of the given context. When no height is
     * specified, we default to the file with the greatest name.
     *
     * @param context the context from which to load assets.
     * @param birthdayHeight the height file to look for among the file names.
     *
     * @return a PirateWalletBirthday that reflects the contents of the file or an exception when
     * parsing fails.
     */
    private suspend fun loadBirthdayFromAssets(
        context: Context,
        network: PirateNetwork,
        birthdayHeight: Int? = null
    ): PirateWalletBirthday {
        twig("loading birthday from assets: $birthdayHeight")
        val directory = birthdayDirectory(network)
        val treeFiles = getFilteredFileNames(context, directory, birthdayHeight)

        twig("found ${treeFiles.size} sapling tree checkpoints: $treeFiles")

        return getFirstValidWalletBirthday(context, directory, treeFiles)
    }

    private suspend fun getFilteredFileNames(
        context: Context,
        directory: String,
        birthdayHeight: Int? = null
    ): List<String> {
        val unfilteredTreeFiles = listBirthdayDirectoryContents(context, directory)
        if (unfilteredTreeFiles.isNullOrEmpty()) {
            throw PirateBirthdayException.PirateMissingBirthdayFilesException(directory)
        }

        val filteredTreeFiles = unfilteredTreeFiles
            .sortDescending()
            .filter { filename ->
                birthdayHeight?.let { birthdayHeight(filename) <= it } ?: true
            }

        if (filteredTreeFiles.isEmpty()) {
            throw PirateBirthdayException.PirateBirthdayFileNotFoundException(
                directory,
                birthdayHeight
            )
        }

        return filteredTreeFiles
    }

    /**
     * @param treeFiles A list of files, sorted in descending order based on `int` value of the first part of the filename.
     */
    @VisibleForTesting
    internal suspend fun getFirstValidWalletBirthday(
        context: Context,
        directory: String,
        treeFiles: List<String>
    ): PirateWalletBirthday {
        var lastException: Exception? = null
        treeFiles.forEach { treefile ->
            try {
                val jsonString = withContext(Dispatchers.IO) {
                    context.assets.open("$directory/$treefile").use { inputStream ->
                        inputStream.reader().use { inputStreamReader ->
                            BufferedReader(inputStreamReader).use { bufferedReader ->
                                bufferedReader.readText()
                            }
                        }
                    }
                }

                return PirateWalletBirthday.from(jsonString)
            } catch (t: Throwable) {
                val exception = PirateBirthdayException.PirateMalformattedBirthdayFilesException(
                    directory,
                    treefile,
                    t
                )
                lastException = exception

                if (IS_FALLBACK_ON_FAILURE) {
                    // TODO: If we ever add crash analytics hooks, this would be something to report
                    twig("Malformed birthday file $t")
                } else {
                    throw exception
                }
            }
        }

        throw lastException!!
    }
}
