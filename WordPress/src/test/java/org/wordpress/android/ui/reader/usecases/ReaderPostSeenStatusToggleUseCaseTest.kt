package org.wordpress.android.ui.reader.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.Error
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.PostSeenStateChanged
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.UserNotAuthenticated
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.ReaderPostSeenToggleSource.READER_POST_CARD
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.ReaderPostSeenToggleSource.READER_POST_DETAILS
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider.SeenStatusToggleCallResult.Success
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostSeenStatusToggleUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var postSeenStatusApiCallsProvider: PostSeenStatusApiCallsProvider
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var readerTracker: ReaderTracker
    @Mock private lateinit var readerPostTableWrapper: ReaderPostTableWrapper
    @Mock private lateinit var readerBlogTableWrapper: ReaderBlogTableWrapper

    private lateinit var seenStatusToggleUseCase: ReaderSeenStatusToggleUseCase

    @Before
    fun setup() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(readerPostTableWrapper.isPostSeen(any())).thenReturn(true)

        seenStatusToggleUseCase = ReaderSeenStatusToggleUseCase(
                networkUtilsWrapper,
                postSeenStatusApiCallsProvider,
                accountStore,
                readerTracker,
                readerPostTableWrapper,
                readerBlogTableWrapper
        )
    }

    @Test
    fun `toggleSeenStatus emits expected state when user not logged in`() = test {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        val dummyPost = createDummyReaderPost()
        val flow = seenStatusToggleUseCase.toggleSeenStatus(dummyPost, READER_POST_CARD)

        val result = flow.toList()
        assertThat(result).isEqualTo(listOf(UserNotAuthenticated))
    }

    @Test
    fun `toggleSeenStatus emits expected state when no network`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val dummyPost = createDummyReaderPost()
        val flow = seenStatusToggleUseCase.toggleSeenStatus(dummyPost, READER_POST_CARD)

        val result = flow.toList()
        assertThat(result).isEqualTo(
                listOf(Error(UiStringRes(string.error_network_connection)))
        )
    }

    @Test
    fun `toggleSeenStatus emits expected state when trying to change status of unsupported post`() = test {
        val dummyPost = createDummyReaderPost(isSeen = true, isSeenSupported = false)
        val flow = seenStatusToggleUseCase.toggleSeenStatus(dummyPost, READER_POST_CARD)

        val result = flow.toList()
        assertThat(result).isEqualTo(
                listOf(Error(UiStringRes(string.reader_error_changing_seen_status_of_unsupported_post)))
        )
    }

    @Test
    fun `markPostAsSeenIfNecessary correctly marks post as seen depending on if it is seen or not`() = test {
        whenever(
                postSeenStatusApiCallsProvider.markPostAsSeen(any())
        ).thenReturn(Success(true))

        val unseenPost = createDummyReaderPost(isSeen = false)
        val seenPost = createDummyReaderPost(isSeen = true)
        whenever(readerPostTableWrapper.isPostSeen(unseenPost)).thenReturn(false)
        seenStatusToggleUseCase.markPostAsSeenIfNecessary(seenPost)

        verify(postSeenStatusApiCallsProvider, times(0)).markPostAsSeen(any())
        verify(readerPostTableWrapper, times(0)).setPostSeenStatusInDb(any(), any())
        verify(readerTracker, times(0)).trackPost(
                stat = any(),
                post = any(),
                source = any()
        )

        seenStatusToggleUseCase.markPostAsSeenIfNecessary(unseenPost)

        verify(postSeenStatusApiCallsProvider, times(1)).markPostAsSeen(unseenPost)
        verify(readerPostTableWrapper, times(1)).setPostSeenStatusInDb(unseenPost, true)
        verify(readerTracker, never()).trackPost(
                AnalyticsTracker.Stat.READER_POST_MARKED_AS_SEEN,
                unseenPost,
                READER_POST_DETAILS.toString()
        )
    }

    @Test
    fun `toggleSeenStatus emits correct state when toggling post seen status`() = test {
        whenever(
                postSeenStatusApiCallsProvider.markPostAsUnseen(any())
        ).thenReturn(Success(false))
        whenever(
                postSeenStatusApiCallsProvider.markPostAsSeen(any())
        ).thenReturn(Success(true))

        val unseenPost = createDummyReaderPost(isSeen = false)
        val seenPost = createDummyReaderPost(isSeen = true)

        whenever(readerPostTableWrapper.isPostSeen(unseenPost)).thenReturn(false)

        val markAsSeenFlow = seenStatusToggleUseCase.toggleSeenStatus(unseenPost, READER_POST_DETAILS)
        val markAsUnseenFlow = seenStatusToggleUseCase.toggleSeenStatus(seenPost, READER_POST_CARD)

        assertThat(markAsSeenFlow.toList()).isEqualTo(
                listOf(
                        PostSeenStateChanged(
                                true,
                                UiStringRes(string.reader_marked_post_as_seen)
                        )
                )
        )

        // local DB status toggle check
        verify(readerPostTableWrapper, times(1)).setPostSeenStatusInDb(unseenPost, true)
        verify(readerBlogTableWrapper, times(1)).decrementUnseenCount(unseenPost.blogId)

        // analytics check
        verify(readerTracker, times(1)).trackPost(
                AnalyticsTracker.Stat.READER_POST_MARKED_AS_SEEN,
                unseenPost,
                READER_POST_DETAILS.toString()
        )

        assertThat(markAsUnseenFlow.toList()).isEqualTo(
                listOf(
                        PostSeenStateChanged(
                                false,
                                UiStringRes(string.reader_marked_post_as_unseen)
                        )
                )
        )

        // local DB status toggle check
        verify(readerPostTableWrapper, times(1)).setPostSeenStatusInDb(seenPost, false)
        verify(readerBlogTableWrapper, times(1)).incrementUnseenCount(unseenPost.blogId)

        // analytics check
        verify(readerTracker, times(1)).trackPost(
                AnalyticsTracker.Stat.READER_POST_MARKED_AS_UNSEEN,
                seenPost,
                READER_POST_CARD.toString()
        )
    }

    private fun createDummyReaderPost(isSeen: Boolean = false, isSeenSupported: Boolean = true): ReaderPost =
            ReaderPost().apply {
                this.postId = 1
                this.blogId = 2
                this.feedId = 3
                this.isSeen = isSeen
                this.isSeenSupported = isSeenSupported
            }
}
