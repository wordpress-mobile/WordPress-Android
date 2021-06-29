package org.wordpress.android.ui.bloggingreminders

import android.view.ViewGroup
import android.widget.ImageView.ScaleType.CENTER
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.databinding.BloggingRemindersIllustrationBinding
import org.wordpress.android.databinding.BloggingRemindersPrimaryButtonBinding
import org.wordpress.android.databinding.BloggingRemindersTextBinding
import org.wordpress.android.databinding.BloggingRemindersTitleBinding
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PrimaryButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Text
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager
import org.wordpress.android.util.viewBinding

sealed class BloggingRemindersViewHolder<T : ViewBinding>(protected val binding: T) :
        RecyclerView.ViewHolder(binding.root) {
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

    class TitleViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
            BloggingRemindersViewHolder<BloggingRemindersTitleBinding>(
                    parentView.viewBinding(
                            BloggingRemindersTitleBinding::inflate
                    )
            ) {
        fun onBind(item: Title) = with(binding) {
            uiHelpers.setTextOrHide(title, item.text)
        }
    }

    class TextViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
            BloggingRemindersViewHolder<BloggingRemindersTextBinding>(
                    parentView.viewBinding(
                            BloggingRemindersTextBinding::inflate
                    )
            ) {
        fun onBind(item: Text) = with(binding) {
            uiHelpers.setTextOrHide(text, item.text)
        }
    }

    class PrimaryButtonViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
            BloggingRemindersViewHolder<BloggingRemindersPrimaryButtonBinding>(
                    parentView.viewBinding(
                            BloggingRemindersPrimaryButtonBinding::inflate
                    )
            ) {
        fun onBind(item: PrimaryButton) = with(binding) {
            uiHelpers.setTextOrHide(primaryButton, item.text)
            primaryButton.setOnClickListener {
                item.onClick.click()
            }
            primaryButton.isEnabled = item.enabled
        }
    }
}
