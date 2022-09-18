package pirate.android.sdk.internal

import pirate.android.sdk.exception.PirateTransactionEncoderException
import pirate.android.sdk.ext.PirateSdk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.buffer
import okio.sink
import java.io.File

class SaplingParamTool {

    companion object {
        /**
         * Checks the given directory for the output and spending params and calls [fetchParams] if
         * they're missing.
         *
         * @param destinationDir the directory where the params should be stored.
         */
        suspend fun ensureParams(destinationDir: String) {
            var hadError = false
            arrayOf(
                PirateSdk.SPEND_PARAM_FILE_NAME,
                PirateSdk.OUTPUT_PARAM_FILE_NAME
            ).forEach { paramFileName ->
                if (!File(destinationDir, paramFileName).existsSuspend()) {
                    twig("WARNING: $paramFileName not found at location: $destinationDir")
                    hadError = true
                }
            }
            if (hadError) {
                try {
                    Bush.trunk.twigTask("attempting to download missing params") {
                        fetchParams(destinationDir)
                    }
                } catch (e: Throwable) {
                    twig("failed to fetch params due to: $e")
                    throw PirateTransactionEncoderException.MissingParamsException
                }
            }
        }

        /**
         * Download and store the params into the given directory.
         *
         * @param destinationDir the directory where the params will be stored. It's assumed that we
         * have write access to this directory. Typically, this should be the app's cache directory
         * because it is not harmful if these files are cleared by the user since they are downloaded
         * on-demand.
         */
        suspend fun fetchParams(destinationDir: String) {
            val client = createHttpClient()
            var failureMessage = ""
            arrayOf(
                PirateSdk.SPEND_PARAM_FILE_NAME,
                PirateSdk.OUTPUT_PARAM_FILE_NAME
            ).forEach { paramFileName ->
                val url = "${PirateSdk.CLOUD_PARAM_DIR_URL}/$paramFileName"
                val request = Request.Builder().url(url).build()
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                if (response.isSuccessful) {
                    twig("fetch succeeded", -1)
                    val file = File(destinationDir, paramFileName)
                    if (file.parentFile?.existsSuspend() == true) {
                        twig("directory exists!", -1)
                    } else {
                        twig("directory did not exist attempting to make it")
                        file.parentFile?.mkdirsSuspend()
                    }
                    withContext(Dispatchers.IO) {
                        response.body?.let { body ->
                            body.source().use { source ->
                                file.sink().buffer().use { sink ->
                                    twig("writing to $file")
                                    sink.writeAll(source)
                                }
                            }
                        }
                    }
                } else {
                    failureMessage += "Error while fetching $paramFileName : $response\n"
                    twig(failureMessage)
                }

                twig("fetch succeeded, done writing $paramFileName")
            }
            if (failureMessage.isNotEmpty()) throw PirateTransactionEncoderException.PirateFetchParamsException(
                failureMessage
            )
        }

        suspend fun clear(destinationDir: String) {
            if (validate(destinationDir)) {
                arrayOf(
                    PirateSdk.SPEND_PARAM_FILE_NAME,
                    PirateSdk.OUTPUT_PARAM_FILE_NAME
                ).forEach { paramFileName ->
                    val file = File(destinationDir, paramFileName)
                    if (file.deleteRecursivelySuspend()) {
                        twig("Files deleted successfully")
                    } else {
                        twig("Error: Files not able to be deleted!")
                    }
                }
            }
        }

        suspend fun validate(destinationDir: String): Boolean {
            return arrayOf(
                PirateSdk.SPEND_PARAM_FILE_NAME,
                PirateSdk.OUTPUT_PARAM_FILE_NAME
            ).all { paramFileName ->
                File(destinationDir, paramFileName).existsSuspend()
            }.also {
                println("Param files${if (!it) "did not" else ""} both exist!")
            }
        }

        //
        // Helpers
        //
        /**
         * Http client is only used for downloading sapling spend and output params data, which are
         * necessary for the wallet to scan blocks.
         *
         * @return an http client suitable for downloading params data.
         */
        private fun createHttpClient(): OkHttpClient {
            // TODO: add logging and timeouts
            return OkHttpClient()
        }
    }
}

suspend fun File.existsSuspend() = withContext(Dispatchers.IO) { exists() }
suspend fun File.mkdirsSuspend() = withContext(Dispatchers.IO) { mkdirs() }
suspend fun File.deleteRecursivelySuspend() = withContext(Dispatchers.IO) { deleteRecursively() }
