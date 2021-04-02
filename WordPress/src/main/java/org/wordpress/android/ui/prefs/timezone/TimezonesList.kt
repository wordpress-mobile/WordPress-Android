package org.wordpress.android.ui.prefs.timezone

sealed class TimezonesList {
    abstract val label: String

    data class TimezoneHeader(
        override val label: String
    ) : TimezonesList()

    data class TimezoneItem(
        override val label: String,
        val value: String,
        val offset: String = "",
        val time: String = ""
    ) : TimezonesList()
}
