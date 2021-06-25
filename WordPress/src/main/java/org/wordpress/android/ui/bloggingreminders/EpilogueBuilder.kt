package org.wordpress.android.ui.bloggingreminders

import android.icu.text.ListFormatter
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
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
    fun buildUiItems(
        bloggingRemindersModel: BloggingRemindersModel?
    ): List<BloggingRemindersItem> {
        val enabledDays = bloggingRemindersModel?.enabledDays?.sorted()?.map {
            DayOfWeek.valueOf(it.name).getDisplayName(TextStyle.FULL, Locale.getDefault())
        }

        val title = when (enabledDays?.size) {
            ZERO -> UiStringRes(string.blogging_reminders_epilogue_not_set_title)
            else -> UiStringRes(string.blogging_reminders_epilogue_title)
        }

        val body = when (enabledDays?.size) {
            ZERO -> UiStringRes(string.blogging_reminders_epilogue_body_no_reminders)
            SEVEN_DAYS -> UiStringRes(string.blogging_reminders_epilogue_body_everyday)
            else -> {
                val numberOfTimes = dayLabelUtils.buildLowercaseNTimesLabel(bloggingRemindersModel)

                val selectedDays = if (VERSION.SDK_INT >= VERSION_CODES.O) {
                    ListFormatter.getInstance().format(enabledDays)
                } else {
                    getFormattedDays(enabledDays)
                }

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

    // for android sdk below Oreo(26), it will show only comma separated list e.g. Monday, Tuesday
    private fun getFormattedDays(enabledDays: List<String>?): String {
        return when (enabledDays?.size) {
            ONE_DAY -> enabledDays.joinToString { it }
            in TWO_DAYS..SIX_DAYS -> enabledDays?.joinToString { it }.toString()
            else -> UiStringRes(string.blogging_reminders_epilogue_body_everyday).toString()
        }
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
