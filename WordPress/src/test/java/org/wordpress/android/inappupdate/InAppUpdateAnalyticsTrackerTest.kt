package org.wordpress.android.inappupdate

import com.google.android.play.core.install.model.AppUpdateType
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.inappupdate.InAppUpdateAnalyticsTracker.Companion.PROPERTY_UPDATE_TYPE
import org.wordpress.android.inappupdate.InAppUpdateAnalyticsTracker.Companion.UPDATE_TYPE_BLOCKING
import org.wordpress.android.inappupdate.InAppUpdateAnalyticsTracker.Companion.UPDATE_TYPE_FLEXIBLE

@RunWith(MockitoJUnitRunner::class)
class InAppUpdateAnalyticsTrackerTest {
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    lateinit var tracker: InAppUpdateAnalyticsTracker

    private val flexibleProps = mapOf(
        PROPERTY_UPDATE_TYPE to UPDATE_TYPE_FLEXIBLE
    )
    private val blockingProps = mapOf(
        PROPERTY_UPDATE_TYPE to UPDATE_TYPE_BLOCKING
    )
    private val emptyProps = emptyMap<String, String>()

    @Before
    fun setUp() {
        tracker = InAppUpdateAnalyticsTracker(analyticsTracker)
    }

    @Test
    fun `trackUpdateShown tracks flexible update shown`() {
        tracker.trackUpdateShown(AppUpdateType.FLEXIBLE)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.IN_APP_UPDATE_SHOWN,
            expectedProps = flexibleProps
        )
    }

    @Test
    fun `trackUpdateShown tracks immediate update shown`() {
        tracker.trackUpdateShown(AppUpdateType.IMMEDIATE)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.IN_APP_UPDATE_SHOWN,
            expectedProps = blockingProps
        )
    }

    @Test
    fun `trackUpdateShown tracks invalid update shown`() {
        tracker.trackUpdateShown(-1)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.IN_APP_UPDATE_SHOWN,
            expectedProps = emptyProps
        )
    }

    @Test
    fun `trackUpdateAccepted tracks flexible update accepted`() {
        tracker.trackUpdateAccepted(AppUpdateType.FLEXIBLE)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.IN_APP_UPDATE_ACCEPTED,
            expectedProps = flexibleProps
        )
    }

    @Test
    fun `trackUpdateAccepted tracks immediate update accepted`() {
        tracker.trackUpdateAccepted(AppUpdateType.IMMEDIATE)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.IN_APP_UPDATE_ACCEPTED,
            expectedProps = blockingProps
        )
    }

    @Test
    fun `trackUpdateAccepted tracks invalid update accepted`() {
        tracker.trackUpdateAccepted(-1)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.IN_APP_UPDATE_ACCEPTED,
            expectedProps = emptyProps
        )
    }

    @Test
    fun `trackUpdateDismissed tracks flexible update dismissed`() {
        tracker.trackUpdateDismissed(AppUpdateType.FLEXIBLE)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.IN_APP_UPDATE_DISMISSED,
            expectedProps = flexibleProps
        )
    }

    @Test
    fun `trackUpdateDismissed tracks immediate update dismissed`() {
        tracker.trackUpdateDismissed(AppUpdateType.IMMEDIATE)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.IN_APP_UPDATE_DISMISSED,
            expectedProps = blockingProps
        )
    }

    @Test
    fun `trackUpdateDismissed tracks invalid update dismissed`() {
        tracker.trackUpdateDismissed(-1)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.IN_APP_UPDATE_DISMISSED,
            expectedProps = emptyProps
        )
    }

    private fun mapCaptor() = argumentCaptor<Map<String, Any?>>()
    private fun verifyCorrectEventTracking(
        expectedEvent: AnalyticsTracker.Stat,
        expectedProps: Map<String, String>,
        expectedTimes: Int = 1
    ) {
        mapCaptor().apply {
            verify(analyticsTracker, times(expectedTimes)).track(
                eq(expectedEvent),
                capture()
            )
            Assertions.assertThat(firstValue).isEqualTo(expectedProps)
        }
    }
}
