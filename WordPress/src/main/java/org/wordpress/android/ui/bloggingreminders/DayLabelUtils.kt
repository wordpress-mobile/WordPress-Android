package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.R
import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringResWithParams
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.viewmodel.ResourceProvider
import javax.inject.Inject

class DayLabelUtils
@Inject constructor(private val resourceProvider: ResourceProvider) {
    fun buildNTimesLabel(bloggingRemindersModel: BloggingRemindersModel?): UiString {
        val counts = resourceProvider.getStringArray(R.array.blogging_goals_count)
        val size = bloggingRemindersModel?.enabledDays?.size ?: 0
        return if (size > 0) {
            UiStringResWithParams(
                    R.string.blogging_goals_n_a_week,
                    listOf(UiStringText(counts[size - 1]))
            )
        } else {
            UiStringRes(R.string.blogging_goals_not_set)
        }
    }
}
