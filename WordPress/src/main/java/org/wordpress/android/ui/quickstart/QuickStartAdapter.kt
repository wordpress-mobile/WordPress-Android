package org.wordpress.android.ui.quickstart

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment.QuickStartListCard
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment.QuickStartListCard.QuickStartHeaderCard
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment.QuickStartListCard.QuickStartTaskCard
import org.wordpress.android.ui.quickstart.viewholders.HeaderViewHolder
import org.wordpress.android.ui.quickstart.viewholders.TaskViewHolder
import org.wordpress.android.ui.utils.UiHelpers

class QuickStartAdapter(
    private val uiHelpers: UiHelpers
) : ListAdapter<QuickStartListCard, ViewHolder>(QuickStartAdapterDiffCallback) {
    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_HEADER -> HeaderViewHolder(parent = viewGroup, uiHelpers = uiHelpers)
        VIEW_TYPE_TASK -> TaskViewHolder(parent = viewGroup)
        else -> throw IllegalArgumentException("Unexpected view type")
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (viewHolder) {
            is HeaderViewHolder -> viewHolder.bind(getItem(position) as QuickStartHeaderCard)
            is TaskViewHolder -> viewHolder.bind(getItem(position) as QuickStartTaskCard)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) VIEW_TYPE_HEADER else VIEW_TYPE_TASK
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    object QuickStartAdapterDiffCallback : DiffUtil.ItemCallback<QuickStartListCard>() {
        override fun areItemsTheSame(oldItem: QuickStartListCard, updatedItem: QuickStartListCard): Boolean {
            return when {
                oldItem is QuickStartHeaderCard && updatedItem is QuickStartHeaderCard -> true
                (oldItem is QuickStartTaskCard && updatedItem is QuickStartTaskCard) &&
                        (oldItem.task.string == updatedItem.task.string) -> true
                else -> throw UnsupportedOperationException("Diff not implemented yet")
            }
        }

        override fun areContentsTheSame(oldItem: QuickStartListCard, newItem: QuickStartListCard) =
                oldItem == newItem
    }

    companion object {
        private const val VIEW_TYPE_HEADER = 0
        private const val VIEW_TYPE_TASK = 1
    }
}
