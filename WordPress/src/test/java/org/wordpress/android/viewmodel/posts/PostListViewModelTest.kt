package org.wordpress.android.viewmodel.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.doReturn
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.list.PostListDescriptor.PostListDescriptorForXmlRpcSite
import org.wordpress.android.fluxc.store.ListStore
import org.wordpress.android.ui.posts.PostListType.DRAFTS
import org.wordpress.android.ui.uploads.LocalDraftUploadStarter

class PostListViewModelTest {
    @Test
    fun `when swiping to refresh, it uploads all local drafts`() {
        // Given
        val site = SiteModel()
        val localDraftUploadStarter = mock<LocalDraftUploadStarter>()

        val viewModel = createPostListViewModel(localDraftUploadStarter = localDraftUploadStarter)
        viewModel.start(createPostListViewModelConnector(site = site))

        // When
        viewModel.swipeToRefresh()

        // Then
        verify(localDraftUploadStarter, times(1)).queueUploadFromSite(eq(site))
    }

    private companion object {
        fun createPostListViewModelConnector(site: SiteModel) = PostListViewModelConnector(
                site = site,
                postListType = DRAFTS,
                authorFilter = mock(),
                postActionHandler = mock(),
                getUploadStatus = mock(),
                doesPostHaveUnhandledConflict = mock(),
                getFeaturedImageUrl = mock(),
                postFetcher = mock()
        )

        fun createPostListViewModel(localDraftUploadStarter: LocalDraftUploadStarter): PostListViewModel {
            val listStore = mock<ListStore> {
                on {
                    getList<PostListDescriptorForXmlRpcSite, PostListItemIdentifier, PostListItemType>(
                            any(),
                            any(),
                            any()
                    )
                } doReturn mock()
            }

            return PostListViewModel(
                    dispatcher = mock(),
                    listStore = listStore,
                    postStore = mock(),
                    accountStore = mock(),
                    listItemUiStateHelper = mock(),
                    networkUtilsWrapper = mock(),
                    localDraftUploadStarter = localDraftUploadStarter,
                    connectionStatus = mock()
            )
        }
    }
}
