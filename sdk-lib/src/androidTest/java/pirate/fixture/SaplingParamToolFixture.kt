package pirate.fixture

import pirate.android.sdk.internal.PirateSaplingParamTool
import pirate.android.sdk.internal.PirateSaplingParamToolProperties
import pirate.android.sdk.internal.SaplingParameters
import java.io.File

object PirateSaplingParamToolFixture {

    internal val PARAMS_DIRECTORY = SaplingParamsFixture.DESTINATION_DIRECTORY
    internal val PARAMS_LEGACY_DIRECTORY = SaplingParamsFixture.DESTINATION_DIRECTORY_LEGACY
    internal val SAPLING_PARAMS_FILES = listOf(
        SaplingParameters(
            PARAMS_DIRECTORY,
            PirateSaplingParamTool.SPEND_PARAM_FILE_NAME,
            PirateSaplingParamTool.SPEND_PARAM_FILE_MAX_BYTES_SIZE,
            PiratPirateSaplingParamTool.SPEND_PARAM_FILE_SHA1_HASH
        ),
        SaplingParameters(
            PARAMS_DIRECTORY,
            PirateSaplingParamTool.OUTPUT_PARAM_FILE_NAME,
            PirateSaplingParamTool.OUTPUT_PARAM_FILE_MAX_BYTES_SIZE,
            PirateSaplingParamTool.OUTPUT_PARAM_FILE_SHA1_HASH
        )
    )

    internal fun new(
        saplingParamsFiles: List<SaplingParameters> = SAPLING_PARAMS_FILES,
        paramsDirectory: File = PARAMS_DIRECTORY,
        paramsLegacyDirectory: File = PARAMS_LEGACY_DIRECTORY
    ) = PirateSaplingParamToolProperties(
        saplingParams = saplingParamsFiles,
        paramsDirectory = paramsDirectory,
        paramsLegacyDirectory = paramsLegacyDirectory
    )
}
