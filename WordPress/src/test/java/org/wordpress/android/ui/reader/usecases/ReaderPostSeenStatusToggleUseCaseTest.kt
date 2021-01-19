package org.wordpress.android.ui.reader.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.analytics.AnalyticsTracker
import org.wordpress.android.datasets.ReaderBlogTableWrapper
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.test
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.Failure
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.PostSeenStateChanged
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.PostSeenState.UserNotAuthenticated
import org.wordpress.android.ui.reader.usecases.ReaderSeenStatusToggleUseCase.ReaderPostSeenToggleSource.READER_POST_CARD
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider
import org.wordpress.android.ui.reader.utils.PostSeenStatusApiCallsProvider.SeenStatusToggleCallResult.Success
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostSeenStatusToggleUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var postSeenStatusApiCallsProvider: PostSeenStatusApiCallsProvider
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
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
                analyticsUtilsWrapper,
                readerPostTableWrapper,
                readerBlogTableWrapper
        )
    }

    @Test
    fun `toggleSeenStatus emits expected state when user not logged in`() = test {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        val dummyPost = createDummyReaderPost()
        val flow = seenStatusToggleUseCase.toggleSeenStatus(dummyPost)

        assertThat(flow.toList()).isEqualTo(listOf(UserNotAuthenticated))
    }

    @Test
    fun `setMySubscriptionToPost emits expected state when no network`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val dummyPost = createDummyReaderPost()
        val flow = seenStatusToggleUseCase.toggleSeenStatus(dummyPost)

        assertThat(flow.toList()).isEqualTo(
                listOf(Failure(UiStringRes(R.string.error_network_connection)))
        )
    }

    @Test
    fun `markPostAsSeenIfNecessary emits expected state when marking post as seen`() = test {
        whenever(
                postSeenStatusApiCallsProvider.markPostAsSeen(any())
        ).thenReturn(Success(true))

        val unseenPost = createDummyReaderPost(isSeen = false)
        val seenPost = createDummyReaderPost(isSeen = true)
        whenever(readerPostTableWrapper.isPostSeen(unseenPost)).thenReturn(false)

        val markAsSeenFlow = seenStatusToggleUseCase.markPostAsSeenIfNecessary(unseenPost)
        val emptyFlow = seenStatusToggleUseCase.markPostAsSeenIfNecessary(seenPost)

        assertThat(markAsSeenFlow.toList()).isEqualTo(
                listOf(
                        PostSeenStateChanged(
                                true,
                                UiStringRes(R.string.reader_marked_post_as_seen)
                        )
                )
        )

        assertThat(emptyFlow.toList()).isEmpty()
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

        val markAsSeenFlow = seenStatusToggleUseCase.toggleSeenStatus(unseenPost)
        val markAsUnseenFlow = seenStatusToggleUseCase.toggleSeenStatus(seenPost)

        assertThat(markAsSeenFlow.toList()).isEqualTo(
                listOf(
                        PostSeenStateChanged(
                                true,
                                UiStringRes(R.string.reader_marked_post_as_seen)
                        )
                )
        )

        // local DB status toggle check
        verify(readerPostTableWrapper, times(1)).togglePostSeenStatusLocally(unseenPost, true)
        verify(readerBlogTableWrapper, times(1)).decrementUnseenCount(unseenPost.blogId)

        // analytics check
        verify(analyticsUtilsWrapper, times(1)).trackWithReaderPostDetails(
                AnalyticsTracker.Stat.READER_POST_MARKED_AS_SEEN, unseenPost, mapOf(
                ReaderSeenStatusToggleUseCase.ACTION_SOURCE_PARAM_NAME to READER_POST_CARD.toString()
        )
        )

        assertThat(markAsUnseenFlow.toList()).isEqualTo(
                listOf(
                        PostSeenStateChanged(
                                false,
                                UiStringRes(R.string.reader_marked_post_as_unseen)
                        )
                )
        )

        // local DB status toggle check
        verify(readerPostTableWrapper, times(1)).togglePostSeenStatusLocally(seenPost, false)
        verify(readerBlogTableWrapper, times(1)).incrementUnseenCount(unseenPost.blogId)

        // analytics check
        verify(analyticsUtilsWrapper, times(1)).trackWithReaderPostDetails(
                AnalyticsTracker.Stat.READER_POST_MARKED_AS_UNSEEN, seenPost, mapOf(
                ReaderSeenStatusToggleUseCase.ACTION_SOURCE_PARAM_NAME to READER_POST_CARD.toString()
        )
        )
    }

    private fun createDummyReaderPost(isSeen: Boolean = false): ReaderPost = ReaderPost().apply {
        this.postId = 1
        this.blogId = 2
        this.feedId = 3
        this.feedItemId = 4
        this.isSeen = isSeen
    }
}
