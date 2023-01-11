package org.wordpress.android.ui.bloggingreminders

import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersDiffCallback.DayButtonsPayload
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Caption
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.MediumEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PromptSwitch
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.TimeItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Tip
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.CAPTION
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.DAY_BUTTONS
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.HIGH_EMPHASIS_TEXT
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.ILLUSTRATION
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.LOW_EMPHASIS_TEXT
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.NOTIFICATION_TIME
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.PROMPT_SWITCH
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.TIP
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Type.TITLE
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewHolder.CaptionViewHolder
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewHolder.DayButtonsViewHolder
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewHolder.HighEmphasisTextViewHolder
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewHolder.IllustrationViewHolder
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewHolder.MediumEmphasisTextViewHolder
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewHolder.PromptSwitchViewHolder
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewHolder.TimeViewHolder
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewHolder.TipViewHolder
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewHolder.TitleViewHolder
import org.wordpress.android.ui.utils.UiHelpers
import javax.inject.Inject

class BloggingRemindersAdapter @Inject constructor(private val uiHelpers: UiHelpers) :
    ListAdapter<BloggingRemindersItem, BloggingRemindersViewHolder<*>>(BloggingRemindersDiffCallback) {
    override fun onBindViewHolder(holder: BloggingRemindersViewHolder<*>, position: Int) {
        onBindViewHolder(holder, position, listOf())
    }

    override fun onBindViewHolder(holder: BloggingRemindersViewHolder<*>, position: Int, payloads: List<Any>) {
        val item = getItem(position)
        when (holder) {
            is IllustrationViewHolder -> holder.onBind(item as Illustration)
            is TitleViewHolder -> holder.onBind(item as Title)
            is HighEmphasisTextViewHolder -> holder.onBind(item as HighEmphasisText)
            is MediumEmphasisTextViewHolder -> holder.onBind(item as MediumEmphasisText)
            is CaptionViewHolder -> holder.onBind(item as Caption)
            is DayButtonsViewHolder -> holder.onBind(item as DayButtons, payloads.firstOrNull() as? DayButtonsPayload)
            is TipViewHolder -> holder.onBind(item as Tip)
            is TimeViewHolder -> holder.onBind(item as TimeItem)
            is PromptSwitchViewHolder -> holder.onBind(item as PromptSwitch)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BloggingRemindersViewHolder<*> {
        return when (Type.values()[viewType]) {
            TITLE -> TitleViewHolder(parent, uiHelpers)
            ILLUSTRATION -> IllustrationViewHolder(parent)
            HIGH_EMPHASIS_TEXT -> HighEmphasisTextViewHolder(parent, uiHelpers)
            LOW_EMPHASIS_TEXT -> MediumEmphasisTextViewHolder(parent, uiHelpers)
            CAPTION -> CaptionViewHolder(parent, uiHelpers)
            DAY_BUTTONS -> DayButtonsViewHolder(parent, uiHelpers)
            TIP -> TipViewHolder(parent, uiHelpers)
            NOTIFICATION_TIME -> TimeViewHolder(parent, uiHelpers)
            PROMPT_SWITCH -> PromptSwitchViewHolder(parent)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return getItem(position).type.ordinal
    }
}
