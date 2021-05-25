package org.wordpress.android.ui.bloggingreminders

import android.view.ViewGroup
import android.widget.ImageView.ScaleType.CENTER
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.databinding.BloggingRemindersCloseButtonBinding
import org.wordpress.android.databinding.BloggingRemindersIllustrationBinding
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.CloseButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.util.image.ImageManager
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
    class IllustrationViewHolder(parentView: ViewGroup, private val imageManager: ImageManager) :
        BloggingRemindersViewHolder<BloggingRemindersIllustrationBinding>(
            parentView.viewBinding(
                BloggingRemindersIllustrationBinding::inflate
            )
        ) {
        fun onBind(item: Illustration) = with(binding) {
            imageManager.load(illustrationView, item.illustration, CENTER)
        }
    }
}
