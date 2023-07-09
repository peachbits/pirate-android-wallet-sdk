package pirate.android.sdk.demoapp.demos.getblock

import android.os.Bundle
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import pirate.android.sdk.demoapp.BaseDemoFragment
import pirate.android.sdk.demoapp.databinding.FragmentGetBlockBinding
import pirate.android.sdk.demoapp.ext.requireApplicationContext
import pirate.android.sdk.demoapp.util.fromResources
import pirate.android.sdk.demoapp.util.mainActivity
import pirate.android.sdk.demoapp.util.toHtml
import pirate.android.sdk.demoapp.util.toRelativeTime
import pirate.android.sdk.demoapp.util.withCommas
import pirate.android.sdk.ext.toHex
import pirate.android.sdk.model.BlockHeight
import pirate.android.sdk.type.PirateNetwork
import kotlin.math.min

/**
 * Retrieves a compact block from the lightwalletd service and displays basic information about it.
 * This demonstrates the basic ability to connect to the server, request a compact block and parse
 * the response.
 */
class GetBlockFragment : BaseDemoFragment<FragmentGetBlockBinding>() {

    private fun setBlockHeight(blockHeight: BlockHeight) {
        val blocks =
            lightwalletService?.getBlockRange(blockHeight..blockHeight)
        val block = blocks?.firstOrNull()
        binding.textInfo.visibility = View.VISIBLE
        binding.textInfo.text = Html.fromHtml(
            """
                <b>block height:</b> ${block?.height.withCommas()}
                <br/><b>block time:</b> ${block?.time.toRelativeTime(requireApplicationContext())}
                <br/><b>number of shielded TXs:</b> ${block?.vtxCount}
                <br/><b>hash:</b> ${block?.hash?.toByteArray()?.toHex()}
                <br/><b>prevHash:</b> ${block?.prevHash?.toByteArray()?.toHex()}
                ${block?.vtxList.toHtml()}
            """.trimIndent()
        )
    }

    private fun onApply(_unused: View? = null) {
        val network = PirateNetwork.fromResources(requireApplicationContext())
        val newHeight = min(binding.textBlockHeight.text.toString().toLongOrNull() ?: network.saplingActivationHeight.value, network.saplingActivationHeight.value)

        try {
            setBlockHeight(BlockHeight.new(network, newHeight))
        } catch (t: Throwable) {
            toast("Error: $t")
        }
        mainActivity()?.hideKeyboard()
    }

    private fun loadNext(offset: Int) {
        val nextBlockHeight = (binding.textBlockHeight.text.toString().toIntOrNull() ?: -1) + offset
        binding.textBlockHeight.setText(nextBlockHeight.toString())
        onApply()
    }

    //
    // Android Lifecycle overrides
    //

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.buttonApply.setOnClickListener(::onApply)
        binding.buttonPrevious.setOnClickListener {
            loadNext(-1)
        }
        binding.buttonNext.setOnClickListener {
            loadNext(1)
        }
    }

    //
    // Base Fragment overrides
    //

    override fun inflateBinding(layoutInflater: LayoutInflater): FragmentGetBlockBinding =
        FragmentGetBlockBinding.inflate(layoutInflater)
}
