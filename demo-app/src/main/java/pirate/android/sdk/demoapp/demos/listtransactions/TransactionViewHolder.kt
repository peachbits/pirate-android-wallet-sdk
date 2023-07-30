package pirate.android.sdk.demoapp.demos.listtransactions

import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import pirate.android.sdk.demoapp.R
import pirate.android.sdk.ext.convertArrrtoshiToArrrString
import pirate.android.sdk.model.TransactionOverview
import pirate.android.sdk.model.Arrrtoshi
import java.text.SimpleDateFormat
import java.util.Locale

/**
 * Simple view holder for displaying confirmed transactions in the recyclerview.
 */
class TransactionViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val amountText = itemView.findViewById<TextView>(R.id.text_transaction_amount)
    private val timeText = itemView.findViewById<TextView>(R.id.text_transaction_timestamp)
    private val icon = itemView.findViewById<ImageView>(R.id.image_transaction_type)
    private val formatter = SimpleDateFormat("M/d h:mma", Locale.getDefault())

    @Suppress("MagicNumber")
    fun bindTo(transaction: TransactionOverview) {
        bindTo(!transaction.isSentTransaction, transaction.blockTimeEpochSeconds, transaction.netValue)
    }

    @Suppress("MagicNumber")
    fun bindTo(isInbound: Boolean, time: Long, value: Arrrtoshi) {
        amountText.text = value.convertArrrtoshiToArrrString()
        timeText.text =
            if (time == 0L) {
                "Pending"
            } else {
                formatter.format(time * 1000L)
            }

        icon.rotation = if (isInbound) 0f else 180f
        icon.rotation = if (isInbound) 0f else 180f
        icon.setColorFilter(
            ContextCompat.getColor(itemView.context, if (isInbound) R.color.tx_inbound else R.color.tx_outbound)
        )
    }
}
