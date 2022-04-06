package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.test
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker

class BloggingPromptsOnboardingViewModelTest : BaseUnitTest() {
    private val uiStateMapper: BloggingPromptsOnboardingUiStateMapper = mock()
    private val siteStore: SiteStore = mock()
    private val classToTest = BloggingPromptsOnboardingViewModel(siteStore, uiStateMapper)
    private val actionObserver: Observer<BloggingPromptsOnboardingAction> = mock()

    @Before
    fun setup() {
        classToTest.action.observeForever(actionObserver)
    }

    @Test
    fun `Should trigger Ready state when start is called`() {
        classToTest.start()
        verify(uiStateMapper).mapReady()
    }

    @Test
    fun `Should trigger OpenEditor action when onTryNow is called`() {
        classToTest.start()
        classToTest.onTryNowClick()
        verify(actionObserver).onChanged(OpenEditor)
    }

    @Test
    fun `Should trigger OpenSitePicker if Remind Me is clicked and user has more than 1 site`() = test {
        whenever(siteStore.sitesCount).thenReturn(2)
        classToTest.onRemindMeClick()
        verify(actionObserver).onChanged(OpenSitePicker)
    }

    @Test
    fun `Should trigger OpenRemindersIntro if Remind Me is clicked and user has only 1 site`() = test {
        whenever(siteStore.sitesCount).thenReturn(1)
        classToTest.onRemindMeClick()
        verify(actionObserver).onChanged(OpenRemindersIntro)
    }
}
