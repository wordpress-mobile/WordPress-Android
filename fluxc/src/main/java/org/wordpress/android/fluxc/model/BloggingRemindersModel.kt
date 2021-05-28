package org.wordpress.android.fluxc.model

data class BloggingRemindersModel(val siteId: Int, val enabledDays: Set<Day> = setOf()) {
    enum class Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }
}
