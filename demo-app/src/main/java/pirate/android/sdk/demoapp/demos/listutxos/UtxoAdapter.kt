package pirate.android.sdk.demoapp.demos.listutxos

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import pirate.android.sdk.db.entity.PirateConfirmedTransaction
import pirate.android.sdk.demoapp.R

/**
 * Simple adapter implementation that knows how to bind a recyclerview to ClearedTransactions.
 */
class UtxoAdapter : ListAdapter<PirateConfirmedTransaction, UtxoViewHolder>(
    object : DiffUtil.ItemCallback<PirateConfirmedTransaction>() {
        override fun areItemsTheSame(
            oldItem: PirateConfirmedTransaction,
            newItem: PirateConfirmedTransaction
        ) = oldItem.minedHeight == newItem.minedHeight

        override fun areContentsTheSame(
            oldItem: PirateConfirmedTransaction,
            newItem: PirateConfirmedTransaction
        ) = oldItem == newItem
    }
) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ) = UtxoViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
    )

    override fun onBindViewHolder(
        holder: UtxoViewHolder,
        position: Int
    ) = holder.bindTo(getItem(position))
}
