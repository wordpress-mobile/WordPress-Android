package org.wordpress.android.support.logs.model

data class LogDay(
    val date: String, // e.g., "Oct-16"
    val displayDate: String, // e.g., "October 16"
    val logEntries: List<String>,
    val logCount: Int
)
