package org.wordpress.android.ui.bloggingreminders

import android.graphics.Typeface
import android.text.Spannable
import android.text.SpannableString
import android.text.style.StyleSpan
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import org.wordpress.android.databinding.BloggingRemindersCaptionBinding
import org.wordpress.android.databinding.BloggingRemindersDayButtonsBinding
import org.wordpress.android.databinding.BloggingRemindersIllustrationBinding
import org.wordpress.android.databinding.BloggingRemindersPromptSwitchBinding
import org.wordpress.android.databinding.BloggingRemindersTextHighEmphasisBinding
import org.wordpress.android.databinding.BloggingRemindersTextMediumEmphasisBinding
import org.wordpress.android.databinding.BloggingRemindersTimeBinding
import org.wordpress.android.databinding.BloggingRemindersTipBinding
import org.wordpress.android.databinding.BloggingRemindersTitleBinding
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersDiffCallback.DayButtonsPayload
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Caption
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons.DayItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.EmphasizedText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.MediumEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PromptSwitch
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.TimeItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Tip
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.util.extensions.viewBinding

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
            text.drawEmphasizedText(uiHelpers, item.text)
        }
    }

    class MediumEmphasisTextViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
        BloggingRemindersViewHolder<BloggingRemindersTextMediumEmphasisBinding>(
            parentView.viewBinding(
                BloggingRemindersTextMediumEmphasisBinding::inflate
            )
        ) {
        fun onBind(item: MediumEmphasisText) = with(binding) {
            if (item.isInvisible) {
                text.visibility = View.INVISIBLE
            } else {
                if (item.text != null) {
                    text.drawEmphasizedText(uiHelpers, item.text)
                }
                text.visibility = View.VISIBLE
            }
        }
    }

    class CaptionViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
        BloggingRemindersViewHolder<BloggingRemindersCaptionBinding>(
            parentView.viewBinding(BloggingRemindersCaptionBinding::inflate)
        ) {
        fun onBind(item: Caption) = with(binding) {
            uiHelpers.setTextOrHide(text, item.text)
        }
    }

    fun TextView.drawEmphasizedText(uiHelpers: UiHelpers, text: EmphasizedText) {
        val message = text.text
        if (text.emphasizeTextParams && message is UiStringResWithParams) {
            val params = message.params.map { uiHelpers.getTextOfUiString(this.context, it) as String }
            val textOfUiString = uiHelpers.getTextOfUiString(this.context, message)
            val spannable = SpannableString(textOfUiString)
            var index = 0
            for (param in params) {
                val indexOfParam = textOfUiString.indexOf(param, index)
                spannable.setSpan(
                    StyleSpan(Typeface.BOLD),
                    indexOfParam,
                    indexOfParam + param.length,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                index = textOfUiString.indexOf(param)
            }
            this.text = spannable
        } else {
            uiHelpers.setTextOrHide(this, message)
        }
    }

    class DayButtonsViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
        BloggingRemindersViewHolder<BloggingRemindersDayButtonsBinding>(
            parentView.viewBinding(
                BloggingRemindersDayButtonsBinding::inflate
            )
        ) {
        fun onBind(item: DayButtons, payload: DayButtonsPayload?) = with(binding) {
            listOf(
                dayOne,
                dayTwo,
                dayThree,
                dayFour,
                dayFive,
                daySix,
                daySeven
            ).forEachIndexed { index, day ->
                if (payload == null) {
                    day.initDay(item.dayItems[index])
                } else if (payload.changedDays[index]) {
                    day.isSelected = item.dayItems[index].isSelected
                }
            }
        }

        private fun TextView.initDay(dayItem: DayItem) {
            uiHelpers.setTextOrHide(this, dayItem.text)
            setOnClickListener { dayItem.onClick.click() }
            isSelected = dayItem.isSelected
        }
    }

    class TipViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
        BloggingRemindersViewHolder<BloggingRemindersTipBinding>(
            parentView.viewBinding(
                BloggingRemindersTipBinding::inflate
            )
        ) {
        fun onBind(item: Tip) = with(binding) {
            uiHelpers.setTextOrHide(title, item.title)
            uiHelpers.setTextOrHide(message, item.message)
        }
    }

    class TimeViewHolder(parentView: ViewGroup, private val uiHelpers: UiHelpers) :
        BloggingRemindersViewHolder<BloggingRemindersTimeBinding>(
            parentView.viewBinding(BloggingRemindersTimeBinding::inflate)
        ) {
        fun onBind(item: TimeItem) = with(binding) {
            uiHelpers.setTextOrHide(timeButton, item.time)
            timeItem.setOnClickListener { item.onClick.click() }
        }
    }

    class PromptSwitchViewHolder(parentView: ViewGroup) :
        BloggingRemindersViewHolder<BloggingRemindersPromptSwitchBinding>(
            parentView.viewBinding(BloggingRemindersPromptSwitchBinding::inflate)
        ) {
        fun onBind(item: PromptSwitch) = with(binding) {
            includePromptSwitch.isChecked = item.isToggled
            includePromptSwitch.setOnCheckedChangeListener { _, _ -> item.onClick.click() }
            promptHelpButtonContainer.setOnClickListener { item.onHelpClick.click() }
        }
    }
}
