package org.wordpress.android.ui.reader.tracker

import java.util.Date

data class ReaderTrackerInfo(
    val startDate: Date? = null,
    val accumulatedTime: Int = 0
)

enum class ReaderTrackerType constructor(val propertyName: String) {
    MAIN_READER("time_in_main_reader"),
    FILTERED_LIST("time_in_reader_filtered_list"),
    PAGED_POST("time_in_reader_paged_post"),
    SUBFILTERED_LIST("time_in_subfiltered_list")
}
