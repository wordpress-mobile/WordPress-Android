package org.wordpress.android.ui.bloggingprompts.onboarding

import androidx.lifecycle.Observer
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.Assert.assertNotNull
import junit.framework.Assert.assertTrue
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore.BloggingPromptsResult
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.DismissDialog
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenEditor
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenRemindersIntro
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingAction.OpenSitePicker
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.INFORMATION
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingDialogFragment.DialogType.ONBOARDING
import org.wordpress.android.ui.bloggingprompts.onboarding.BloggingPromptsOnboardingUiState.Ready
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import java.util.Date

@InternalCoroutinesApi
class BloggingPromptsOnboardingViewModelTest : BaseUnitTest() {
    private val uiStateMapper = BloggingPromptsOnboardingUiStateMapper()
    private val siteStore: SiteStore = mock()
    private val selectedSiteRepository: SelectedSiteRepository = mock()
//    private val bloggingPromptsStore: BloggingPromptsStore = mock()

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

    private val bloggingPromptsStore: BloggingPromptsStore = mock {
        onBlocking { getPromptForDate(any(), any()) } doReturn flowOf(bloggingPrompt)
    }

    private lateinit var classToTest: BloggingPromptsOnboardingViewModel
    private val actionObserver: Observer<BloggingPromptsOnboardingAction> = mock()

    private val viewStates = mutableListOf<BloggingPromptsOnboardingUiState>()

    @Before
    fun setup() {
        classToTest = BloggingPromptsOnboardingViewModel(
                siteStore,
                uiStateMapper,
                selectedSiteRepository,
                bloggingPromptsStore,
                TEST_DISPATCHER
        )

        classToTest.action.observeForever(actionObserver)
        classToTest.uiState.observeForever { if (it != null) viewStates.add(it) }
        whenever(bloggingPromptsStore.getPromptForDate(any(), any())).thenReturn(flowOf(bloggingPrompt))
    }

    @Test
    fun `Should provide a Ready UI state when start is called`() = runBlocking {
        classToTest.start(ONBOARDING)
        val startState = viewStates[0]
        assertNotNull(startState)
        assertTrue(startState is Ready)
    }

    // ONBOARDING dialog type actions

    @Test
    fun `Should trigger OpenEditor action when primary button is tapped`() = runBlocking {
        classToTest.start(ONBOARDING)

        val startState = viewStates[0]
        (startState as Ready).onPrimaryButtonClick()

        verify(bloggingPromptsStore, times(1)).getPromptForDate(any(), any())
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
}
