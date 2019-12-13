package org.wordpress.android.ui.reader

import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

sealed class ReaderTrackerInfo(val key: String, var accumulatedTime: Int, private val dateProvider: DateProvider) {
    private var startTime: Date? = null

    fun init() {
        accumulatedTime = 0
        startTime = null
    }

    fun start() {
        startTime = dateProvider.getCurrentTime()
    }

    fun stop() {
        startTime?.let {
            accumulatedTime += DateTimeUtils.secondsBetween(dateProvider.getCurrentTime(), startTime)
        }
    }

    class ReaderTopLevelList(dateProvider: DateProvider = DateProvider()) : ReaderTrackerInfo("time_in_main_reader", 0, dateProvider)
    class ReaderFilteredList(dateProvider: DateProvider = DateProvider()) : ReaderTrackerInfo("time_in_reader_filtered_list", 0, dateProvider)
    class ReaderPagedPosts(dateProvider: DateProvider = DateProvider()) : ReaderTrackerInfo("time_in_reader_paged_post", 0, dateProvider)
}
