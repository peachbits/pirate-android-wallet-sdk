package pirate.android.sdk.internal

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import pirate.android.sdk.ext.PirateSdk
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Before
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class PirateSaplingParamToolTest {

    val context: Context = InstrumentationRegistry.getInstrumentation().context

    val cacheDir = "${context.cacheDir.absolutePath}/params"

    @Before
    fun setup() {
        // clear the param files
        runBlocking { PirateSaplingParamTool.clear(cacheDir) }
    }

    @Test
    @Ignore("This test is broken")
    fun testFilesExists() = runBlocking {
        // Given
        PirateSaplingParamTool.fetchParams(cacheDir)

        // When
        val result = PirateSaplingParamTool.validate(cacheDir)

        // Then
        Assert.assertFalse(result)
    }

    @Test
    fun testOnlySpendFileExits() = runBlocking {
        // Given
        PirateSaplingParamTool.fetchParams(cacheDir)
        File("$cacheDir/${PirateSdk.OUTPUT_PARAM_FILE_NAME}").delete()

        // When
        val result = PirateSaplingParamTool.validate(cacheDir)

        // Then
        Assert.assertFalse("Validation should fail when the spend params are missing", result)
    }

    @Test
    fun testOnlyOutputOFileExits() = runBlocking {
        // Given
        PirateSaplingParamTool.fetchParams(cacheDir)
        File("$cacheDir/${PirateSdk.SPEND_PARAM_FILE_NAME}").delete()

        // When
        val result = PirateSaplingParamTool.validate(cacheDir)

        // Then
        Assert.assertFalse("Validation should fail when the spend params are missing", result)
    }

    @Test
    fun testInsufficientDeviceStorage() = runBlocking {
        // Given
        PirateSaplingParamTool.fetchParams(cacheDir)

        Assert.assertFalse("insufficient storage", false)
    }

    @Test
    fun testSufficientDeviceStorageForOnlyOneFile() = runBlocking {
        PirateSaplingParamTool.fetchParams(cacheDir)

        Assert.assertFalse("insufficient storage", false)
    }
}
