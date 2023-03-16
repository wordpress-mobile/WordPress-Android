package org.wordpress.android.ui.jpfullplugininstall.install

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.ui.jpfullplugininstall.install.JetpackFullPluginInstallAnalyticsTracker.Status
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class JetpackFullPluginInstallAnalyticsTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val classToTest = JetpackFullPluginInstallAnalyticsTracker(
        analyticsTracker = analyticsTrackerWrapper,
    )

    @Test
    fun `Should track screen status when trackScreenShown is called`() {
        classToTest.trackScreenShown(Status.Initial)
        verify(analyticsTrackerWrapper)
            .track(Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_VIEWED, mapOf("status" to "initial"))
    }

    @Test
    fun `Should track cancel button clicked when trackCancelButtonClicked is called`() {
        classToTest.trackCancelButtonClicked(Status.Loading)
        verify(analyticsTrackerWrapper)
            .track(Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_CANCEL_TAPPED, mapOf("status" to "loading"))
    }

    @Test
    fun `Should track install button clicked when trackInstallButtonClicked is called`() {
        classToTest.trackInstallButtonClicked()
        verify(analyticsTrackerWrapper)
            .track(Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_INSTALL_TAPPED, emptyMap())
    }

    @Test
    fun `Should track retry button clicked when trackRetryButtonClicked is called`() {
        classToTest.trackRetryButtonClicked()
        verify(analyticsTrackerWrapper)
            .track(Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_RETRY_TAPPED, emptyMap())
    }

    @Test
    fun `Should track installation success when trackJetpackInstallationSuccess is called`() {
        classToTest.trackJetpackInstallationSuccess()
        verify(analyticsTrackerWrapper)
            .track(Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_SUCCESS, emptyMap())
    }

    @Test
    fun `Should track done button clicked when trackDoneButtonClicked is called`() {
        classToTest.trackDoneButtonClicked()
        verify(analyticsTrackerWrapper)
            .track(Stat.JETPACK_INSTALL_FULL_PLUGIN_FLOW_DONE_TAPPED, emptyMap())
    }
}
