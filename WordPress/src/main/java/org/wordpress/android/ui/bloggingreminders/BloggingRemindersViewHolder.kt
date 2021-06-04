package org.wordpress.android.ui.bloggingreminders

import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.databinding.BloggingRemindersDayButtonsBinding
import org.wordpress.android.databinding.BloggingRemindersIllustrationBinding
import org.wordpress.android.databinding.BloggingRemindersPrimaryButtonBinding
import org.wordpress.android.databinding.BloggingRemindersTextHighEmphasisBinding
import org.wordpress.android.databinding.BloggingRemindersTextMediumEmphasisBinding
import org.wordpress.android.databinding.BloggingRemindersTitleBinding
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons.DayItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PrimaryButton
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.MediumEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.viewBinding

sealed class BloggingRemindersViewHolder<T : ViewBinding>(protected val binding: T) :
        RecyclerView.ViewHolder(binding.root) {
    class IllustrationViewHolder(parentView: ViewGroup) :
            BloggingRemindersViewHolder<BloggingRemindersIllustrationBinding>(
                    parentView.viewBinding(
                            BloggingRemindersIllustrationBinding::inflate
                    )
            ) {
        fun onBind(item: Illustration) = with(binding) {
            illustrationView.setImageResource(item.illustration)
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

    class HighEmphasisTextViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
            BloggingRemindersViewHolder<BloggingRemindersTextHighEmphasisBinding>(
                    parentView.viewBinding(
                            BloggingRemindersTextHighEmphasisBinding::inflate
                    )
            ) {
        fun onBind(item: HighEmphasisText) = with(binding) {
            uiHelpers.setTextOrHide(text, item.text)
        }
    }

    class MediumEmphasisTextViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
            BloggingRemindersViewHolder<BloggingRemindersTextMediumEmphasisBinding>(
                    parentView.viewBinding(
                            BloggingRemindersTextMediumEmphasisBinding::inflate
                    )
            ) {
        fun onBind(item: MediumEmphasisText) = with(binding) {
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

    class DayButtonsViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
            BloggingRemindersViewHolder<BloggingRemindersDayButtonsBinding>(
                    parentView.viewBinding(
                            BloggingRemindersDayButtonsBinding::inflate
                    )
            ) {
        fun onBind(item: DayButtons) = with(binding) {
            listOf(
                    dayOne,
                    dayTwo,
                    dayThree,
                    dayFour,
                    dayFive,
                    daySix,
                    daySeven
            ).forEachIndexed { index, day -> day.initDay(item.dayItems[index]) }
        }

        private fun TextView.initDay(dayItem: DayItem) {
            uiHelpers.setTextOrHide(this, dayItem.text)
            setOnClickListener { dayItem.onClick.click() }
            isSelected = dayItem.isSelected
        }
    }
}
