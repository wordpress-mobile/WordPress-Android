package org.wordpress.android.ui.bloggingreminders

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.work.WorkInfo
import org.wordpress.android.databinding.BloggingRemindersItemBinding
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAdapter.Item
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersAdapter.ItemViewHolder
import org.wordpress.android.util.viewBinding

class BloggingRemindersAdapter(val onClick: (Item) -> Unit) : ListAdapter<Item, ItemViewHolder>(ItemDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = ItemViewHolder(parent)

    override fun onBindViewHolder(holder: ItemViewHolder, position: Int) {
        holder.bind(getItem(position), onClick)
    }

    data class Item(val workInfo: WorkInfo)

    class ItemViewHolder(
        parent: ViewGroup,
        private val binding: BloggingRemindersItemBinding = parent.viewBinding(BloggingRemindersItemBinding::inflate)
    ) : ViewHolder(binding.root) {
        fun bind(item: Item, onClick: (Item) -> Unit) = with(binding) {
            content.text = item.workInfo.toFormattedText()
            root.setOnClickListener { onClick(item) }
        }

        private fun WorkInfo.toFormattedText() = "id: $id\n" +
                "state: $state\n" +
                "data: ${outputData.keyValueMap}\n" +
                "tags: $tags\n" +
                "progress: $progress\n" +
                "run attempt count: $runAttemptCount\n"
    }

    object ItemDiffCallback : ItemCallback<Item>() {
        override fun areItemsTheSame(old: Item, new: Item) = old.workInfo.id == old.workInfo.id
        override fun areContentsTheSame(old: Item, new: Item) = old.workInfo == new.workInfo
    }
}
