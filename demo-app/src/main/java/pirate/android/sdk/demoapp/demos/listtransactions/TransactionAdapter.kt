package pirate.android.sdk.demoapp.demos.listtransactions

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import pirate.android.sdk.db.entity.PirateConfirmedTransaction
import pirate.android.sdk.demoapp.R

/**
 * Simple adapter implementation that knows how to bind a recyclerview to ClearedTransactions.
 */
class TransactionAdapter : ListAdapter<PirateConfirmedTransaction, TransactionViewHolder>(
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
    ) = TransactionViewHolder(
        LayoutInflater.from(parent.context).inflate(R.layout.item_transaction, parent, false)
    )

    override fun onBindViewHolder(
        holder: TransactionViewHolder,
        position: Int
    ) = holder.bindTo(getItem(position))
}
