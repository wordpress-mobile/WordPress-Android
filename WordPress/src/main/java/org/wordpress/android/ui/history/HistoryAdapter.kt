package org.wordpress.android.ui.history

import android.os.Bundle
import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView.Adapter
import android.view.ViewGroup
import org.wordpress.android.ui.history.HistoryListItem.Footer
import org.wordpress.android.ui.history.HistoryListItem.Header
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.ui.history.HistoryListItem.ViewType

class HistoryAdapter(
    private val itemClickListener: (HistoryListItem) -> Unit
) : Adapter<HistoryViewHolder>() {
    private val list = mutableListOf<HistoryListItem>()

    init {
        setHasStableIds(true)
    }

    override fun getItemCount(): Int {
        return list.size
    }

    override fun getItemId(position: Int): Long = list[position].longId()

    override fun getItemViewType(position: Int): Int = list[position].type.id

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int, payloads: List<Any>) {
        if (payloads.isNotEmpty() && (payloads[0] as? Bundle)?.size() ?: 0 > 0) {
            val bundle = payloads[0] as Bundle
            holder.updateChanges(bundle)
        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        when (holder) {
            is FooterItemViewHolder -> holder.bind(list[position] as Footer)
            is HeaderItemViewHolder -> holder.bind(list[position] as Header)
            is RevisionItemViewHolder -> holder.bind(list[position] as Revision)
            else -> throw IllegalArgumentException("Unexpected view holder in History")
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        return when (viewType) {
            ViewType.REVISION.id -> RevisionItemViewHolder(
                    parent,
                    itemClickListener
            )
            ViewType.FOOTER.id -> FooterItemViewHolder(parent)
            ViewType.HEADER.id -> HeaderItemViewHolder(parent)
            else -> throw IllegalArgumentException("Unexpected view type in History")
        }
    }

    internal fun updateList(items: List<HistoryListItem>) {
        list.clear()
        list.addAll(items)
        DiffUtil.calculateDiff(HistoryDiffCallback(list.toList(), items)).dispatchUpdatesTo(this)
    }
}
