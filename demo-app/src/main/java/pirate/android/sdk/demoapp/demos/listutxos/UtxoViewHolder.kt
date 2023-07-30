package pirate.android.sdk.demoapp.demos.listutxos

import android.view.View
import android.widget.TextView
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
class UtxoViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val amountText = itemView.findViewById<TextView>(R.id.text_transaction_amount)
    private val timeText = itemView.findViewById<TextView>(R.id.text_transaction_timestamp)
    private val formatter = SimpleDateFormat("M/d h:mma", Locale.getDefault())

    @Suppress("MagicNumber")
    fun bindTo(transaction: TransactionOverview) {
        bindToHelper(transaction.netValue, transaction.blockTimeEpochSeconds)
    }

    @Suppress("MagicNumber")
    private fun bindToHelper(amount: Arrrtoshi, time: Long) {
        amountText.text = amount.convertArrrtoshiToArrrString()
        timeText.text = if (time == 0L) {
            "Pending"
        } else {
            formatter.format(time * 1000L)
        }
    }
}
