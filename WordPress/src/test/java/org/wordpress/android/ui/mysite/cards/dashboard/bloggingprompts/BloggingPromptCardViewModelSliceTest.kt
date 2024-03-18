package org.wordpress.android.ui.mysite.cards.dashboard.bloggingprompts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.clearInvocations
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.notNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import org.wordpress.android.fluxc.store.bloggingprompts.BloggingPromptsStore
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsPostTagProvider
import org.wordpress.android.ui.bloggingprompts.BloggingPromptsSettingsHelper
import org.wordpress.android.ui.mysite.BloggingPromptCardNavigationAction
import org.wordpress.android.ui.mysite.BloggingPromptsCardTrackHelper
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.mysite.SiteNavigationAction
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.prefs.AppPrefsWrapper
import org.wordpress.android.ui.utils.UiString

@Suppress("LargeClass")
@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BloggingPromptCardViewModelSliceTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var appPrefsWrapper: AppPrefsWrapper

    @Mock
    lateinit var bloggingPromptsCardAnalyticsTracker: BloggingPromptsCardAnalyticsTracker

    @Mock
    lateinit var bloggingPromptsSettingsHelper: BloggingPromptsSettingsHelper

    @Mock
    lateinit var bloggingPromptsCardTrackHelper: BloggingPromptsCardTrackHelper

    @Mock
    lateinit var bloggingPromptsPostTagProvider: BloggingPromptsPostTagProvider

    @Mock
    lateinit var bloggingPromptCardBuilder: BloggingPromptCardBuilder

    @Mock
    lateinit var promptsStore: BloggingPromptsStore

    private lateinit var viewModelSlice: BloggingPromptCardViewModelSlice

    private lateinit var navigationActions: MutableList<SiteNavigationAction>

    private lateinit var snackbars: MutableList<SnackbarMessageHolder>

    private val site: SiteModel = SiteModel().apply {
        id = 1
        siteId = 1L
        url = "http://site.com"
        name = "Site"
        iconUrl = "http://site.com/icon.jpg"
    }

    @Before
    fun setup() {
        viewModelSlice = BloggingPromptCardViewModelSlice(
            testDispatcher(),
            selectedSiteRepository,
            appPrefsWrapper,
            bloggingPromptsCardAnalyticsTracker,
            bloggingPromptsSettingsHelper,
            bloggingPromptsCardTrackHelper,
            bloggingPromptsPostTagProvider,
            bloggingPromptCardBuilder,
            promptsStore
        )

        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)

        navigationActions = mutableListOf()
        viewModelSlice.onNavigation.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                navigationActions.add(it)
            }
        }

        snackbars = mutableListOf()
        viewModelSlice.onSnackbarMessage.observeForever { event ->
            event?.getContentIfNotHandled()?.let {
                snackbars.add(it)
            }
        }

        viewModelSlice.initialize(testScope())
    }

    @Test
    fun `given blogging prompt card, when share button is clicked, share action is called`() = test {
        val params = viewModelSlice.getBuilderParams(mock())
        val expectedShareMessage = "Test prompt"

        params.onShareClick(expectedShareMessage)

        assertThat(navigationActions).containsOnly(BloggingPromptCardNavigationAction.SharePrompt(expectedShareMessage))
    }

    @Test
    fun `given blogging prompt card, when answer button is clicked, answer action is called`() = test {
        val attribution = "attribution"
        val mockPromptModel = mock<BloggingPromptModel>()
        whenever(mockPromptModel.attribution).thenReturn(attribution)
        val params = viewModelSlice.getBuilderParams(mockPromptModel)

        params.onAnswerClick(123)

        verify(bloggingPromptsCardAnalyticsTracker).trackMySiteCardAnswerPromptClicked(attribution)
        assertThat(navigationActions).containsOnly(BloggingPromptCardNavigationAction.AnswerPrompt(site, 123))
    }

    @Test
    fun `given blogging prompt card, when view more button is clicked, view more action is called`() = test {
        val params = viewModelSlice.getBuilderParams(mock())

        params.onViewMoreClick()

        assertThat(navigationActions).containsOnly(BloggingPromptCardNavigationAction.ViewMore)
    }

    @Test
    fun `given blogging prompt card, when view answers is clicked, view more action is called`() = test {
        val tagUrl = "valid-url"

        val expectedTag = mock<ReaderTag>()
        whenever(bloggingPromptsPostTagProvider.promptSearchReaderTag(tagUrl)).thenReturn(expectedTag)

        val params = viewModelSlice.getBuilderParams(mock())
        params.onViewAnswersClick(tagUrl)

        assertThat(navigationActions).containsOnly(BloggingPromptCardNavigationAction.ViewAnswers(expectedTag))
        verify(bloggingPromptsCardAnalyticsTracker).trackMySiteCardViewAnswersClicked()
    }


    @Test
    fun `given blogging prompt card, when skip button is clicked, prompt is skipped and undo snackbar displayed`() =
        test {
            val params = viewModelSlice.getBuilderParams(mock())

            params.onSkipClick()

            verify(appPrefsWrapper).setSkippedPromptDay(notNull(), any())

            assertThat(snackbars.size).isEqualTo(1)

            val expectedSnackbar = snackbars.first()
            assertThat(expectedSnackbar.buttonTitle).isEqualTo(UiString.UiStringRes(R.string.undo))
            assertThat(expectedSnackbar.message).isEqualTo(
                UiString.UiStringRes(R.string.my_site_blogging_prompt_card_skipped_snackbar)
            )
            assertThat(expectedSnackbar.isImportant).isEqualTo(true)
        }

    @Test
    fun `given skip undo snackbar, when undo is clicked, then undo skip action and refresh prompt`() =
        test {
            val params = viewModelSlice.getBuilderParams(mock())

            params.onSkipClick()

            clearInvocations(appPrefsWrapper)

            // click undo action
            val snackbar = snackbars.first()
            snackbar.buttonAction.invoke()

            verify(appPrefsWrapper).setSkippedPromptDay(eq(null), any())
        }

    @Test
    fun `given skip undo snackbar, when undo is clicked, then it tracks undo event`() =
        test {
            val params = viewModelSlice.getBuilderParams(mock())

            params.onSkipClick()

            clearInvocations(appPrefsWrapper)

            // click undo action
            val snackbar = snackbars.first()
            snackbar.buttonAction.invoke()

            verify(bloggingPromptsCardAnalyticsTracker).trackMySiteCardSkipThisPromptUndoClicked()
        }

    @Test
    fun `given blogging prompt card, when remove button is clicked, prompt is removed and notifies card was removed`() =
        test {
            val params = viewModelSlice.getBuilderParams(mock())

            params.onRemoveClick()

            verify(bloggingPromptsSettingsHelper).updatePromptsCardEnabled(any(), eq(false))
            assertThat(navigationActions.last() is BloggingPromptCardNavigationAction.CardRemoved)
        }

    @Test
    fun `given remove undo snackbar, when undo is clicked, then it tracks undo event`() = test {
        val params = viewModelSlice.getBuilderParams(mock())

        params.onRemoveClick()
        (navigationActions.last() as BloggingPromptCardNavigationAction.CardRemoved).undoClick()

        verify(bloggingPromptsCardAnalyticsTracker).trackMySiteCardRemoveFromDashboardUndoClicked()
    }

    @Test
    fun `when onBloggingPromptsLearnMoreClicked should post value on onBloggingPromptsLearnMore`() {
        val params = viewModelSlice.getBuilderParams(mock())

        params.onViewMoreClick()

        assertThat(navigationActions).containsOnly(BloggingPromptCardNavigationAction.ViewMore)
    }
}
