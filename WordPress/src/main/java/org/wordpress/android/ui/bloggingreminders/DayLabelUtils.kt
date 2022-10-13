package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.R
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ResourceProvider
import java.util.Locale
import javax.inject.Inject

class DayLabelUtils
@Inject constructor(private val resourceProvider: ResourceProvider) {
    fun buildNTimesLabel(bloggingRemindersModel: BloggingRemindersUiModel?): UiString {
        val counts = resourceProvider.getStringArray(R.array.blogging_reminders_count)
        val size = bloggingRemindersModel?.enabledDays?.size ?: 0
        return if (size > 0) {
            UiStringResWithParams(
                    R.string.blogging_reminders_n_a_week,
                    listOf(UiStringText(counts[size - 1]))
            )
        } else {
            UiStringRes(R.string.blogging_reminders_not_set)
        }
    }

    fun buildLowercaseNTimesLabel(bloggingRemindersModel: BloggingRemindersUiModel?): String? {
        val counts = resourceProvider.getStringArray(R.array.blogging_reminders_count).map {
            it.lowercase(Locale.getDefault())
        }
        val size = bloggingRemindersModel?.enabledDays?.size ?: 0
        return counts.getOrNull(size - 1)
    }

    fun buildSiteSettingsLabel(bloggingRemindersModel: BloggingRemindersUiModel?): UiString {
        val counts = resourceProvider.getStringArray(R.array.blogging_reminders_count)
        val size = bloggingRemindersModel?.enabledDays?.size ?: 0
        return if (size > 0) {
            UiStringResWithParams(R.string.blogging_reminders_site_settings_label, listOf(
                    UiStringText(counts[size - 1]),
                    UiStringText(bloggingRemindersModel?.getNotificationTime().toString()))
            )
        } else {
            UiStringRes(R.string.blogging_reminders_not_set)
        }
    }
}
