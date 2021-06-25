package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.R.drawable
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Caption
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.HighEmphasisText
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Illustration
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersItem.Title
import org.wordpress.android.ui.bloggingreminders.BloggingRemindersViewModel.UiState.PrimaryButton
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import java.time.DayOfWeek
import javax.inject.Inject

class EpilogueBuilder @Inject constructor(
    private val dayLabelUtils: DayLabelUtils
) {
    fun buildUiItems(
        bloggingRemindersModel: BloggingRemindersModel?
    ): List<BloggingRemindersItem> {
        val enabledDays = bloggingRemindersModel?.enabledDays?.sorted()?.map { DayOfWeek.valueOf(it.name) }

        val selectedDays = when (enabledDays?.size) {
            ONE_DAY -> enabledDays.joinToString { it.toString() }
            TWO_DAYS -> enabledDays.joinToString(separator = " and ") {
                it.toString()}
            in THREE_DAYS..SIX_DAYS -> {
                val firstDays = enabledDays?.dropLast(1)
                val lastDay = enabledDays?.drop(enabledDays.count() - 1)
                firstDays?.joinToString(postfix = " and ") {
                    it.toString()
                } + lastDay?.joinToString { it.toString() }
            }
            else -> UiStringRes(string.blogging_reminders_epilogue_body_everyday).toString()
        }

        val title = when (enabledDays?.size) {
            ZERO -> UiStringRes(string.blogging_reminders_epilogue_not_set_title)
            else -> UiStringRes(string.blogging_reminders_epilogue_title)
        }

        val body = when (enabledDays?.size) {
            ZERO -> UiStringRes(string.blogging_reminders_epilogue_body_no_reminders)
            SEVEN_DAYS -> UiStringRes(string.blogging_reminders_epilogue_body_everyday)
            else -> {
                val numberOfTimes = dayLabelUtils.buildNTimesLabel(bloggingRemindersModel)
                // TODO: Undo this when desugaring is merged and remove selectedDay above
                // val selectedDays = ListFormatter.getInstance().format(enabledDays)

                UiStringResWithParams(
                        string.blogging_reminders_epilogue_body_days,
                        listOf(numberOfTimes, UiStringText(selectedDays)))
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

    companion object {
        private const val ZERO = 0
        private const val ONE_DAY = 1
        private const val TWO_DAYS = 2
        private const val THREE_DAYS = 3
        private const val SIX_DAYS = 6
        private const val SEVEN_DAYS = 7
    }
}
