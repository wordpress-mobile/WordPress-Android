package org.wordpress.android.ui.bloggingreminders

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.databinding.BloggingRemindersCloseButtonBinding
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.CloseButton
import org.wordpress.android.util.viewBinding

sealed class BloggingRemindersViewHolder<T : ViewBinding>(protected val binding: T) :
    RecyclerView.ViewHolder(binding.root) {
    class CloseButtonViewHolder(parentView: ViewGroup) :
        BloggingRemindersViewHolder<BloggingRemindersCloseButtonBinding>(
            parentView.viewBinding(
                BloggingRemindersCloseButtonBinding::inflate
            )
        ) {
        fun onBind(item: CloseButton) = with(binding) {
            closeButton.setOnClickListener { item.listItemInteraction.click() }
        }
    }
}
