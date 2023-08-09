package pirate.fixture

import pirate.android.sdk.internal.Files
import pirate.android.sdk.internal.SaplingParamTool
import pirate.android.sdk.internal.SaplingParameters
import pirate.android.sdk.internal.ext.createNewFileSuspend
import pirate.android.sdk.internal.ext.deleteSuspend
import pirate.android.sdk.internal.ext.existsSuspend
import pirate.android.sdk.internal.ext.listFilesSuspend
import pirate.android.sdk.test.getAppContext
import kotlinx.coroutines.runBlocking
import java.io.File

object SaplingParamsFixture {

    internal val DESTINATION_DIRECTORY_LEGACY: File = File(
        getAppContext().cacheDir,
        SaplingParamTool.SAPLING_PARAMS_LEGACY_SUBDIRECTORY
    )

    internal val DESTINATION_DIRECTORY: File
        get() = runBlocking {
            Files.getZcashNoBackupSubdirectory(getAppContext())
        }

    internal const val SPEND_FILE_NAME = SaplingParamTool.SPEND_PARAM_FILE_NAME
    internal const val SPEND_FILE_MAX_SIZE = SaplingParamTool.SPEND_PARAM_FILE_MAX_BYTES_SIZE
    internal const val SPEND_FILE_HASH = SaplingParamTool.SPEND_PARAM_FILE_SHA1_HASH

    internal const val OUTPUT_FILE_NAME = SaplingParamTool.OUTPUT_PARAM_FILE_NAME
    internal const val OUTPUT_FILE_MAX_SIZE = SaplingParamTool.OUTPUT_PARAM_FILE_MAX_BYTES_SIZE
    internal const val OUTPUT_FILE_HASH = SaplingParamTool.OUTPUT_PARAM_FILE_SHA1_HASH

    internal fun new(
        destinationDirectoryPath: File = DESTINATION_DIRECTORY,
        fileName: String = SPEND_FILE_NAME,
        fileMaxSize: Long = SPEND_FILE_MAX_SIZE,
        fileHash: String = SPEND_FILE_HASH
    ) = SaplingParameters(
        destinationDirectory = destinationDirectoryPath,
        fileName = fileName,
        fileMaxSizeBytes = fileMaxSize,
        fileHash = fileHash
    )

    internal suspend fun createFile(paramsFile: File) {
        paramsFile.createNewFileSuspend()
    }

    internal suspend fun clearAllFilesFromDirectory(destinationDir: File) {
        if (!destinationDir.existsSuspend()) {
            return
        }
        for (file in destinationDir.listFilesSuspend()!!) {
            file.deleteSuspend()
        }
    }
}
