package pirate.android.sdk.fixture

import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.model.PirateNetwork

object BlockRangeFixture {

    // Be aware that changing these bounds values in a broader range may result in a timeout reached in
    // SyncBlockchainBenchmark. So if changing these, don't forget to align also the test timeout in
    // waitForBalanceScreen() appropriately.

    @Suppress("MagicNumber")
    private val BLOCK_HEIGHT_LOWER_BOUND = BlockHeight.new(PirateNetwork.Mainnet, 1730001L)

    @Suppress("MagicNumber")
    private val BLOCK_HEIGHT_UPPER_BOUND = BlockHeight.new(PirateNetwork.Mainnet, 1730100L)

    fun new(
        lowerBound: BlockHeight = BLOCK_HEIGHT_LOWER_BOUND,
        upperBound: BlockHeight = BLOCK_HEIGHT_UPPER_BOUND
    ): ClosedRange<BlockHeight> {
        return lowerBound..upperBound
    }
}
