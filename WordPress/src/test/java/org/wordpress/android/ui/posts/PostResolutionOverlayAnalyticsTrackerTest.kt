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

@RunWith(MockitoJUnitRunner::class)
class PostResolutionOverlayAnalyticsTrackerTest {
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    lateinit var tracker: PostResolutionOverlayAnalyticsTracker

    @Before
    fun setUp() {
        tracker = PostResolutionOverlayAnalyticsTracker(analyticsTracker)
    }

    @Test
    fun `tracksScreenShown tracks correct event`() {
        tracker.trackShown(PostResolutionType.AUTOSAVE_REVISION_CONFLICT)
        verify(analyticsTracker, times(1)).track(
            eq(AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_SCREEN_SHOWN))
        tracker.trackShown(PostResolutionType.SYNC_CONFLICT)
        verify(analyticsTracker, times(1)).track(
            eq(AnalyticsTracker.Stat.RESOLVE_CONFLICT_SCREEN_SHOWN))
    }

    @Test
    fun `tracksCancel tracks correct event`() {
        tracker.trackCancel(PostResolutionType.AUTOSAVE_REVISION_CONFLICT)
        verify(analyticsTracker, times(1)).track(
            eq(AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CANCEL_TAPPED))
        tracker.trackCancel(PostResolutionType.SYNC_CONFLICT)
        verify(analyticsTracker, times(1)).track(
            eq(AnalyticsTracker.Stat.RESOLVE_CONFLICT_CANCEL_TAPPED))
    }

    @Test
    fun `tracksClose tracks correct event`() {
        tracker.trackClose(PostResolutionType.AUTOSAVE_REVISION_CONFLICT)
        verify(analyticsTracker, times(1)).track(
            eq(AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_CLOSE_TAPPED))
        tracker.trackClose(PostResolutionType.SYNC_CONFLICT)
        verify(analyticsTracker, times(1)).track(
            eq(AnalyticsTracker.Stat.RESOLVE_CONFLICT_CLOSE_TAPPED))
    }

    @Test
    fun `tracksDismiss tracks correct event`() {
        tracker.trackDismissed(PostResolutionType.AUTOSAVE_REVISION_CONFLICT)
        verify(analyticsTracker, times(1)).track(
            eq(AnalyticsTracker.Stat.RESOLVE_AUTOSAVE_CONFLICT_DISMISSED))
        tracker.trackDismissed(PostResolutionType.SYNC_CONFLICT)
        verify(analyticsTracker, times(1)).track(
            eq(AnalyticsTracker.Stat.RESOLVE_CONFLICT_DISMISSED))
    }

    @Test
    fun `tracksConfirm tracks correct event and properties`() {
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
        }
    }

    private fun mapCaptor() = argumentCaptor<Map<String, Any?>>()
}
