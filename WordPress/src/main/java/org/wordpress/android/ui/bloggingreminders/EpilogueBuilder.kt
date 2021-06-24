package org.wordpress.android.ui.bloggingreminders

import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
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
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

class EpilogueBuilder @Inject constructor(
    private val dayLabelUtils: DayLabelUtils
) {
    @RequiresApi(VERSION_CODES.O) // TODO: Remove this annotation once de-sugaring PR is merged
    fun buildUiItems(
        bloggingRemindersModel: BloggingRemindersModel?
    ): List<BloggingRemindersItem> {
        val numberOfTimes = dayLabelUtils.buildNTimesLabel(bloggingRemindersModel)
        val enabledDays = bloggingRemindersModel?.enabledDays?.map { DayOfWeek.valueOf(it.name) }

        val selectedDays = when (enabledDays?.count()) {
            ONE_DAY -> enabledDays.joinToString { formattedDay(it) }
            TWO_DAYS -> enabledDays.joinToString(separator = " and ") { formattedDay(it) }
            in THREE_DAYS..SIX_DAYS -> {
                val firstDays = enabledDays?.dropLast(1)
                val lastDay = enabledDays?.drop(enabledDays.count() - 1)
                firstDays?.joinToString(postfix = " and ") {
                    formattedDay(it)
                } + lastDay?.joinToString { formattedDay(it) }
            }
            else -> UiStringRes(string.blogging_reminders_epilogue_body_everyday).toString()
        }

        val title = when (enabledDays?.count()) {
            ZERO -> UiStringRes(string.blogging_reminders_epilogue_not_set_title)
            else -> UiStringRes(string.blogging_reminders_epilogue_title)
        }

        val body = when (enabledDays?.count()) {
            ZERO -> UiStringRes(string.blogging_reminders_epilogue_body_no_reminders)
            SEVEN_DAYS -> UiStringRes(string.blogging_reminders_epilogue_body_everyday)
            else -> UiStringResWithParams(
                    string.blogging_reminders_epilogue_body_days,
                    listOf(numberOfTimes, UiStringText(selectedDays)))
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

    @RequiresApi(VERSION_CODES.O)
    private fun formattedDay(it: DayOfWeek) = it.getDisplayName(TextStyle.FULL, Locale.getDefault())

    companion object {
        private const val ZERO = 0
        private const val ONE_DAY = 1
        private const val TWO_DAYS = 2
        private const val THREE_DAYS = 3
        private const val SIX_DAYS = 6
        private const val SEVEN_DAYS = 7
    }
}
