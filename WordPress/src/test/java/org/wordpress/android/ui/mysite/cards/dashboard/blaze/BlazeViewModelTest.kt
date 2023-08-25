package org.wordpress.android.ui.mysite.cards.dashboard.blaze

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.store.MediaStore
import org.wordpress.android.ui.blaze.BlazeFeatureUtils
import org.wordpress.android.ui.blaze.BlazeFlowSource
import org.wordpress.android.ui.blaze.BlazeUiState
import org.wordpress.android.ui.blaze.PageUIModel
import org.wordpress.android.ui.blaze.PostUIModel
import org.wordpress.android.ui.blaze.blazeoverlay.BlazeViewModel
import org.wordpress.android.ui.mysite.SelectedSiteRepository

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class BlazeViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    lateinit var blazeFeatureUtils: BlazeFeatureUtils

    @Mock
    lateinit var mediaStore: MediaStore

    @Mock
    private lateinit var dispatcher: Dispatcher

    private lateinit var blazeViewModel: BlazeViewModel

    private lateinit var uiStates: MutableList<BlazeUiState>
    private lateinit var promoteUiState: MutableList<BlazeUiState>
    @Before
    fun setUp() {
        blazeViewModel = BlazeViewModel(
            blazeFeatureUtils,
            dispatcher,
            mediaStore,
            selectedSiteRepository
        )
        uiStates = mutableListOf()
        promoteUiState = mutableListOf()
        blazeViewModel.promoteUiState.observeForever {
            promoteUiState.add(it)
        }
        blazeViewModel.uiState.observeForever {
            uiStates.add(it)
        }
    }

    @Test
    fun `given getSelectedSite is null, when xx, then exception`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(null)
        val model = PostUIModel(postId = 1L, title = "title", featuredImageId = 1L,
            url = "url", featuredImageUrl = "featuredImageUrl"
        )
        val result = blazeViewModel.start(BlazeFlowSource.POSTS_LIST, model, getShouldShowOverlay())


        Assertions.assertThat(result).isNotNull
    }

    @Test
    fun `given blaze overlay shown is false, when started from post list, then uiState is set to promote post`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
        whenever(blazeFeatureUtils.shouldHideBlazeOverlay()).thenReturn(false)

        val model = PostUIModel(postId = 1L, title = "title", featuredImageId = 1L,
            url = "url", featuredImageUrl = "featuredImageUrl"
        )
        blazeViewModel.start(BlazeFlowSource.POSTS_LIST, model, getShouldShowOverlay())

        Assertions.assertThat(uiStates.last()).isInstanceOf(BlazeUiState.PromoteScreen.PromotePost::class.java)
    }

    @Test
    fun `given blaze overlay shown is true, when started from post list, then uiState is set to webview screen`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
        whenever(blazeFeatureUtils.shouldHideBlazeOverlay()).thenReturn(true)

        val model = PostUIModel(postId = 1L, title = "title", featuredImageId = 1L,
            url = "url", featuredImageUrl = "featuredImageUrl"
        )
        blazeViewModel.start(BlazeFlowSource.POSTS_LIST, model, getShouldShowOverlay())

        Assertions.assertThat(uiStates.last()).isInstanceOf(BlazeUiState.WebViewScreen::class.java)
    }

    @Test
    fun `given blaze overlay shown is false, when started from page list, then uiState is set to promote post`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
        whenever(blazeFeatureUtils.shouldHideBlazeOverlay()).thenReturn(false)

        val model = PageUIModel(pageId = 1L, title = "title", featuredImageId = 1L,
            url = "url", featuredImageUrl = "featuredImageUrl"
        )
        blazeViewModel.start(BlazeFlowSource.PAGES_LIST, model, getShouldShowOverlay())

        Assertions.assertThat(uiStates.last()).isInstanceOf(BlazeUiState.PromoteScreen.PromotePage::class.java)
    }

    @Test
    fun `given blaze overlay shown is true, when started from page list, then uiState is set to webview screen`() {
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(mock())
        whenever(blazeFeatureUtils.shouldHideBlazeOverlay()).thenReturn(true)

        val model = PageUIModel(pageId = 1L, title = "title", featuredImageId = 1L,
            url = "url", featuredImageUrl = "featuredImageUrl"
        )
        blazeViewModel.start(BlazeFlowSource.PAGES_LIST, model, getShouldShowOverlay())

        Assertions.assertThat(uiStates.last()).isInstanceOf(BlazeUiState.WebViewScreen::class.java)
    }

    @Test
    fun `given blaze overlay shown is false, when started from dashboard card, then uiState is set to site`() {
        whenever(blazeFeatureUtils.shouldHideBlazeOverlay()).thenReturn(false)

        blazeViewModel.start(BlazeFlowSource.DASHBOARD_CARD, null, getShouldShowOverlay())

        Assertions.assertThat(uiStates.last()).isInstanceOf(BlazeUiState.PromoteScreen.Site::class.java)
    }

    @Test
    fun `given blaze overlay shown is true, when started from dashboard card, then uiState is set to webview screen`() {
        whenever(blazeFeatureUtils.shouldHideBlazeOverlay()).thenReturn(true)

        blazeViewModel.start(BlazeFlowSource.DASHBOARD_CARD, null, getShouldShowOverlay())

        Assertions.assertThat(uiStates.last()).isInstanceOf(BlazeUiState.WebViewScreen::class.java)
    }

    @Test
    fun `given blaze overlay shown is false, when started from no campaigns list, then uiState is set to site`() {
        whenever(blazeFeatureUtils.shouldHideBlazeOverlay()).thenReturn(false)

        blazeViewModel.start(BlazeFlowSource.CAMPAIGN_LISTING_PAGE, null, getShouldShowOverlay())

        Assertions.assertThat(uiStates.last()).isInstanceOf(BlazeUiState.PromoteScreen.Site::class.java)
    }

    @Test
    fun `given blaze overlay shown is true, when started from no campaigns list, then uiState is set to webview`() {
        whenever(blazeFeatureUtils.shouldHideBlazeOverlay()).thenReturn(true)

        blazeViewModel.start(BlazeFlowSource.CAMPAIGN_LISTING_PAGE, null, getShouldShowOverlay())

        Assertions.assertThat(uiStates.last()).isInstanceOf(BlazeUiState.WebViewScreen::class.java)
    }
}
