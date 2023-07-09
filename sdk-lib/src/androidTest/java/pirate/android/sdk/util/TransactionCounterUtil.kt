package pirate.android.sdk.util

import androidx.test.platform.app.InstrumentationRegistry
import pirate.android.sdk.internal.PirateTroubleshootingTwig
import pirate.android.sdk.internal.Twig
import pirate.android.sdk.internal.service.PirateLightWalletGrpcService
import pirate.android.sdk.internal.twig
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.type.PirateNetwork
import org.junit.Ignore
import org.junit.Test

class TransactionCounterUtil {

    private val network = PirateNetwork.Mainnet
    private val context = InstrumentationRegistry.getInstrumentation().context
    private val service = PirateLightWalletGrpcService(context, network)

    init {
        Twig.plant(PirateTroubleshootingTwig())
    }

    @Test
    @Ignore("This test is broken")
    fun testBlockSize() {
        val sizes = mutableMapOf<Int, Int>()
        service.getBlockRange(
            BlockHeight.new(PirateNetwork.Mainnet, 900_000)..BlockHeight.new(
                PirateNetwork.Mainnet,
                910_000
            )
        ).forEach { b ->
            twig("h: ${b.header.size()}")
            val s = b.serializedSize
            sizes[s] = (sizes[s] ?: 0) + 1
        }
        twig("sizes: ${sizes.toSortedMap()}")
    }

    @Test
    @Ignore("This test is broken")
    fun testCountTransactions() {
        val txCounts = mutableMapOf<Int, Int>()
        val outputCounts = mutableMapOf<Int, Int>()
        var totalOutputs = 0
        var totalTxs = 0
        service.getBlockRange(
            BlockHeight.new(PirateNetwork.Mainnet, 900_000)..BlockHeight.new(
                PirateNetwork.Mainnet,
                950_000
            )
        ).forEach { b ->
            b.header.size()
            b.vtxList.map { it.outputsCount }.forEach { oCount ->
                outputCounts[oCount] = (outputCounts[oCount] ?: 0) + oCount.coerceAtLeast(1)
                totalOutputs += oCount
            }
            b.vtxCount.let { count ->
                txCounts[count] = (txCounts[count] ?: 0) + count.coerceAtLeast(1)
                totalTxs += count
            }
        }
        twig("txs: $txCounts")
        twig("outputs: $outputCounts")
        twig("total: $totalTxs  $totalOutputs")
    }
}
/*


 */
