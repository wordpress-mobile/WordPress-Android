package org.wordpress.android.fluxc.model

data class BloggingRemindersModel(
    val siteId: Int,
    val enabledDays: Set<Day> = setOf(),
    val hour: Int = 10,
    val minute: Int = 0,
    val isPromptIncluded: Boolean = false,
    val isPromptsCardEnabled: Boolean = true,
) {
    enum class Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }
}
