package org.wordpress.android.ui.reader.saved

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.ui.reader.saved.ReaderSavedPostsViewModel.Event
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.NetworkUtilsWrapper
import uniffi.wp_api.ReaderPostSaveIdentity
import uniffi.wp_api.ReaderSavedKey
import uniffi.wp_mobile.ReaderSavedException
import uniffi.wp_mobile.ReaderSavedListInfo
import uniffi.wp_mobile.ReaderSavedListItem
import uniffi.wp_mobile.ReaderSavedPostsCollection
import uniffi.wp_mobile_cache.ListState
import uniffi.wp_mobile_cache.ReaderPostRecord

@ExperimentalCoroutinesApi
class ReaderSavedPostsViewModelTest : BaseUnitTest() {
    @Mock
    lateinit var repository: ReaderSavedPostsRepository

    @Mock
    lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    @Mock
    lateinit var readerTracker: ReaderTracker

    @Mock
    lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var viewModel: ReaderSavedPostsViewModel

    @Before
    fun setUp() {
        viewModel = ReaderSavedPostsViewModel(
            repository,
            readerPostTableWrapper,
            readerTracker,
            networkUtilsWrapper,
            testDispatcher()
        )
    }

    @Test
    fun `unsaving offline reports no network and doesn't touch the account`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.onUnsaveClicked(postItem)

        assertThat(viewModel.events.first()).isEqualTo(Event.NoNetwork)
        verify(repository, never()).setSaved(any<ReaderPostSaveIdentity>(), any())
    }

    @Test
    fun `unsaving a post unsaves it on the account and clears its local flag`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(repository.setSaved(identity, false)).thenReturn(true)

        viewModel.onUnsaveClicked(postItem)

        assertThat(viewModel.events.first()).isEqualTo(Event.Unsaved(postItem))
        verify(readerPostTableWrapper).setBookmarkFlag(BLOG_ID, POST_ID, false)
    }

    @Test
    fun `a failed unsave reports the failure and leaves the local flag alone`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(repository.setSaved(identity, false)).thenReturn(false)

        viewModel.onUnsaveClicked(postItem)

        assertThat(viewModel.events.first()).isEqualTo(Event.RequestFailed)
        verify(readerPostTableWrapper, never()).setBookmarkFlag(any(), any(), any())
    }

    @Test
    fun `unsaving an item without a post unsaves its key`() = test {
        val urlItem = ReaderSavedListItem(ReaderSavedKey.Url("https://example.com"), null, null, false, false)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(repository.unsaveKey(urlItem.key)).thenReturn(true)

        viewModel.onUnsaveClicked(urlItem)

        assertThat(viewModel.events.first()).isEqualTo(Event.Unsaved(urlItem))
    }

    @Test
    fun `a refresh that fails shows the cached items with a load error`() = test {
        val collection = mock<ReaderSavedPostsCollection>()
        whenever(repository.savedPosts()).thenReturn(collection)
        whenever(collection.loadItems()).thenReturn(listOf(postItem))
        whenever(collection.listInfo()).thenReturn(ReaderSavedListInfo(ListState.ERROR, null, false, null))
        whenever(collection.refresh()).thenThrow(ReaderSavedException.PostCannotBeSaved())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel.start()

        val state = viewModel.uiState.value
        assertThat(state.items).containsExactly(postItem)
        assertThat(state.loadFailed).isTrue()
        assertThat(state.isRefreshing).isFalse()
    }

    private companion object {
        const val BLOG_ID = 1L
        const val POST_ID = 2L
        val identity = ReaderPostSaveIdentity(
            id = POST_ID.toULong(),
            siteId = BLOG_ID.toULong(),
            feedId = null,
            feedItemId = null,
            isExternal = false,
            url = null
        )
        val postItem = ReaderSavedListItem(
            key = ReaderSavedKey.Post(BLOG_ID.toULong(), POST_ID),
            dateSaved = null,
            post = ReaderPostRecord(key = ReaderSavedKey.Post(BLOG_ID.toULong(), POST_ID), identity = identity),
            isRecent = false,
            isLocalOnly = false
        )
    }
}
