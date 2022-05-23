package org.wordpress.android.ui.quickstart

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.fluxc.store.QuickStartStore.QuickStartTask
import org.wordpress.android.ui.quickstart.QuickStartFullScreenDialogFragment.QuickStartTaskCard
import org.wordpress.android.ui.quickstart.viewholders.TaskViewHolder

class QuickStartAdapter : ListAdapter<QuickStartTaskCard, ViewHolder>(QuickStartAdapterDiffCallback) {
    private var listener: OnQuickStartAdapterActionListener? = null

    override fun onCreateViewHolder(viewGroup: ViewGroup, viewType: Int) = when (viewType) {
        VIEW_TYPE_TASK -> TaskViewHolder(
                parent = viewGroup,
                listener = listener
        )
        else -> throw IllegalArgumentException("Unexpected view type")
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, position: Int) {
        when (viewHolder) {
            is TaskViewHolder -> viewHolder.bind(
                    taskCard = getItem(position),
                    shouldHideDivider = position == itemCount - 1
            )
        }
    }

    override fun getItemViewType(position: Int): Int {
        return VIEW_TYPE_TASK
    }

    override fun getItemCount(): Int {
        return currentList.size
    }

    fun setOnTaskTappedListener(listener: OnQuickStartAdapterActionListener?) {
        this.listener = listener
    }

    interface OnQuickStartAdapterActionListener {
        fun onSkipTaskTapped(task: QuickStartTask)
        fun onTaskTapped(task: QuickStartTask)
    }

    object QuickStartAdapterDiffCallback : DiffUtil.ItemCallback<QuickStartTaskCard>() {
        override fun areItemsTheSame(oldItem: QuickStartTaskCard, updatedItem: QuickStartTaskCard) =
                oldItem.task.string == updatedItem.task.string

        override fun areContentsTheSame(oldItem: QuickStartTaskCard, newItem: QuickStartTaskCard) =
                oldItem == newItem
    }

    companion object {
        private const val VIEW_TYPE_TASK = 0
    }
}
