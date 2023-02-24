package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.DayButtons.DayItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.EmphasizedText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.MediumEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.PromptSwitch
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.TimeItem
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Tip
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.LocaleManagerWrapper
import java.time.DayOfWeek
import java.time.format.TextStyle.SHORT
import javax.inject.Inject

class DaySelectionBuilder
@Inject constructor(
    private val daysProvider: DaysProvider,
    private val dayLabelUtils: DayLabelUtils,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper,
) {
    fun buildSelection(
        bloggingRemindersModel: BloggingRemindersUiModel?,
        onSelectDay: (DayOfWeek) -> Unit,
        onSelectTime: () -> Unit,
        onPromptSwitchToggled: () -> Unit,
        onPromptHelpButtonClicked: () -> Unit
    ): List<BloggingRemindersItem> {
        val daysOfWeek = daysProvider.getDaysOfWeekByLocale()
        val text = dayLabelUtils.buildNTimesLabel(bloggingRemindersModel)
        val nTimesLabel = MediumEmphasisText(
            EmphasizedText(text),
            bloggingRemindersModel?.enabledDays?.isEmpty() == true
        )
        val selectionList = mutableListOf(
            Illustration(R.drawable.img_illustration_calendar),
            Title(UiStringRes(R.string.blogging_reminders_select_days)),
            MediumEmphasisText(UiStringRes(R.string.blogging_reminders_select_days_message)),
            DayButtons(daysOfWeek.map {
                DayItem(
                    UiStringText(it.getDisplayName(SHORT, localeManagerWrapper.getLocale())),
                    bloggingRemindersModel?.enabledDays?.contains(it) == true,
                    ListItemInteraction.create(it, onSelectDay)
                )
            }),
            nTimesLabel
        )

        if (bloggingRemindersModel?.enabledDays?.isNotEmpty() == true) {
            selectionList.add(
                TimeItem(
                    UiStringText(bloggingRemindersModel.getNotificationTime()),
                    ListItemInteraction.create(onSelectTime)
                )
            )

            if (bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()) {
                selectionList.add(
                    PromptSwitch(
                        bloggingRemindersModel.isPromptIncluded,
                        ListItemInteraction.create(onPromptSwitchToggled),
                        ListItemInteraction.create(onPromptHelpButtonClicked)
                    )
                )
            }
        }

        selectionList.add(
            Tip(UiStringRes(string.blogging_reminders_tip), UiStringRes(string.blogging_reminders_tip_message))
        )

        return selectionList
    }

    fun buildPrimaryButton(
        bloggingRemindersModel: BloggingRemindersUiModel?,
        isFirstTimeFlow: Boolean,
        onConfirm: (BloggingRemindersUiModel?) -> Unit
    ): PrimaryButton {
        val buttonEnabled = if (isFirstTimeFlow) {
            bloggingRemindersModel?.enabledDays?.isNotEmpty() == true
        } else {
            true
        }
        val buttonText = if (isFirstTimeFlow) {
            if (bloggingPromptsSettingsHelper.isPromptsFeatureAvailable()) {
                string.blogging_prompt_set_reminders
            } else {
                string.blogging_reminders_notify_me
            }
        } else {
            R.string.blogging_reminders_update
        }
        return PrimaryButton(
            UiStringRes(buttonText),
            enabled = buttonEnabled,
            ListItemInteraction.create(bloggingRemindersModel, onConfirm)
        )
    }
}
