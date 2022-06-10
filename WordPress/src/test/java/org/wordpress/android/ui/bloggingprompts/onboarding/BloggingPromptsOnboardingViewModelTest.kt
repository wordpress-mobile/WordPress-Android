package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.argumentCaptor
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.assertj.core.api.Assertions
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.DismissDialog
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.DoNothing
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.INFORMATION
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.ONBOARDING
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import java.util.Date

@InternalCoroutinesApi
class BloggingPromptsOnboardingViewModelTest : BaseUnitTest() {
    private val uiStateMapper = BloggingPromptsOnboardingUiStateMapper()
    private val siteStore: SiteStore = mock()
    private val selectedSiteRepository: SelectedSiteRepository = mock()
    private val bloggingPromptsStore: BloggingPromptsStore = mock()
    private val analyticsTracker: BloggingPromptsOnboardingAnalyticsTracker = mock()
    private val appPrefsWrapper: AppPrefsWrapper = mock()

    private val bloggingPrompt = BloggingPromptsResult(
            model = BloggingPromptModel(
                    id = 123,
                    text = "title",
                    title = "",
                    content = "content",
                    date = Date(),
                    isAnswered = false,
                    attribution = "",
                    respondentsCount = 5,
                    respondentsAvatarUrls = listOf()
            )
    )

    private var classToTest = BloggingPromptsOnboardingViewModel(
            siteStore,
            uiStateMapper,
            selectedSiteRepository,
            bloggingPromptsStore,
            analyticsTracker,
            appPrefsWrapper,
            TEST_DISPATCHER
    )
    private val actionObserver: Observer<BloggingPromptsOnboardingAction> = mock()
    private val snackbarObserver: Observer<Event<SnackbarMessageHolder>> = mock()

    private val viewStates = mutableListOf<BloggingPromptsOnboardingUiState>()

    @Before
    fun setup() {
        classToTest.action.observeForever(actionObserver)
        classToTest.uiState.observeForever { if (it != null) viewStates.add(it) }
        classToTest.snackBarMessage.observeForever(snackbarObserver)
        whenever(bloggingPromptsStore.getPromptForDate(any(), any())).thenReturn(flowOf(bloggingPrompt))
    }

    @Test
    fun `Should provide a Ready UI state when start is called`() = runBlocking {
        classToTest.start(ONBOARDING)
        val startState = viewStates[0]
        assertNotNull(startState)
        assertTrue(startState is Ready)
    }

    @Test
    fun `Should mark dialog as displayed when start is called`() = runBlocking {
        classToTest.start(ONBOARDING)
        verify(appPrefsWrapper).markBloggingPromptOnboardingDialogAsDisplayed()
    }

    // ONBOARDING dialog type actions

    @Test
    fun `Should trigger OpenEditor action when primary button is tapped`() = runBlocking {
        val selectedSiteModel = SiteModel()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(selectedSiteModel)
        classToTest.start(ONBOARDING)

        val startState = viewStates[0]
        (startState as Ready).onPrimaryButtonClick()

        verify(bloggingPromptsStore, times(1)).getPromptForDate(eq(selectedSiteModel), any())
        verify(actionObserver).onChanged(OpenEditor(123))
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

    @Test
    fun `Should show snackbar if blogging prompt is not available`() {
        whenever(bloggingPromptsStore.getPromptForDate(any(), any())).thenReturn(flowOf(BloggingPromptsResult(null)))

        val selectedSiteModel = SiteModel()
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(selectedSiteModel)
        classToTest.start(ONBOARDING)

        val startState = viewStates[0]
        (startState as Ready).onPrimaryButtonClick()

        verify(bloggingPromptsStore, times(1)).getPromptForDate(eq(selectedSiteModel), any())
        verify(actionObserver).onChanged(DoNothing)

        val captor = argumentCaptor<Event<SnackbarMessageHolder>>()
        verify(snackbarObserver).onChanged(captor.capture())
        val message = captor.firstValue.getContentIfNotHandled()?.message as? UiStringRes
        Assertions.assertThat(message?.stringRes).isEqualTo(R.string.blogging_prompts_onboarding_prompts_loading)
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
    fun `Should track screen shown only the first time start is called with ONBOARDING`() = runBlocking {
        classToTest.start(ONBOARDING)
        classToTest.start(ONBOARDING)
        verify(analyticsTracker).trackScreenShown()
    }

    @Test
    fun `Should track screen shown only the first time start is called with INFORMATION`() = runBlocking {
        classToTest.start(INFORMATION)
        classToTest.start(INFORMATION)
        verify(analyticsTracker).trackScreenShown()
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
