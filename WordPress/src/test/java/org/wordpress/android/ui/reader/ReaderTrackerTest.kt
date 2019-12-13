package org.wordpress.android.ui.reader

import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.ui.reader.utils.ReaderTrackersProvider
import java.util.Calendar
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ReaderTrackerTest {
    @Mock lateinit var dateProvider: DateProvider
    lateinit var readerTrackersProvider: ReaderTrackersProvider

    private lateinit var tracker: ReaderTracker

    @Before
    fun setup() {
        readerTrackersProvider = ReaderTrackersProvider(dateProvider)
        tracker = ReaderTracker(readerTrackersProvider)
    }

    @Test
    fun `trackers are initialized on initTrackers`() {
        tracker.initTrackers()
        val expected = mapOf(
                "time_in_main_reader" to 0,
                "time_in_reader_filtered_list" to 0,
                "time_in_reader_paged_post" to 0
        )

        assertThat(tracker.getAnalyticsData()).isEqualTo(expected)
    }

    @Test
    fun `trackers accumulates as expected`() {
        tracker.initTrackers()
        val startPoint = Date()

        whenever(dateProvider.getCurrentTime()).thenReturn(startPoint)

        tracker.start(ReaderTrackerInfo.ReaderTopLevelList::class.java)
        tracker.start(ReaderTrackerInfo.ReaderFilteredList::class.java)
        tracker.start(ReaderTrackerInfo.ReaderPagedPosts::class.java)

        whenever(dateProvider.getCurrentTime()).thenReturn(addToDate(startPoint, Int.MAX_VALUE - 1))
        tracker.stop(ReaderTrackerInfo.ReaderTopLevelList::class.java)

        whenever(dateProvider.getCurrentTime()).thenReturn(addToDate(startPoint, Int.MAX_VALUE - 2))
        tracker.stop(ReaderTrackerInfo.ReaderFilteredList::class.java)

        whenever(dateProvider.getCurrentTime()).thenReturn(addToDate(startPoint, Int.MAX_VALUE - 3))
        tracker.stop(ReaderTrackerInfo.ReaderPagedPosts::class.java)

        val expected = mapOf(
                "time_in_main_reader" to Int.MAX_VALUE - 1,
                "time_in_reader_filtered_list" to Int.MAX_VALUE - 2,
                "time_in_reader_paged_post" to Int.MAX_VALUE - 3
        )
        assertThat(tracker.getAnalyticsData()).isEqualTo(expected)
    }

    @Test
    fun `trackers accumulates as expected in multiple sessions`() {
        tracker.initTrackers()

        val numRep = 10

        for (i in 0 until numRep) {
            val startPoint = Date()

            whenever(dateProvider.getCurrentTime()).thenReturn(startPoint)

            tracker.start(ReaderTrackerInfo.ReaderTopLevelList::class.java)
            tracker.start(ReaderTrackerInfo.ReaderFilteredList::class.java)
            tracker.start(ReaderTrackerInfo.ReaderPagedPosts::class.java)

            whenever(dateProvider.getCurrentTime()).thenReturn(addToDate(startPoint, 1))
            tracker.stop(ReaderTrackerInfo.ReaderTopLevelList::class.java)

            whenever(dateProvider.getCurrentTime()).thenReturn(addToDate(startPoint, 2))
            tracker.stop(ReaderTrackerInfo.ReaderFilteredList::class.java)

            whenever(dateProvider.getCurrentTime()).thenReturn(addToDate(startPoint, 3))
            tracker.stop(ReaderTrackerInfo.ReaderPagedPosts::class.java)
        }

        val expected = mapOf(
                "time_in_main_reader" to (1 * numRep),
                "time_in_reader_filtered_list" to (2 * numRep),
                "time_in_reader_paged_post" to (3 * numRep)
        )
        assertThat(tracker.getAnalyticsData()).isEqualTo(expected)
    }

    @Test
    fun `trackers resets on initTrackers after multiple sessions`() {
        tracker.initTrackers()

        val numRep = 10

        for (i in 0 until numRep) {
            val startPoint = Date()

            whenever(dateProvider.getCurrentTime()).thenReturn(startPoint)

            tracker.start(ReaderTrackerInfo.ReaderTopLevelList::class.java)
            tracker.start(ReaderTrackerInfo.ReaderFilteredList::class.java)
            tracker.start(ReaderTrackerInfo.ReaderPagedPosts::class.java)

            whenever(dateProvider.getCurrentTime()).thenReturn(addToDate(startPoint, 1))
            tracker.stop(ReaderTrackerInfo.ReaderTopLevelList::class.java)

            whenever(dateProvider.getCurrentTime()).thenReturn(addToDate(startPoint, 2))
            tracker.stop(ReaderTrackerInfo.ReaderFilteredList::class.java)

            whenever(dateProvider.getCurrentTime()).thenReturn(addToDate(startPoint, 3))
            tracker.stop(ReaderTrackerInfo.ReaderPagedPosts::class.java)
        }

        var expected = mapOf(
                "time_in_main_reader" to (1 * numRep),
                "time_in_reader_filtered_list" to (2 * numRep),
                "time_in_reader_paged_post" to (3 * numRep)
        )
        assertThat(tracker.getAnalyticsData()).isEqualTo(expected)

        expected = mapOf(
                "time_in_main_reader" to 0,
                "time_in_reader_filtered_list" to 0,
                "time_in_reader_paged_post" to 0
        )

        tracker.initTrackers()
        assertThat(tracker.getAnalyticsData()).isEqualTo(expected)
    }

    private fun addToDate(date: Date, seconds: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.SECOND, seconds)
        return calendar.time
    }
}
