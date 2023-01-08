package org.wordpress.android.ui.reader.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.ui.reader.comments.ThreadedCommentsActionSource.READER_THREADED_COMMENTS
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.Failure
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.FollowCommentsNotAllowed
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.FollowStateChanged
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.Loading
import org.wordpress.android.ui.reader.usecases.ReaderCommentsFollowUseCase.FollowCommentsState.UserNotAuthenticated
import org.wordpress.android.ui.reader.utils.PostSubscribersApiCallsProvider
import org.wordpress.android.ui.reader.utils.PostSubscribersApiCallsProvider.PostSubscribersCallResult
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderCommentsFollowUseCaseTest : BaseUnitTest() {
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var postSubscribersApiCallsProvider: PostSubscribersApiCallsProvider
    @Mock private lateinit var accountStore: AccountStore
    @Mock private lateinit var readerTracker: ReaderTracker
    @Mock private lateinit var readerPostTableWrapper: ReaderPostTableWrapper

    private lateinit var followCommentsUseCase: ReaderCommentsFollowUseCase

    private val blogId = 100L
    private val postId = 1000L

    @Before
    fun setup() {
        whenever(accountStore.hasAccessToken()).thenReturn(true)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        followCommentsUseCase = ReaderCommentsFollowUseCase(
                networkUtilsWrapper,
                postSubscribersApiCallsProvider,
                accountStore,
                readerTracker,
                readerPostTableWrapper
        )
    }

    @Test
    fun `getMySubscriptionToPost emits expected state when user not logged in`() = test {
        whenever(accountStore.hasAccessToken()).thenReturn(false)
        val flow = followCommentsUseCase.getMySubscriptionToPost(blogId, postId, false)

        assertThat(flow.toList()).isEqualTo(listOf(UserNotAuthenticated))
    }

    @Test
    fun `getMySubscriptionToPost emits expected state when no network`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val flow = followCommentsUseCase.getMySubscriptionToPost(blogId, postId, false)

        assertThat(flow.toList()).isEqualTo(
                listOf(Loading, Failure(blogId, postId, UiStringRes(R.string.error_network_connection)))
        )
    }

    @Test
    fun `getMySubscriptionToPost emits expected state when cannot follow comments`() = test {
        whenever(postSubscribersApiCallsProvider.getCanFollowComments(anyLong())).thenReturn(false)

        val flow = followCommentsUseCase.getMySubscriptionToPost(blogId, postId, false)

        assertThat(flow.toList()).isEqualTo(listOf(Loading, FollowCommentsNotAllowed))
    }

    @Test
    fun `getMySubscriptionToPost emits expected state when can follow with success`() = test {
        whenever(postSubscribersApiCallsProvider.getCanFollowComments(anyLong())).thenReturn(true)
        whenever(postSubscribersApiCallsProvider.getMySubscriptionToPost(anyLong(), anyLong()))
                .thenReturn(PostSubscribersCallResult.Success(true, false))

        val flow = followCommentsUseCase.getMySubscriptionToPost(blogId, postId, false)

        assertThat(flow.toList()).isEqualTo(listOf(
                        Loading,
                        FollowStateChanged(
                            blogId,
                            postId,
                            true,
                            false
                        )
        ))
    }

    @Test
    fun `getMySubscriptionToPost emits expected state when can follow with failure`() = test {
        val errorMessage = "There was an error"
        val failure = PostSubscribersCallResult.Failure(errorMessage)

        whenever(postSubscribersApiCallsProvider.getCanFollowComments(anyLong())).thenReturn(true)
        whenever(postSubscribersApiCallsProvider.getMySubscriptionToPost(anyLong(), anyLong())).thenReturn(failure)

        val flow = followCommentsUseCase.getMySubscriptionToPost(blogId, postId, false)

        assertThat(flow.toList()).isEqualTo(listOf(
                Loading,
                Failure(blogId, postId, UiStringText(errorMessage))
        ))
    }

    @Test
    fun `setMySubscriptionToPost emits expected state when no network`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val flow = followCommentsUseCase.setMySubscriptionToPost(blogId, postId, true, READER_THREADED_COMMENTS)

        assertThat(flow.toList()).isEqualTo(
                listOf(Loading, Failure(blogId, postId, UiStringRes(R.string.error_network_connection)))
        )
    }

    @Test
    fun `setMySubscriptionToPost emits expected state when subscribing with success`() = test {
        whenever(postSubscribersApiCallsProvider.subscribeMeToPost(anyLong(), anyLong()))
                .thenReturn(PostSubscribersCallResult.Success(true, false))

        val flow = followCommentsUseCase.setMySubscriptionToPost(blogId, postId, true, READER_THREADED_COMMENTS)

        assertThat(flow.toList()).isEqualTo(listOf(
                Loading,
                FollowStateChanged(
                        blogId,
                        postId,
                        true,
                        false,
                        false,
                        UiStringRes(R.string.reader_follow_comments_subscribe_success_enable_push),
                        false
                )
        ))
    }

    @Test
    fun `setMySubscriptionToPost emits expected state when unsubscribing with failure`() = test {
        val errorMessage = "There was an error"
        val failure = PostSubscribersCallResult.Failure(errorMessage)

        whenever(postSubscribersApiCallsProvider.unsubscribeMeFromPost(anyLong(), anyLong())).thenReturn(failure)

        val flow = followCommentsUseCase.setMySubscriptionToPost(blogId, postId, false, READER_THREADED_COMMENTS)

        assertThat(flow.toList()).isEqualTo(listOf(
                Loading,
                Failure(blogId, postId, UiStringText(errorMessage))
        ))
    }

    @Test
    fun `setEnableByPushNotifications emits expected state when no network`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        val flow = followCommentsUseCase.setEnableByPushNotifications(blogId, postId, true, READER_THREADED_COMMENTS)

        assertThat(flow.toList()).isEqualTo(listOf(FollowStateChanged(
                        blogId = blogId,
                        postId = postId,
                        isFollowing = true,
                        isReceivingNotifications = false,
                        false,
                        userMessage = UiStringRes(R.string.error_network_connection),
                        true
        )))
    }

    @Test
    fun `setEnableByPushNotifications emits expected state when subscribing with success`() = test {
        whenever(postSubscribersApiCallsProvider.managePushNotificationsForPost(anyLong(), anyLong(), eq(true)))
                .thenReturn(PostSubscribersCallResult.Success(true, true))

        val flow = followCommentsUseCase.setEnableByPushNotifications(blogId, postId, true, READER_THREADED_COMMENTS)

        assertThat(flow.toList()).isEqualTo(listOf(
                FollowStateChanged(
                        blogId,
                        postId,
                        true,
                        true,
                        false,
                        UiStringRes(R.string.reader_follow_comments_subscribe_to_push_success),
                        false
                )
        ))
    }

    @Test
    fun `setEnableByPushNotifications emits expected state when subscribing with failure`() = test {
        val errorMessage = "There was an error"

        whenever(postSubscribersApiCallsProvider.managePushNotificationsForPost(anyLong(), anyLong(), eq(true)))
                .thenReturn(PostSubscribersCallResult.Failure(errorMessage))

        val flow = followCommentsUseCase.setEnableByPushNotifications(blogId, postId, true, READER_THREADED_COMMENTS)

        assertThat(flow.toList()).isEqualTo(listOf(
                FollowStateChanged(
                        blogId = blogId,
                        postId = postId,
                        isFollowing = true,
                        isReceivingNotifications = false,
                        false,
                        userMessage = UiStringRes(R.string.reader_follow_comments_could_not_subscribe_to_push_error),
                        true
                )
        ))
    }
}
