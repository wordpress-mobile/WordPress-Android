package org.wordpress.android.ui.jetpackoverlay

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.main.WPMainNavigationView
import org.wordpress.android.util.BuildConfigWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

@RunWith(MockitoJUnitRunner::class)
class JetpackFeatureRemovalHelperTest {
    @Mock
    private lateinit var buildConfigWrapper: BuildConfigWrapper

    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    private lateinit var helper: JetpackFeatureRemovalHelper

    @Before
    fun setUp() {
        helper = JetpackFeatureRemovalHelper(buildConfigWrapper, analyticsTrackerWrapper)
    }

    // The removal is a build-flavor invariant: the WordPress app never has the Jetpack-powered
    // features and the Jetpack app always does. Nothing about it is decided at runtime.
    @Test
    fun `given the WordPress app, then Jetpack features are removed`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(false)

        assertThat(helper.shouldRemoveJetpackFeatures()).isTrue
        assertThat(helper.shouldShowNotifications()).isFalse
        assertThat(helper.shouldShowJetpackPoweredEditorFeatures()).isFalse
        assertThat(helper.shouldShowTemplateSelectionInPages()).isFalse
        assertThat(helper.shouldShowPublishedPostStatsButton()).isFalse
        assertThat(helper.shouldShowHelpAndSupportOnEditor()).isFalse
        assertThat(helper.getDeepLinkTrackingName()).isEqualTo(JETPACK_DEEPLINK_TRACKING_NAME)
    }

    @Test
    fun `given the Jetpack app, then Jetpack features are kept`() {
        whenever(buildConfigWrapper.isJetpackApp).thenReturn(true)

        assertThat(helper.shouldRemoveJetpackFeatures()).isFalse
        assertThat(helper.shouldShowNotifications()).isTrue
        assertThat(helper.shouldShowJetpackPoweredEditorFeatures()).isTrue
        assertThat(helper.shouldShowTemplateSelectionInPages()).isTrue
        assertThat(helper.shouldShowPublishedPostStatsButton()).isTrue
        assertThat(helper.shouldShowHelpAndSupportOnEditor()).isTrue
        assertThat(helper.getDeepLinkTrackingName()).isNull()
    }

    @Test
    fun `when we track reader accessed event, then the proper event is tracked`() {
        helper.trackPageAccessedEventIfNeeded(WPMainNavigationView.PageType.READER)

        verify(analyticsTrackerWrapper, times(1)).track(AnalyticsTracker.Stat.READER_ACCESSED)
    }

    @Test
    fun `when we track my site accessed event, then the proper event is tracked`() {
        val site = SiteModel()

        helper.trackPageAccessedEventIfNeeded(WPMainNavigationView.PageType.MY_SITE, site)

        verify(analyticsTrackerWrapper, times(1)).track(AnalyticsTracker.Stat.MY_SITE_ACCESSED, site)
    }
}
