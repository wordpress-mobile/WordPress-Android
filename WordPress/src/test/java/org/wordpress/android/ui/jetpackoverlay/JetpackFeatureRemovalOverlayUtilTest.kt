package org.wordpress.android.ui.jetpackoverlay

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalOverlayUtil.JetpackFeatureCollectionOverlaySource
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalOverlayUtilTest {
    @Mock
    private lateinit var jetpackFeatureRemovalHelper: JetpackFeatureRemovalHelper

    @Mock
    private lateinit var shownTracker: JetpackFeatureOverlayShownTracker

    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var overlayUtil: JetpackFeatureRemovalOverlayUtil

    @Before
    fun setUp() {
        overlayUtil = JetpackFeatureRemovalOverlayUtil(
            jetpackFeatureRemovalHelper,
            shownTracker,
            analyticsTrackerWrapper
        )
    }

    @Test
    fun `given the Jetpack app, when checking the feature collection overlay, then it is not shown`() {
        whenever(jetpackFeatureRemovalHelper.shouldRemoveJetpackFeatures()).thenReturn(false)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isFalse
    }

    @Test
    fun `given the WordPress app and the overlay was never shown, then it is shown`() {
        whenever(jetpackFeatureRemovalHelper.shouldRemoveJetpackFeatures()).thenReturn(true)
        whenever(shownTracker.getFeatureCollectionOverlayShown()).thenReturn(false)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isTrue
    }

    @Test
    fun `given the WordPress app and the overlay was already shown, then it is not shown again`() {
        whenever(jetpackFeatureRemovalHelper.shouldRemoveJetpackFeatures()).thenReturn(true)
        whenever(shownTracker.getFeatureCollectionOverlayShown()).thenReturn(true)

        assertThat(overlayUtil.shouldShowFeatureCollectionJetpackOverlayForFirstTime()).isFalse
    }

    @Test
    fun `when the overlay is shown on app open, then it is recorded so it does not show again`() {
        overlayUtil.onFeatureCollectionOverlayShown(JetpackFeatureCollectionOverlaySource.APP_OPEN)

        verify(shownTracker).setFeatureCollectionOverlayShown()
    }

    @Test
    fun `when the overlay is opened from the feature card, then it is not recorded as the app-open showing`() {
        overlayUtil.onFeatureCollectionOverlayShown(JetpackFeatureCollectionOverlaySource.FEATURE_CARD)

        verify(shownTracker, never()).setFeatureCollectionOverlayShown()
    }

    @Test
    fun `shouldHideJetpackFeatures follows the removal state`() {
        whenever(jetpackFeatureRemovalHelper.shouldRemoveJetpackFeatures()).thenReturn(true)

        assertThat(overlayUtil.shouldHideJetpackFeatures()).isTrue
    }
}
