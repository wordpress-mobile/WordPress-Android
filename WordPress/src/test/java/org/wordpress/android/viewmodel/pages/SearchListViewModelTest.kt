package org.wordpress.android.viewmodel.pages

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.MutableLiveData
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R.string
import org.wordpress.android.TEST_SCOPE
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.ui.pages.PageItem
import org.wordpress.android.ui.pages.PageItem.Action.VIEW_PAGE
import org.wordpress.android.ui.pages.PageItem.Divider
import org.wordpress.android.ui.pages.PageItem.DraftPage
import org.wordpress.android.ui.pages.PageItem.Empty
import org.wordpress.android.ui.pages.PageItem.PublishedPage
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.pages.PageListViewModel.PageListType
import org.wordpress.android.viewmodel.pages.PostModelUploadUiStateUseCase.PostUploadUiState
import org.wordpress.android.viewmodel.uistate.ProgressBarUiState
import java.util.Date
import java.util.SortedMap

@RunWith(MockitoJUnitRunner::class)
class SearchListViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock lateinit var resourceProvider: ResourceProvider
    @Mock lateinit var site: SiteModel
    @Mock lateinit var pagesViewModel: PagesViewModel
    @Mock lateinit var createPageListItemLabelsUseCase: CreatePageListItemLabelsUseCase
    @Mock lateinit var pageItemProgressUiStateUseCase: PageItemProgressUiStateUseCase
    @Mock lateinit var pageListItemActionsUseCase: CreatePageListItemActionsUseCase
    @Mock lateinit var createUploadStateUseCase: PostModelUploadUiStateUseCase

    private lateinit var searchPages: MutableLiveData<SortedMap<PageListType, List<PageModel>>>
    private lateinit var viewModel: SearchListViewModel

    private lateinit var page: PageModel

    @Before
    fun setUp() {
        page = PageModel(PostModel(), site, 1, "title", PUBLISHED, Date(), false, 11L, null, 0)
        viewModel = SearchListViewModel(
                createPageListItemLabelsUseCase,
                createUploadStateUseCase,
                pageListItemActionsUseCase,
                pageItemProgressUiStateUseCase,
                resourceProvider,
                TEST_SCOPE
        )
        searchPages = MutableLiveData()

        whenever(pageItemProgressUiStateUseCase.getProgressStateForPage(any())).thenReturn(
                Pair(
                        ProgressBarUiState.Hidden,
                        false
                )
        )
        whenever(pagesViewModel.searchPages).thenReturn(searchPages)
        whenever(pagesViewModel.site).thenReturn(site)
        whenever(pagesViewModel.uploadStatusTracker).thenReturn(mock())
        whenever(createPageListItemLabelsUseCase.createLabels(any(), any())).thenReturn(
                Pair(
                        mock(), 0
                )
        )
        whenever(createUploadStateUseCase.createUploadUiState(any(), any(), any())).thenReturn(
                PostUploadUiState.NothingToUpload
        )
        viewModel.start(pagesViewModel)
    }

    @Test
    fun `show empty item on start`() {
        searchPages.value = null

        assertThat(viewModel.searchResult.value).containsOnly(Empty(string.pages_search_suggestion, true))
    }

    @Test
    fun `adds divider to published group`() {
        val expectedTitle = "title"
        var searchResult: List<PageItem>? = null
        viewModel.searchResult.observeForever { searchResult = it }
        for (status in PageListType.values()) {
            whenever(resourceProvider.getString(status.title)).thenReturn(expectedTitle)

            searchPages.value = sortedMapOf(status to listOf())

            assertThat(searchResult).containsOnly(Divider(expectedTitle))
        }
    }

    @Test
    fun `builds list with dividers from grouped result`() {
        val expectedTitle = "title"
        whenever(resourceProvider.getString(any())).thenReturn(expectedTitle)

        val publishedPageId = 1
        val publishedPageRemoteId = 11L
        val publishedPage = page.copy(pageId = publishedPageId, remoteId = publishedPageRemoteId, status = PUBLISHED)
        val publishedList = PageListType.PUBLISHED to listOf(publishedPage)
        val draftPageId = 2
        val draftPageRemoteId = 22L
        val draftPage = page.copy(pageId = draftPageId, remoteId = draftPageRemoteId, status = DRAFT)
        val draftList = PageListType.DRAFTS to listOf(draftPage)

        searchPages.value = sortedMapOf(publishedList, draftList)

        val searchResult = viewModel.searchResult.value
        assertThat(searchResult).isNotNull
        assertThat(searchResult).hasSize(4)
        assertThat(searchResult!![0]).isInstanceOf(Divider::class.java)
        (searchResult[0] as Divider).apply {
            assertThat(this.title).isEqualTo(expectedTitle)
        }
        assertThat(searchResult[1]).isInstanceOf(PublishedPage::class.java)
        (searchResult[1] as PublishedPage).apply {
            assertThat(this.remoteId).isEqualTo(publishedPageRemoteId)
        }
        assertThat(searchResult[2]).isInstanceOf(Divider::class.java)
        (searchResult[2] as Divider).apply {
            assertThat(this.title).isEqualTo(expectedTitle)
        }
        assertThat(searchResult[3]).isInstanceOf(DraftPage::class.java)
        (searchResult[3] as DraftPage).apply {
            assertThat(this.remoteId).isEqualTo(draftPageRemoteId)
        }
    }

    @Test
    fun `passes action to page view model on menu action`() {
        val clickedPage = PageItem.PublishedPage(
                remoteId = 1,
                localId = 1,
                title = "title",
                date = Date(),
                labels = listOf(),
                labelsColor = 0,
                indent = 0,
                imageUrl = null,
                actions = mock(),
                actionsEnabled = false,
                progressBarUiState = ProgressBarUiState.Hidden,
                showOverlay = false
        )
        val action = VIEW_PAGE

        viewModel.onMenuAction(action, clickedPage)

        verify(pagesViewModel).onMenuAction(action, clickedPage)
    }

    @Test
    fun `passes page to page view model on item tapped`() {
        val clickedPage = PageItem.PublishedPage(
                remoteId = 1,
                localId = 1,
                title = "title",
                date = Date(),
                labels = listOf(),
                labelsColor = 0,
                indent = 0,
                imageUrl = null,
                actions = mock(),
                actionsEnabled = false,
                progressBarUiState = ProgressBarUiState.Hidden,
                showOverlay = false
        )

        viewModel.onItemTapped(clickedPage)

        verify(pagesViewModel).onItemTapped(clickedPage)
    }
}
