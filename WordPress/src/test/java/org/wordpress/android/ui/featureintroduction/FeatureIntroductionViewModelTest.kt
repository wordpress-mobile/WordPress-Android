package org.wordpress.android.ui.featureintroduction

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper

class FeatureIntroductionViewModelTest {
    private val analyticsTracker: AnalyticsTrackerWrapper = mock()
    private val classToTest: FeatureIntroductionViewModel = FeatureIntroductionViewModel(analyticsTracker)

    private val stat = Stat.BLOGGING_PROMPTS_INTRODUCTION_SCREEN_DISMISSED
    private val properties = emptyMap<String, Any?>()

    @Test
    fun `Should NOT track dismiss analytics event if event is not set`() {
        classToTest.onCloseButtonClick()
        classToTest.onBackButtonClick()
        verify(analyticsTracker, times(0)).track(any(), any<Map<String, Any?>>())
    }

    @Test
    fun `Should track dismiss analytics event on close button click`() {
        classToTest.setDismissAnalyticsEvent(stat, properties)
        classToTest.onCloseButtonClick()
        verify(analyticsTracker).track(stat, properties)
    }

    @Test
    fun `Should track dismiss analytics event on back button click`() {
        classToTest.setDismissAnalyticsEvent(stat, properties)
        classToTest.onBackButtonClick()
        verify(analyticsTracker).track(stat, properties)
    }
}
