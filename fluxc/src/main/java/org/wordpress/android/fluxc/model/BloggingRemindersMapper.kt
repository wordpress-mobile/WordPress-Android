package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.FRIDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.MONDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SATURDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.SUNDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.THURSDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.TUESDAY
import org.wordpress.android.fluxc.model.BloggingRemindersModel.Day.WEDNESDAY
import org.wordpress.android.fluxc.persistence.BloggingRemindersDao.BloggingReminders
import javax.inject.Inject

class BloggingRemindersMapper
@Inject constructor() {
    fun toDatabaseModel(domainModel: BloggingRemindersModel): BloggingReminders =
        with(domainModel) {
            return BloggingReminders(
                localSiteId = this.siteId,
                monday = enabledDays.contains(MONDAY),
                tuesday = enabledDays.contains(TUESDAY),
                wednesday = enabledDays.contains(WEDNESDAY),
                thursday = enabledDays.contains(THURSDAY),
                friday = enabledDays.contains(FRIDAY),
                saturday = enabledDays.contains(SATURDAY),
                sunday = enabledDays.contains(SUNDAY),
                hour = this.hour,
                minute = this.minute,
                isPromptRemindersOptedIn = domainModel.isPromptIncluded,
                isPromptsCardOptedIn = domainModel.isPromptsCardEnabled,
            )
        }

    fun toDomainModel(databaseModel: BloggingReminders): BloggingRemindersModel =
        with(databaseModel) {
            return BloggingRemindersModel(
                siteId = localSiteId,
                enabledDays = mutableSetOf<Day>().let { list ->
                    if (monday) list.add(MONDAY)
                    if (tuesday) list.add(TUESDAY)
                    if (wednesday) list.add(WEDNESDAY)
                    if (thursday) list.add(THURSDAY)
                    if (friday) list.add(FRIDAY)
                    if (saturday) list.add(SATURDAY)
                    if (sunday) list.add(SUNDAY)
                    list
                },
                hour = hour,
                minute = minute,
                isPromptIncluded = isPromptRemindersOptedIn,
                isPromptsCardEnabled = isPromptsCardOptedIn,
            )
        }
}
