package org.wordpress.android.ui.reader

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.tracker.ReaderTrackerType
import org.wordpress.android.ui.reader.utils.DateProvider
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper
import java.util.Calendar
import java.util.Date

@RunWith(MockitoJUnitRunner::class)
class ReaderTrackerTest {
    @Mock
    lateinit var dateProvider: DateProvider

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper

    private lateinit var tracker: ReaderTracker

    @Before
    fun setup() {
        tracker = ReaderTracker(
            dateProvider,
            appPrefsWrapper,
            analyticsTrackerWrapper,
            analyticsUtilsWrapper
        )
    }

    @Test
    fun `trackers are setup on setupTrackers`() {
        tracker.setupTrackers()
        val expected = mapOf(
            "time_in_main_reader" to 0,
            "time_in_reader_filtered_list" to 0,
            "time_in_reader_paged_post" to 0,
            "time_in_subfiltered_list" to 0
        )

        assertThat(tracker.getAnalyticsData()).isEqualTo(expected)
    }

    @Test
    fun `trackers accumulates as expected`() {
        tracker.setupTrackers()
        val startPoint = Date()

        whenever(dateProvider.getCurrentDate()).thenReturn(startPoint)

        tracker.start(ReaderTrackerType.MAIN_READER)
        tracker.start(ReaderTrackerType.FILTERED_LIST)
        tracker.start(ReaderTrackerType.PAGED_POST)
        tracker.start(ReaderTrackerType.SUBFILTERED_LIST)

        whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, Int.MAX_VALUE - 1))
        tracker.stop(ReaderTrackerType.MAIN_READER)

        whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, Int.MAX_VALUE - 2))
        tracker.stop(ReaderTrackerType.FILTERED_LIST)

        whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, Int.MAX_VALUE - 3))
        tracker.stop(ReaderTrackerType.PAGED_POST)

        whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, Int.MAX_VALUE - 4))
        tracker.stop(ReaderTrackerType.SUBFILTERED_LIST)

        val expected = mapOf(
            "time_in_main_reader" to Int.MAX_VALUE - 1,
            "time_in_reader_filtered_list" to Int.MAX_VALUE - 2,
            "time_in_reader_paged_post" to Int.MAX_VALUE - 3,
            "time_in_subfiltered_list" to Int.MAX_VALUE - 4
        )
        assertThat(tracker.getAnalyticsData()).isEqualTo(expected)
    }

    @Test
    fun `trackers accumulates as expected in multiple sessions`() {
        tracker.setupTrackers()

        val numRep = 10

        (0 until numRep).forEach {
            val startPoint = Date()

            whenever(dateProvider.getCurrentDate()).thenReturn(startPoint)

            tracker.start(ReaderTrackerType.MAIN_READER)
            tracker.start(ReaderTrackerType.FILTERED_LIST)
            tracker.start(ReaderTrackerType.PAGED_POST)
            tracker.start(ReaderTrackerType.SUBFILTERED_LIST)

            whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, 1))
            tracker.stop(ReaderTrackerType.MAIN_READER)

            whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, 2))
            tracker.stop(ReaderTrackerType.FILTERED_LIST)

            whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, 3))
            tracker.stop(ReaderTrackerType.PAGED_POST)

            whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, 4))
            tracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
        }

        val expected = mapOf(
            "time_in_main_reader" to (1 * numRep),
            "time_in_reader_filtered_list" to (2 * numRep),
            "time_in_reader_paged_post" to (3 * numRep),
            "time_in_subfiltered_list" to (4 * numRep)
        )
        assertThat(tracker.getAnalyticsData()).isEqualTo(expected)
    }

    @Test
    fun `trackers are setup correctly on setupTrackers after multiple sessions`() {
        tracker.setupTrackers()

        val numRep = 10

        (0 until numRep).forEach {
            val startPoint = Date()

            whenever(dateProvider.getCurrentDate()).thenReturn(startPoint)

            tracker.start(ReaderTrackerType.MAIN_READER)
            tracker.start(ReaderTrackerType.FILTERED_LIST)
            tracker.start(ReaderTrackerType.PAGED_POST)
            tracker.start(ReaderTrackerType.SUBFILTERED_LIST)

            whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, 1))
            tracker.stop(ReaderTrackerType.MAIN_READER)

            whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, 2))
            tracker.stop(ReaderTrackerType.FILTERED_LIST)

            whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, 3))
            tracker.stop(ReaderTrackerType.PAGED_POST)

            whenever(dateProvider.getCurrentDate()).thenReturn(addToDate(startPoint, 4))
            tracker.stop(ReaderTrackerType.SUBFILTERED_LIST)
        }

        var expected = mapOf(
            "time_in_main_reader" to (1 * numRep),
            "time_in_reader_filtered_list" to (2 * numRep),
            "time_in_reader_paged_post" to (3 * numRep),
            "time_in_subfiltered_list" to (4 * numRep)
        )
        assertThat(tracker.getAnalyticsData()).isEqualTo(expected)

        expected = mapOf(
            "time_in_main_reader" to 0,
            "time_in_reader_filtered_list" to 0,
            "time_in_reader_paged_post" to 0,
            "time_in_subfiltered_list" to 0
        )

        tracker.setupTrackers()
        assertThat(tracker.getAnalyticsData()).isEqualTo(expected)
    }

    @Test
    fun `tracker is not running if not started`() {
        tracker.setupTrackers()

        assertThat(tracker.isRunning(ReaderTrackerType.MAIN_READER)).isEqualTo(false)
    }

    @Test
    fun `tracker is running after start`() {
        tracker.setupTrackers()
        val startPoint = Date()
        whenever(dateProvider.getCurrentDate()).thenReturn(startPoint)

        tracker.start(ReaderTrackerType.MAIN_READER)

        assertThat(tracker.isRunning(ReaderTrackerType.MAIN_READER)).isEqualTo(true)
    }

    @Test
    fun `tracker is not running after stop`() {
        tracker.setupTrackers()
        val startPoint = Date()
        whenever(dateProvider.getCurrentDate()).thenReturn(startPoint)

        tracker.start(ReaderTrackerType.MAIN_READER)
        tracker.stop(ReaderTrackerType.MAIN_READER)

        assertThat(tracker.isRunning(ReaderTrackerType.MAIN_READER)).isEqualTo(false)
    }

    private fun addToDate(date: Date, seconds: Int): Date {
        val calendar = Calendar.getInstance()
        calendar.time = date
        calendar.add(Calendar.SECOND, seconds)
        return calendar.time
    }
}
