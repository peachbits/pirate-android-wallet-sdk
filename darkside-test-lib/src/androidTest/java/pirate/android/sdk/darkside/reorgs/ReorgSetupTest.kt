package pirate.android.sdk.darkside.reorgs

import androidx.test.ext.junit.runners.AndroidJUnit4
import pirate.android.sdk.darkside.test.DarksideTestCoordinator
import pirate.android.sdk.darkside.test.ScopedTest
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.type.PirateNetwork
import org.junit.Before
import org.junit.BeforeClass
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ReorgSetupTest : ScopedTest() {

    private val birthdayHeight = BlockHeight.new(PirateNetwork.Mainnet, 663150)
    private val targetHeight = BlockHeight.new(PirateNetwork.Mainnet, 663250)

    @Before
    fun setup() {
        sithLord.await()
    }

    @Test
    fun testBeforeReorg_minHeight() = timeout(30_000L) {
        // validate that we are synced, at least to the birthday height
        validator.validateMinHeightDownloaded(birthdayHeight)
    }

    @Test
    fun testBeforeReorg_maxHeight() = timeout(30_000L) {
        // validate that we are not synced beyond the target height
        validator.validateMaxHeightScanned(targetHeight)
    }

    companion object {

        private val sithLord = DarksideTestCoordinator()
        private val validator = sithLord.validator

        @BeforeClass
        @JvmStatic
        fun startOnce() {
            sithLord.enterTheDarkside()
            sithLord.synchronizer.start(classScope)
        }
    }
}
