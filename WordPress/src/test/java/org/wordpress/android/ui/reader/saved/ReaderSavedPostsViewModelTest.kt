package org.wordpress.android.ui.reader.saved

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState
import org.wordpress.android.ui.reader.discover.ReaderCardUiState.ReaderPostUiState.CompactBlogSectionData
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.saved.ReaderSavedPostsRepository.WriteResult
import org.wordpress.android.ui.reader.saved.ReaderSavedPostsViewModel.Event
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListEmptyState
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.image.ImageType
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

    @Mock
    lateinit var cardBuilder: ReaderSavedPostCardBuilder

    private lateinit var viewModel: ReaderSavedPostsViewModel

    @Before
    fun setUp() {
        viewModel = ReaderSavedPostsViewModel(
            repository,
            readerPostTableWrapper,
            readerTracker,
            networkUtilsWrapper,
            cardBuilder,
            testDispatcher()
        )
    }

    @Test
    fun `unsaving offline still unsaves, since wordpress-rs sends it with the next sync`() = test {
        whenever(repository.setSaved(identity, false)).thenReturn(WriteResult.SUCCEEDED)

        viewModel.onUnsaveClicked(postItem)

        assertThat(viewModel.events.first()).isEqualTo(Event.Unsaved(postItem))
        verify(networkUtilsWrapper, never()).isNetworkAvailable()
    }

    @Test
    fun `unsaving a post unsaves it on the account and clears its local flag`() = test {
        whenever(repository.setSaved(identity, false)).thenReturn(WriteResult.SUCCEEDED)

        viewModel.onUnsaveClicked(postItem)

        assertThat(viewModel.events.first()).isEqualTo(Event.Unsaved(postItem))
        verify(readerPostTableWrapper).setBookmarkFlag(BLOG_ID, POST_ID, false)
    }

    @Test
    fun `unsaving a post is tracked as the device's Saved list tracks it`() = test {
        whenever(repository.setSaved(identity, false)).thenReturn(WriteResult.SUCCEEDED)

        viewModel.onUnsaveClicked(postItem)
        viewModel.events.first()

        verify(readerTracker).trackBlog(
            AnalyticsTracker.Stat.READER_POST_UNSAVED_FROM_SAVED_POST_LIST,
            BLOG_ID,
            0L,
            null,
            ReaderTracker.SOURCE_SAVED
        )
    }

    @Test
    fun `a failed unsave reports the failure and leaves the local flag alone`() = test {
        whenever(repository.setSaved(identity, false)).thenReturn(WriteResult.FAILED)

        viewModel.onUnsaveClicked(postItem)

        assertThat(viewModel.events.first()).isEqualTo(Event.RequestFailed)
        verify(readerPostTableWrapper, never()).setBookmarkFlag(any(), any(), any())
    }

    @Test
    fun `unsaving an item without a post unsaves its key`() = test {
        val urlItem = ReaderSavedListItem(ReaderSavedKey.Url("https://example.com"), null, null, false, false)
        whenever(repository.unsaveKey(urlItem.key)).thenReturn(WriteResult.SUCCEEDED)

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
        whenever(cardBuilder.build(any(), any(), any(), any())).thenReturn(card)

        viewModel.start(photonWidth = 100, photonHeight = 50)

        val state = viewModel.uiState.value
        assertThat(state.posts.map { it.item }).containsExactly(postItem)
        assertThat(state.error).isInstanceOf(ReaderPostListEmptyState.RequestFailed::class.java)
        assertThat(state.isRefreshing).isFalse()
    }

    private companion object {
        val card = ReaderPostUiState(
            source = ReaderTracker.SOURCE_SAVED,
            postId = 0L,
            blogId = 0L,
            feedId = 0L,
            isFollowed = false,
            blogSection = CompactBlogSectionData(
                0L, 0L, "", UiStringText(""), null, null, false, ImageType.BLAVATAR, null
            ),
            title = null,
            excerpt = null,
            featuredImageUrl = null,
            thumbnailStripSection = null,
            featuredImageVisibility = false,
            moreMenuVisibility = false,
            bookmarkAction = PrimaryAction(true, type = ReaderPostCardActionType.BOOKMARK),
            likeAction = PrimaryAction(false, type = ReaderPostCardActionType.LIKE),
            reblogAction = PrimaryAction(false, type = ReaderPostCardActionType.REBLOG),
            commentsAction = PrimaryAction(true, type = ReaderPostCardActionType.COMMENTS),
            onItemClicked = { _, _ -> },
            onItemRendered = {},
            onMoreButtonClicked = {},
            onMoreDismissed = {},
        )
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
        private val postKey = ReaderSavedKey.Post(BLOG_ID.toULong(), POST_ID)
        val postItem = ReaderSavedListItem(
            key = postKey,
            dateSaved = null,
            post = ReaderPostRecord(key = postKey, identity = identity),
            isPending = false,
            isWaitingForRoom = false
        )
    }
}
