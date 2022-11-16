package org.wordpress.android.sharedlogin

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.sharedlogin.SharedLoginAnalyticsTracker.ErrorType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class SharedLoginAnalyticsTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val classToTest = SharedLoginAnalyticsTracker(analyticsTrackerWrapper)

    @Test
    fun `Should track login start correctly`() {
        classToTest.trackLoginStart()
        verify(analyticsTrackerWrapper).track(Stat.SHARED_LOGIN_START)
    }

    @Test
    fun `Should track login success correctly`() {
        classToTest.trackLoginSuccess()
        verify(analyticsTrackerWrapper).track(Stat.SHARED_LOGIN_SUCCESS)
    }

    @Test
    fun `Should track login failed WPNotLoggedInError correctly`() {
        classToTest.trackLoginFailed(ErrorType.WPNotLoggedInError)
        verify(analyticsTrackerWrapper).track(
                Stat.SHARED_LOGIN_FAILED,
                mapOf("error_type" to "wp_not_logged_in_error")
        )
    }

    @Test
    fun `Should track login failed QueryLoginDataError correctly`() {
        classToTest.trackLoginFailed(ErrorType.QueryLoginDataError)
        verify(analyticsTrackerWrapper).track(
                Stat.SHARED_LOGIN_FAILED,
                mapOf("error_type" to "query_login_data_error")
        )
    }
}
