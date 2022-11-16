package org.wordpress.android.userflags

import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.userflags.UserFlagsAnalyticsTracker.ErrorType
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class UserFlagsAnalyticsTrackerTest {
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper = mock()
    private val classToTest = UserFlagsAnalyticsTracker(analyticsTrackerWrapper)

    @Test
    fun `Should track get user flags start correctly`() {
        classToTest.trackStart()
        verify(analyticsTrackerWrapper).track(Stat.USER_FLAGS_START)
    }

    @Test
    fun `Should track get user flags success correctly`() {
        classToTest.trackSuccess()
        verify(analyticsTrackerWrapper).track(Stat.USER_FLAGS_SUCCESS)
    }

    @Test
    fun `Should track failed with NoUserFlagsFoundError correctly`() {
        classToTest.trackFailed(ErrorType.NoUserFlagsFoundError)
        verify(analyticsTrackerWrapper).track(
                Stat.USER_FLAGS_FAILED,
                mapOf("error_type" to "no_user_flags_found_error")
        )
    }

    @Test
    fun `Should track failed with QueryUserFlagsError correctly`() {
        classToTest.trackFailed(ErrorType.QueryUserFlagsError)
        verify(analyticsTrackerWrapper).track(
                Stat.USER_FLAGS_FAILED,
                mapOf("error_type" to "query_user_flags_error")
        )
    }

    @Test
    fun `Should track failed with UpdateUserFlagsError correctly`() {
        classToTest.trackFailed(ErrorType.UpdateUserFlagsError)
        verify(analyticsTrackerWrapper).track(
                Stat.USER_FLAGS_FAILED,
                mapOf("error_type" to "update_user_flags_error")
        )
    }
}
