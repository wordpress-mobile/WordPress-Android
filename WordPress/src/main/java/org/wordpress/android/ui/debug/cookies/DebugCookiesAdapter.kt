package org.wordpress.android.ui.debug.cookies

import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil.ItemCallback
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import org.wordpress.android.databinding.DebugCookieItemBinding
import org.wordpress.android.ui.debug.cookies.DebugCookiesAdapter.DebugCookieItem
import org.wordpress.android.ui.debug.cookies.DebugCookiesAdapter.DebugCookieItemViewHolder
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.util.extensions.viewBinding

class DebugCookiesAdapter : ListAdapter<DebugCookieItem, DebugCookieItemViewHolder>(DebugCookiesDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        DebugCookieItemViewHolder(parent.viewBinding(DebugCookieItemBinding::inflate))

    override fun onBindViewHolder(holder: DebugCookieItemViewHolder, position: Int) {
        holder.onBind(getItem(position))
    }

    class DebugCookieItemViewHolder(private val binding: DebugCookieItemBinding) : ViewHolder(binding.root) {
        fun onBind(item: DebugCookieItem) = with(binding) {
            cookieHost.text = item.host
            cookieName.text = item.name
            cookieValue.text = item.value

            root.setOnClickListener { item.onClick.click() }
            deleteCookieButton.setOnClickListener { item.onDeleteClick.click() }
        }
    }

    class DebugCookiesDiffCallback : ItemCallback<DebugCookieItem>() {
        override fun areItemsTheSame(oldItem: DebugCookieItem, newItem: DebugCookieItem) = oldItem.key == newItem.key
        override fun areContentsTheSame(oldItem: DebugCookieItem, newItem: DebugCookieItem) = oldItem == newItem
    }

    data class DebugCookieItem(
        val key: String,
        val host: String,
        val name: String,
        val value: String?,
        val onClick: ListItemInteraction,
        val onDeleteClick: ListItemInteraction
    )
}
