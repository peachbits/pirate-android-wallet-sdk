package pirate.android.sdk.ext

import pirate.android.sdk.BuildConfig

object BenchmarkingExt {
    private const val TARGET_BUILD_TYPE = "benchmark" // NON-NLS

    fun isBenchmarking(): Boolean = TARGET_BUILD_TYPE == BuildConfig.BUILD_TYPE
}
