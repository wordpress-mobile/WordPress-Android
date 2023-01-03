package org.wordpress.android.ui.history

import android.app.Activity
import android.os.Bundle
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import org.wordpress.android.WordPress
import org.wordpress.android.ui.history.HistoryListItem.Footer
import org.wordpress.android.ui.history.HistoryListItem.Header
import org.wordpress.android.ui.history.HistoryListItem.Revision
import org.wordpress.android.ui.history.HistoryListItem.ViewType
import org.wordpress.android.util.image.ImageManager
import javax.inject.Inject

class HistoryAdapter(
    val activity: Activity,
    private val itemClickListener: (HistoryListItem) -> Unit
) : Adapter<HistoryViewHolder>() {
    private val list = mutableListOf<HistoryListItem>()

    @Inject
    lateinit var imageManager: ImageManager

    init {
        (activity.applicationContext as WordPress).component().inject(this)
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
                itemClickListener,
                imageManager
            )

            ViewType.FOOTER.id -> FooterItemViewHolder(parent)
            ViewType.HEADER.id -> HeaderItemViewHolder(parent)
            else -> throw IllegalArgumentException("Unexpected view type in History")
        }
    }

    internal fun updateList(items: List<HistoryListItem>) {
        val diffCallback = HistoryDiffCallback(list.toList(), items)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        list.clear()
        list.addAll(items)
        diffResult.dispatchUpdatesTo(this)
    }
}
