package pirate.android.sdk.internal

import androidx.test.filters.MediumTest
import androidx.test.filters.SmallTest
import pirate.android.sdk.exception.PirateTransactionEncoderException
import pirate.android.sdk.internal.ext.getSha1Hash
import pirate.android.sdk.internal.ext.listFilesSuspend
import pirate.android.sdk.test.getAppContext
import pirate.fixture.PirateSaplingParamToolFixture
import pirate.fixture.SaplingParamsFixture
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PirateSaplingParamToolBasicTest {

    @Before
    fun setup() {
        // clear the param files
        runBlocking {
            SaplingParamsFixture.clearAllFilesFromDirectory(SaplingParamsFixture.DESTINATION_DIRECTORY)
            SaplingParamsFixture.clearAllFilesFromDirectory(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY)
        }
    }

    @Test
    @SmallTest
    fun init_sapling_param_tool_test() = runTest {
        val spendSaplingParams = SaplingParamsFixture.new()
        val outputSaplingParams = SaplingParamsFixture.new(
            SaplingParamsFixture.DESTINATION_DIRECTORY,
            SaplingParamsFixture.OUTPUT_FILE_NAME,
            SaplingParamsFixture.OUTPUT_FILE_MAX_SIZE,
            SaplingParamsFixture.OUTPUT_FILE_HASH
        )

        val saplingParamTool = PirateSaplingParamTool(
            PirateSaplingParamToolProperties(
                emptyList(),
                SaplingParamsFixture
                    .DESTINATION_DIRECTORY,
                SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY
            )
        )

        // we inject params files to let the ensureParams() finish successfully without executing its extended operation
        // like fetchParams, etc.
        SaplingParamsFixture.createFile(File(spendSaplingParams.destinationDirectory, spendSaplingParams.fileName))
        SaplingParamsFixture.createFile(File(outputSaplingParams.destinationDirectory, outputSaplingParams.fileName))

        saplingParamTool.ensureParams(spendSaplingParams.destinationDirectory)
    }

    @Test
    @SmallTest
    fun init_and_get_params_destination_dir_test() = runTest {
        val destDir = PirateSaplingParamTool.new(getAppContext()).properties.paramsDirectory

        assertNotNull(destDir)
        assertEquals(
            SaplingParamsFixture.DESTINATION_DIRECTORY.absolutePath,
            destDir.absolutePath,
            "Failed to validate init operation's destination directory."
        )
    }

    @Test
    @MediumTest
    fun move_files_from_legacy_destination_test() = runTest {
        SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY.mkdirs()
        val spendFile = File(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, SaplingParamsFixture.SPEND_FILE_NAME)
        val outputFile = File(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, SaplingParamsFixture.OUTPUT_FILE_NAME)

        // now we inject params files to the legacy location to be "moved" to the preferred location
        SaplingParamsFixture.createFile(spendFile)
        SaplingParamsFixture.createFile(outputFile)

        assertTrue(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, spendFile))
        assertTrue(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, outputFile))
        assertFalse(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY, spendFile))
        assertFalse(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY, outputFile))

        // we need to use modified array of sapling parameters to pass through the SHA1 hashes validation
        val destDir = PirateSaplingParamTool.initAndGetParamsDestinationDir(
            PirateSaplingParamToolFixture.new(
                saplingParamsFiles = listOf(
                    SaplingParameters(
                        PirateSaplingParamToolFixture.PARAMS_DIRECTORY,
                        PirateSaplingParamTool.SPEND_PARAM_FILE_NAME,
                        PirateSaplingParamTool.SPEND_PARAM_FILE_MAX_BYTES_SIZE,
                        spendFile.getSha1Hash()
                    ),
                    SaplingParameters(
                        PirateSaplingParamToolFixture.PARAMS_DIRECTORY,
                        PirateSaplingParamTool.OUTPUT_PARAM_FILE_NAME,
                        PirateSaplingParamTool.OUTPUT_PARAM_FILE_MAX_BYTES_SIZE,
                        outputFile.getSha1Hash()
                    )
                )
            )
        )

        assertEquals(
            SaplingParamsFixture.DESTINATION_DIRECTORY.absolutePath,
            destDir.absolutePath
        )

        assertFalse(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, spendFile))
        assertFalse(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY, outputFile))
        assertTrue(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY, spendFile))
        assertTrue(isFileInPlace(SaplingParamsFixture.DESTINATION_DIRECTORY, outputFile))
    }

    private suspend fun isFileInPlace(directory: File, file: File): Boolean {
        return directory.listFilesSuspend()?.any { it.name == file.name } ?: false
    }

    @Test
    @MediumTest
    fun ensure_params_exception_thrown_test() = runTest {
        val saplingParamTool = PirateSaplingParamTool(
            PirateSaplingParamToolFixture.new(
                saplingParamsFiles = listOf(
                    SaplingParameters(
                        PirateSaplingParamToolFixture.PARAMS_DIRECTORY,
                        "test_file_1",
                        PirateSaplingParamTool.SPEND_PARAM_FILE_MAX_BYTES_SIZE,
                        PirateSaplingParamTool.SPEND_PARAM_FILE_SHA1_HASH
                    ),
                    SaplingParameters(
                        PirateSaplingParamToolFixture.PARAMS_DIRECTORY,
                        "test_file_0",
                        PirateSaplingParamTool.OUTPUT_PARAM_FILE_MAX_BYTES_SIZE,
                        PirateSaplingParamTool.OUTPUT_PARAM_FILE_SHA1_HASH
                    )
                )
            )
        )

        // now we inject params files to the preferred location to pass through the check missing files phase
        SaplingParamsFixture.createFile(
            File(
                saplingParamTool.properties.saplingParams[0].destinationDirectory,
                saplingParamTool.properties.saplingParams[0].fileName
            )
        )
        SaplingParamsFixture.createFile(
            File(
                saplingParamTool.properties.saplingParams[1].destinationDirectory,
                saplingParamTool.properties.saplingParams[1].fileName
            )
        )

        // the ensure params block should fail in validation phase, because we use a different params file names
        assertFailsWith<PirateTransactionEncoderException.PirateMissingParamsException> {
            saplingParamTool.ensureParams(PirateSaplingParamToolFixture.PARAMS_DIRECTORY)
        }
    }
}
