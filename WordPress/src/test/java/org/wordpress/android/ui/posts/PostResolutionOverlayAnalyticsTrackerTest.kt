package org.wordpress.android.ui.posts

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
import org.wordpress.android.ui.posts.PostResolutionOverlayAnalyticsTracker.Companion.PROPERTY_CONFIRM_TYPE
import org.wordpress.android.ui.posts.PostResolutionOverlayAnalyticsTracker.Companion.PROPERTY_SOURCE
import org.wordpress.android.ui.posts.PostResolutionOverlayAnalyticsTracker.Companion.PROPERTY_SOURCE_PAGE
import org.wordpress.android.ui.posts.PostResolutionOverlayAnalyticsTracker.Companion.PROPERTY_SOURCE_POST

@RunWith(MockitoJUnitRunner::class)
class PostResolutionOverlayAnalyticsTrackerTest {
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    lateinit var tracker: PostResolutionOverlayAnalyticsTracker

    private val pageProps = mapOf(PROPERTY_SOURCE to PROPERTY_SOURCE_PAGE)
    private val postProps = mapOf(PROPERTY_SOURCE to PROPERTY_SOURCE_POST)

    @Before
    fun setUp() {
        tracker = PostResolutionOverlayAnalyticsTracker(analyticsTracker)
    }

    @Test
    fun `given page, tracksScreenShown tracks correct event`() {
        tracker.trackShown(PostResolutionType.AUTOSAVE_REVISION_CONFLICT, true)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_SCREEN_SHOWN,
            expectedProps = pageProps,
        )

