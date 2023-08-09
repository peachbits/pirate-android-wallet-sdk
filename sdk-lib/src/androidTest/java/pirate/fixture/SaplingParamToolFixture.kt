package pirate.fixture

import pirate.android.sdk.internal.SaplingParamTool
import pirate.android.sdk.internal.SaplingParamToolProperties
import pirate.android.sdk.internal.SaplingParameters
import java.io.File

object SaplingParamToolFixture {

    internal val PARAMS_DIRECTORY = SaplingParamsFixture.DESTINATION_DIRECTORY
    internal val PARAMS_LEGACY_DIRECTORY = SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY
    internal val SAPLING_PARAMS_FILES = listOf(
        SaplingParameters(
            PARAMS_DIRECTORY,
            SaplingParamTool.SPEND_PARAM_FILE_NAME,
            SaplingParamTool.SPEND_PARAM_FILE_MAX_BYTES_SIZE,
            PiratSaplingParamTool.SPEND_PARAM_FILE_SHA1_HASH
        ),
        SaplingParameters(
            PARAMS_DIRECTORY,
            SaplingParamTool.OUTPUT_PARAM_FILE_NAME,
            SaplingParamTool.OUTPUT_PARAM_FILE_MAX_BYTES_SIZE,
            SaplingParamTool.OUTPUT_PARAM_FILE_SHA1_HASH
        )
    )

    internal fun new(
        saplingParamsFiles: List<SaplingParameters> = SAPLING_PARAMS_FILES,
        paramsDirectory: File = PARAMS_DIRECTORY,
        paramsLegacyDirectory: File = PARAMS_LEGACY_DIRECTORY
    ) = SaplingParamToolProperties(
        saplingParams = saplingParamsFiles,
        paramsDirectory = paramsDirectory,
        paramsLegacyDirectory = paramsLegacyDirectory
    )
}
