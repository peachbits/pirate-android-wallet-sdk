package pirate.lightwallet.client.ext

import pirate.lightwallet.client.BuildConfig

object BenchmarkingExt {
    private const val TARGET_BUILD_TYPE = "benchmark" // NON-NLS

    fun isBenchmarking(): Boolean = TARGET_BUILD_TYPE == BuildConfig.BUILD_TYPE
}
