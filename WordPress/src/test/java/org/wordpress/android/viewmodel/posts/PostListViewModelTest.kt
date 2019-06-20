package org.wordpress.android.viewmodel.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PagedListWrapper
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.posts.PostListType.SEARCH
import org.wordpress.android.ui.uploads.LocalDraftUploadStarter

class PostListViewModelTest : BaseUnitTest() {
    @Mock private lateinit var site: SiteModel
    @Mock private lateinit var localDraftUploadStarter: LocalDraftUploadStarter

    private lateinit var viewModel: PostListViewModel

    @UseExperimental(InternalCoroutinesApi::class)
    @Before
    fun setUp() {
        val listStore = mock<ListStore>()
        val postList = mock<PagedListWrapper<PostListItemType>>()

        whenever(
                postList.listError
        ).thenReturn(mock())

        whenever(
                postList.isFetchingFirstPage
        ).thenReturn(mock())

        whenever(
                postList.isEmpty
        ).thenReturn(mock())

        whenever(
                postList.data
        ).thenReturn(mock())

        whenever(
                postList.isLoadingMore
        ).thenReturn(mock())

        whenever(
                listStore.getList<PostListDescriptorForXmlRpcSite, PostListItemIdentifier, PostListItemType>(
                        any(),
                        any(),
                        any()
                )
        ).thenReturn(postList)

        viewModel = PostListViewModel(
                dispatcher = mock(),
                listStore = listStore,
                postStore = mock(),
                accountStore = mock(),
                listItemUiStateHelper = mock(),
                networkUtilsWrapper = mock(),
                localDraftUploadStarter = localDraftUploadStarter,
                connectionStatus = mock(),
                uiDispatcher = TEST_DISPATCHER,
                bgDispatcher = TEST_DISPATCHER
        )
    }

    @Test
    fun `when swiping to refresh, it uploads all local drafts`() {
        viewModel.start(createPostListViewModelConnector(site = site, postListType = DRAFTS))

        // When
        viewModel.swipeToRefresh()

        // Then
        verify(localDraftUploadStarter, times(1)).queueUploadFromSite(eq(site))
    }

    @Test
    fun `empty search query should show search prompt`() {
        viewModel.start(createPostListViewModelConnector(site = site, postListType = SEARCH))

        val emptyViewStateResults = mutableListOf<PostListEmptyUiState>()

        viewModel.emptyViewState.observeForever {
            emptyViewStateResults.add(it)
        }

        viewModel.search(null, 0)

        assertThat(emptyViewStateResults.size).isEqualTo(2) // initial state + state after search
        assertThat(emptyViewStateResults[1].emptyViewVisible).isTrue()
    }

    private companion object {
        fun createPostListViewModelConnector(site: SiteModel, postListType: PostListType) = PostListViewModelConnector(
                site = site,
                postListType = postListType,
                authorFilter = mock(),
                postActionHandler = mock(),
                getUploadStatus = mock(),
                doesPostHaveUnhandledConflict = mock(),
                getFeaturedImageUrl = mock(),
                postFetcher = mock()
        )
    }
}