        tracker.trackShown(PostResolutionType.SYNC_CONFLICT, true)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_CONFLICT_SCREEN_SHOWN,
            expectedProps = pageProps,
        )
    }
    @Test
    fun `given post, tracksScreenShown tracks correct event`() {
        tracker.trackShown(PostResolutionType.AUTOSAVE_REVISION_CONFLICT)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_SCREEN_SHOWN,
            expectedProps = postProps,
        )

        tracker.trackShown(PostResolutionType.SYNC_CONFLICT)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_CONFLICT_SCREEN_SHOWN,
            expectedProps = postProps,
        )
    }

    @Test
    fun `given page, tracksCancel tracks correct event`() {
        tracker.trackCancel(PostResolutionType.AUTOSAVE_REVISION_CONFLICT, true)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CANCEL_TAPPED,
            expectedProps = pageProps,
        )

        tracker.trackCancel(PostResolutionType.SYNC_CONFLICT, true)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_CONFLICT_CANCEL_TAPPED,
            expectedProps = pageProps,
        )
    }

    @Test
    fun `given post, tracksCancel tracks correct event`() {
        tracker.trackCancel(PostResolutionType.AUTOSAVE_REVISION_CONFLICT)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CANCEL_TAPPED,
            expectedProps = postProps,
        )

        tracker.trackCancel(PostResolutionType.SYNC_CONFLICT)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_CONFLICT_CANCEL_TAPPED,
            expectedProps = postProps,
        )
    }

    @Test
    fun `given page, tracksClose tracks correct event`() {
        tracker.trackClose(PostResolutionType.AUTOSAVE_REVISION_CONFLICT, true)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CLOSE_TAPPED,
            expectedProps = pageProps,
        )

        tracker.trackClose(PostResolutionType.SYNC_CONFLICT, true)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_CONFLICT_CLOSE_TAPPED,
            expectedProps = pageProps,
        )
    }

    @Test
    fun `given post, tracksClose tracks correct event`() {
        tracker.trackClose(PostResolutionType.AUTOSAVE_REVISION_CONFLICT)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CLOSE_TAPPED,
            expectedProps = postProps,
        )

        tracker.trackClose(PostResolutionType.SYNC_CONFLICT)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_CONFLICT_CLOSE_TAPPED,
            expectedProps = postProps,
        )
    }


    @Test
    fun `given page, tracksDismiss tracks correct event`() {
        tracker.trackDismissed(PostResolutionType.AUTOSAVE_REVISION_CONFLICT, true)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_DISMISSED,
            expectedProps = pageProps,
        )
        tracker.trackDismissed(PostResolutionType.SYNC_CONFLICT, true)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_CONFLICT_DISMISSED,
            expectedProps = pageProps,
        )
    }

    @Test
    fun `given post, tracksDismiss tracks correct event`() {
        tracker.trackDismissed(PostResolutionType.AUTOSAVE_REVISION_CONFLICT)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_DISMISSED,
            expectedProps = postProps,
        )
        tracker.trackDismissed(PostResolutionType.SYNC_CONFLICT)
        verifyCorrectEventTracking(
            expectedEvent = AnalyticsTracker.Stat.RESOLVE_CONFLICT_DISMISSED,
            expectedProps = postProps,
        )
    }

    @Test
    fun `given page, tracksConfirm tracks correct event and properties`() {
        tracker.trackConfirm(
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT,
            PostResolutionConfirmationType.CONFIRM_OTHER,
            true
        )
        tracker.trackConfirm(
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT,
            PostResolutionConfirmationType.CONFIRM_LOCAL,
            true
        )
        mapCaptor().apply {
            verify(analyticsTracker, times(2)).track(
                eq(AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CONFIRM_TAPPED),
                capture()
            )

            Assertions.assertThat(firstValue).containsEntry(PROPERTY_CONFIRM_TYPE, "remote_version")
            Assertions.assertThat(secondValue).containsEntry(PROPERTY_CONFIRM_TYPE, "local_version")
            Assertions.assertThat(firstValue).containsEntry(PROPERTY_SOURCE, PROPERTY_SOURCE_PAGE)
            Assertions.assertThat(secondValue).containsEntry(PROPERTY_SOURCE, PROPERTY_SOURCE_PAGE)
        }

        tracker.trackConfirm(PostResolutionType.SYNC_CONFLICT, PostResolutionConfirmationType.CONFIRM_OTHER, true)
        tracker.trackConfirm(PostResolutionType.SYNC_CONFLICT, PostResolutionConfirmationType.CONFIRM_LOCAL, true)
        mapCaptor().apply {
            verify(analyticsTracker, times(2)).track(
                eq(AnalyticsTracker.Stat.RESOLVE_CONFLICT_CONFIRM_TAPPED),
                capture()
            )

            Assertions.assertThat(firstValue).containsEntry(PROPERTY_CONFIRM_TYPE, "remote_version")
            Assertions.assertThat(secondValue).containsEntry(PROPERTY_CONFIRM_TYPE, "local_version")
            Assertions.assertThat(firstValue).containsEntry(PROPERTY_SOURCE, PROPERTY_SOURCE_PAGE)
            Assertions.assertThat(secondValue).containsEntry(PROPERTY_SOURCE, PROPERTY_SOURCE_PAGE)
        }
    }

    @Test
    fun `given post, tracksConfirm tracks correct event and properties`() {
        tracker.trackConfirm(
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT,
            PostResolutionConfirmationType.CONFIRM_OTHER
        )
        tracker.trackConfirm(
            PostResolutionType.AUTOSAVE_REVISION_CONFLICT,
            PostResolutionConfirmationType.CONFIRM_LOCAL
        )
        mapCaptor().apply {
            verify(analyticsTracker, times(2)).track(
                eq(AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CONFIRM_TAPPED),
                capture()
            )

            Assertions.assertThat(firstValue).containsEntry(PROPERTY_CONFIRM_TYPE, "remote_version")
            Assertions.assertThat(secondValue).containsEntry(PROPERTY_CONFIRM_TYPE, "local_version")
            Assertions.assertThat(firstValue).containsEntry(PROPERTY_SOURCE, PROPERTY_SOURCE_POST)
            Assertions.assertThat(secondValue).containsEntry(PROPERTY_SOURCE, PROPERTY_SOURCE_POST)
        }

        tracker.trackConfirm(PostResolutionType.SYNC_CONFLICT, PostResolutionConfirmationType.CONFIRM_OTHER)
        tracker.trackConfirm(PostResolutionType.SYNC_CONFLICT, PostResolutionConfirmationType.CONFIRM_LOCAL)
        mapCaptor().apply {
            verify(analyticsTracker, times(2)).track(
                eq(AnalyticsTracker.Stat.RESOLVE_CONFLICT_CONFIRM_TAPPED),
                capture()
            )

            Assertions.assertThat(firstValue).containsEntry(PROPERTY_CONFIRM_TYPE, "remote_version")
            Assertions.assertThat(secondValue).containsEntry(PROPERTY_CONFIRM_TYPE, "local_version")
            Assertions.assertThat(firstValue).containsEntry(PROPERTY_SOURCE, PROPERTY_SOURCE_POST)
            Assertions.assertThat(secondValue).containsEntry(PROPERTY_SOURCE, PROPERTY_SOURCE_POST)
        }
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
