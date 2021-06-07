package org.wordpress.android.ui.mysite

import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.databinding.BloggingReminderTaskCardBinding
import org.wordpress.android.ui.mysite.BloggingReminderTaskCardAdapter.BloggingReminderTaskCardViewHolder
import org.wordpress.android.ui.mysite.MySiteItem.DynamicCard.BloggingReminderCard.BloggingReminderTaskCard
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

class BloggingReminderTaskCardAdapter(private val uiHelpers: UiHelpers) : Adapter<BloggingReminderTaskCardViewHolder>() {
    private var items = listOf<BloggingReminderTaskCard>()

    fun loadData(newItems: List<BloggingReminderTaskCard>) {
        val diffResult = DiffUtil.calculateDiff(BloggingReminderTaskCardAdapterDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = BloggingReminderTaskCardViewHolder(
            parent.viewBinding(BloggingReminderTaskCardBinding::inflate)
    )

    override fun onBindViewHolder(holder: BloggingReminderTaskCardViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount() = items.size

    inner class BloggingReminderTaskCardViewHolder(val binding: BloggingReminderTaskCardBinding) : ViewHolder(binding.root) {
        fun bind(taskCard: BloggingReminderTaskCard) = with(binding) {
            taskCardDescription.text = uiHelpers.getTextOfUiString(root.context, taskCard.description)
            taskCardIllustration.setImageResource(taskCard.illustration)

            taskCardView.apply {
                checkedIconTint = ContextCompat.getColorStateList(root.context, taskCard.accentColor)
                isChecked = taskCard.done
                setOnClickListener(if (taskCard.done) null else ({ taskCard.onClick.click() }))
            }
        }
    }

    inner class BloggingReminderTaskCardAdapterDiffCallback(
        private val oldItems: List<BloggingReminderTaskCard>,
        private val newItems: List<BloggingReminderTaskCard>
    ) : DiffUtil.Callback() {
        override fun getOldListSize() = oldItems.size

        override fun getNewListSize() = newItems.size

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition].quickStartTask == newItems[newItemPosition].quickStartTask

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                oldItems[oldItemPosition] == newItems[newItemPosition]
    }
}
