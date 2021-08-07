package org.wordpress.android.ui.debug.cookies

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.DiffUtil.Callback
import androidx.recyclerview.widget.RecyclerView.Adapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.databinding.DebugCookieItemBinding
import org.wordpress.android.ui.debug.cookies.DebugCookiesAdapter.DebugCookieItemViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.viewBinding

class DebugCookiesAdapter : Adapter<DebugCookieItemViewHolder>() {
    private var items: List<DebugCookieItem> = listOf()

    fun update(newItems: List<DebugCookieItem>) {
        val diffResult = DiffUtil.calculateDiff(DebugCookiesDiffCallback(items, newItems))
        items = newItems
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            DebugCookieItemViewHolder(parent.viewBinding(DebugCookieItemBinding::inflate))

    override fun onBindViewHolder(holder: DebugCookieItemViewHolder, position: Int) {
        holder.onBind(items[position])
    }

    override fun getItemCount() = items.size

    class DebugCookieItemViewHolder(private val binding: DebugCookieItemBinding) : ViewHolder(binding.root) {
        fun onBind(item: DebugCookieItem) = with(binding) {
            cookieDomain.text = item.domain
            cookieName.text = item.name
            cookieValue.text = item.value

            root.setOnClickListener { item.onClick.click() }
            deleteCookieButton.setOnClickListener { item.onDeleteClick.click() }
        }
    }

    class DebugCookiesDiffCallback(
        private val oldList: List<DebugCookieItem>,
        private val newList: List<DebugCookieItem>
    ) : Callback() {
        override fun getOldListSize() = oldList.size
        override fun getNewListSize() = newList.size
        override fun areItemsTheSame(old: Int, new: Int) = oldList[old].key == newList[new].key
        override fun areContentsTheSame(old: Int, new: Int) = oldList[old] == newList[new]
    }

    data class DebugCookieItem(
        val key: String,
        val domain: String,
        val name: String,
        val value: String?,
        val onClick: ListItemInteraction,
        val onDeleteClick: ListItemInteraction
    )
}
