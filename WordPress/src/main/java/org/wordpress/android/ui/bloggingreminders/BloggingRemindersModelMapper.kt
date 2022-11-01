package org.wordpress.android.ui.bloggingreminders

import org.wordpress.android.fluxc.model.BloggingRemindersModel
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day
import java.time.DayOfWeek
import java.time.DayOfWeek.FRIDAY
import java.time.DayOfWeek.MONDAY
import java.time.DayOfWeek.SATURDAY
import java.time.DayOfWeek.SUNDAY
import java.time.DayOfWeek.THURSDAY
import java.time.DayOfWeek.TUESDAY
import java.time.DayOfWeek.WEDNESDAY
import javax.inject.Inject

class BloggingRemindersModelMapper
@Inject constructor() {
    fun toDomainModel(uiModel: BloggingRemindersUiModel): BloggingRemindersModel {
        return BloggingRemindersModel(
                uiModel.siteId,
                uiModel.enabledDays.map {
                    when (it) {
                        SATURDAY -> Day.SATURDAY
                        MONDAY -> Day.MONDAY
                        TUESDAY -> Day.TUESDAY
                        WEDNESDAY -> Day.WEDNESDAY
                        THURSDAY -> Day.THURSDAY
                        FRIDAY -> Day.FRIDAY
                        SUNDAY -> Day.SUNDAY
                    }
                }.toSet(),
                uiModel.hour,
                uiModel.minute,
                uiModel.isPromptIncluded
        )
    }

    fun toUiModel(domainModel: BloggingRemindersModel): BloggingRemindersUiModel {
        return BloggingRemindersUiModel(
                domainModel.siteId,
                domainModel.enabledDays.map { DayOfWeek.valueOf(it.name) }.toSet(),
                domainModel.hour,
                domainModel.minute,
                domainModel.isPromptIncluded
        )
    }
}
