package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.test
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import org.wordpress.android.ui.mysite.SelectedSiteRepository

class BloggingPromptsOnboardingViewModelTest : BaseUnitTest() {
    private val uiStateMapper: BloggingPromptsOnboardingUiStateMapper = mock()
    private val siteStore: SiteStore = mock()
    private val selectedSiteRepository: SelectedSiteRepository = mock()
    private val classToTest = BloggingPromptsOnboardingViewModel(siteStore, uiStateMapper, selectedSiteRepository)
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
        classToTest.onPrimaryButtonClick()
        verify(actionObserver).onChanged(OpenEditor)
    }

    @Test
    fun `Should trigger OpenSitePicker if Remind Me is clicked and user has more than 1 site`() = test {
        val selectedSiteModel = SiteModel()
        whenever(siteStore.sitesCount).thenReturn(2)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(selectedSiteModel)
        classToTest.onSecondaryButtonClick()
        verify(actionObserver).onChanged(OpenSitePicker(selectedSiteModel))
    }

    @Test
    fun `Should trigger OpenRemindersIntro if Remind Me is clicked and user has only 1 site`() = test {
        val siteModel = SiteModel().apply { id = 123 }
        whenever(siteStore.sitesCount).thenReturn(1)
        whenever(siteStore.sites).thenReturn(listOf(siteModel))
        classToTest.onSecondaryButtonClick()
        verify(actionObserver).onChanged(OpenRemindersIntro(123))
    }

    @Test
    fun `Should trigger OpenRemindersIntro after site is selected on site picker and onSiteSelected is called`() {
        val selectedSiteLocalId = 123
        classToTest.onSiteSelected(selectedSiteLocalId)
        verify(actionObserver).onChanged(OpenRemindersIntro(selectedSiteLocalId))
    }
}
