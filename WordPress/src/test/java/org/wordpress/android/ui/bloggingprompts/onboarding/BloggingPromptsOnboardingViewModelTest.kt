package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.DismissDialog
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.INFORMATION
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.ONBOARDING
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.mysite.SelectedSiteRepository

class BloggingPromptsOnboardingViewModelTest : BaseUnitTest() {
    private val uiStateMapper = BloggingPromptsOnboardingUiStateMapper()
    private val siteStore: SiteStore = mock()
    private val selectedSiteRepository: SelectedSiteRepository = mock()
    private val analyticsTracker: BloggingPromptsOnboardingAnalyticsTracker = mock()
    private val classToTest = BloggingPromptsOnboardingViewModel(
            siteStore, uiStateMapper, selectedSiteRepository, analyticsTracker
    )
    private val actionObserver: Observer<BloggingPromptsOnboardingAction> = mock()

    private val viewStates = mutableListOf<BloggingPromptsOnboardingUiState>()

    @Before
    fun setup() {
        classToTest.action.observeForever(actionObserver)
        classToTest.uiState.observeForever { if (it != null) viewStates.add(it) }
    }

    @Test
    fun `Should provide a Ready UI state when start is called`() = runBlocking {
        classToTest.start(ONBOARDING)
        val startState = viewStates[0]
        assertNotNull(startState)
        assertTrue(startState is Ready)
    }

    // ONBOARDING dialog type actions

    /* ktlint-disable max-line-length */
    @Test
    fun `Should trigger OpenEditor action when primary button is tapped`() = runBlocking {
        classToTest.start(ONBOARDING)

        val startState = viewStates[0]
        (startState as Ready).onPrimaryButtonClick()
        verify(actionObserver).onChanged(OpenEditor(1234))
    }

    @Test
    fun `Should trigger OpenSitePicker if Remind Me is clicked and user has more than 1 site`() = runBlocking {
        classToTest.start(ONBOARDING)
        val selectedSiteModel = SiteModel()
        whenever(siteStore.sitesCount).thenReturn(2)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(selectedSiteModel)

        val startState = viewStates[0]
        (startState as Ready).onPrimaryButtonClick()
        startState.onSecondaryButtonClick()
        verify(actionObserver).onChanged(OpenSitePicker(selectedSiteModel))
    }

    @Test
    fun `Should trigger OpenRemindersIntro if Remind Me is clicked and user has only 1 site`() = runBlocking {
        classToTest.start(ONBOARDING)
        val siteModel = SiteModel().apply { id = 123 }
        whenever(siteStore.sitesCount).thenReturn(1)
        whenever(siteStore.sites).thenReturn(listOf(siteModel))

        val startState = viewStates[0]
        (startState as Ready).onPrimaryButtonClick()
        startState.onSecondaryButtonClick()
        verify(actionObserver).onChanged(OpenRemindersIntro(123))
    }

    @Test
    fun `Should trigger OpenRemindersIntro after site is selected on site picker and onSiteSelected is called`() {
        val selectedSiteLocalId = 123
        classToTest.onSiteSelected(selectedSiteLocalId)
        verify(actionObserver).onChanged(OpenRemindersIntro(selectedSiteLocalId))
    }

    // INFORMATION dialog type actions

    @Test
    fun `Should trigger DismissDialog action when primary button is tapped and dialog type is INFORMATION`() =
            runBlocking {
                classToTest.start(INFORMATION)

                val startState = viewStates[0]
                (startState as Ready).onPrimaryButtonClick()
                verify(actionObserver).onChanged(DismissDialog)
            }

    @Test
    fun `Should track screen shown when start is called with ONBOARDING`() = runBlocking {
        classToTest.start(ONBOARDING)
        verify(analyticsTracker).trackScreenShown()
    }

    @Test
    fun `Should track screen shown when start is called with INFORMATION`() = runBlocking {
        classToTest.start(INFORMATION)
        verify(analyticsTracker).trackScreenShown()
    }

    @Test
    fun `Should track screen dismissed when stop is called`() {
        classToTest.stop()
        verify(analyticsTracker).trackScreenDismissed()
    }

    @Test
    fun `Should track try it now clicked when onPrimaryButtonClick is called with ONBOARDING`() = runBlocking {
        classToTest.start(ONBOARDING)
        val startState = viewStates[0]
        (startState as Ready).onPrimaryButtonClick()
        verify(analyticsTracker).trackTryItNowClicked()
    }

    @Test
    fun `Should track got it clicked when onPrimaryButtonClick is called with INFORMATION`() = runBlocking {
        classToTest.start(INFORMATION)
        val startState = viewStates[0]
        (startState as Ready).onPrimaryButtonClick()
        verify(analyticsTracker).trackGotItClicked()
    }

    @Test
    fun `Should track remind me clicked when onSecondaryButtonClick is called`() = runBlocking {
        classToTest.start(INFORMATION)
        val startState = viewStates[0]
        (startState as Ready).onSecondaryButtonClick()
        verify(analyticsTracker).trackRemindMeClicked()
    }
}
