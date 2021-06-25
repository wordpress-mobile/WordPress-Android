package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Caption
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.ListFormatterUtils
import org.wordpress.android.util.LocaleManagerWrapper
import java.time.DayOfWeek
import java.time.format.TextStyle
import javax.inject.Inject

class EpilogueBuilder @Inject constructor(
    private val dayLabelUtils: DayLabelUtils,
    private val localeManagerWrapper: LocaleManagerWrapper,
    private val listFormatterUtils: ListFormatterUtils
) {
    fun buildUiItems(
        bloggingRemindersModel: BloggingRemindersModel?
    ): List<BloggingRemindersItem> {
        val enabledDays = bloggingRemindersModel?.enabledDays ?: setOf()

        val title = when {
            enabledDays.isEmpty() -> UiStringRes(string.blogging_reminders_epilogue_not_set_title)
            else -> UiStringRes(string.blogging_reminders_epilogue_title)
        }

        val body = when (enabledDays.size) {
            ZERO -> UiStringRes(string.blogging_reminders_epilogue_body_no_reminders)
            SEVEN_DAYS -> UiStringRes(string.blogging_reminders_epilogue_body_everyday)
            else -> {
                val numberOfTimes = dayLabelUtils.buildLowercaseNTimesLabel(bloggingRemindersModel)

                val selectedDays = if (enabledDays.size == SEVEN_DAYS) {
                    UiStringRes(string.blogging_reminders_epilogue_body_everyday).toString()
                } else {
                    listFormatterUtils.formatList(enabledDays.print())
                }

                UiStringResWithParams(
                        string.blogging_reminders_epilogue_body_days,
                        listOf(numberOfTimes, UiStringText(selectedDays))
                )
            }
        }

        return listOf(
                Illustration(drawable.img_illustration_bell_yellow_96dp),
                Title(title),
                HighEmphasisText(body),
                Caption(UiStringRes(string.blogging_reminders_epilogue_caption))
        )
    }

    fun buildPrimaryButton(
        onDone: () -> Unit
    ): PrimaryButton {
        return PrimaryButton(
                UiStringRes(string.blogging_reminders_done),
                enabled = true,
                ListItemInteraction.create(onDone)
        )
    }

    private fun Set<Day>.print(): List<String> {
        return this.sorted().map {
            DayOfWeek.valueOf(it.name).getDisplayName(TextStyle.FULL, localeManagerWrapper.getLocale())
        }
    }

    companion object {
        private const val ZERO = 0
        private const val SEVEN_DAYS = 7
    }
}
