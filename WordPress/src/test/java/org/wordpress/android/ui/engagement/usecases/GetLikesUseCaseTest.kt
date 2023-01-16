package org.wordpress.android.ui.engagement.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.CommentAction.FETCHED_COMMENT_LIKES
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged.FetchPostLikes
import org.wordpress.android.fluxc.model.LikeModel.LikeType.COMMENT_LIKE
import org.wordpress.android.fluxc.model.LikeModel.LikeType.POST_LIKE
import org.wordpress.android.fluxc.store.AccountStore
import org.wordpress.android.fluxc.store.CommentStore
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType
import org.wordpress.android.fluxc.store.CommentStore.OnCommentLikesChanged
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.OnPostLikesChanged
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.ui.engagement.GetLikesUseCase
import org.wordpress.android.ui.engagement.GetLikesUseCase.FailureType.GENERIC
import org.wordpress.android.ui.engagement.GetLikesUseCase.FailureType.NO_NETWORK
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure.EmptyStateData
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Loading
import org.wordpress.android.ui.engagement.GetLikesUseCase.LikeGroupFingerPrint
import org.wordpress.android.ui.engagement.GetLikesUseCase.PaginationParams
import org.wordpress.android.ui.engagement.GetLikesUseCase.PagingInfo
import org.wordpress.android.ui.engagement.utils.getDefaultLikers
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetLikesUseCaseTest : BaseUnitTest() {
    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    private lateinit var dispatcher: Dispatcher

    @Mock
    private lateinit var commentStore: CommentStore

    @Mock
    private lateinit var postStore: PostStore

    @Mock
    private lateinit var accountStore: AccountStore

    private lateinit var getLikesUseCase: GetLikesUseCase
    private val siteId = 100L
    private val postId = 1000L
    private val commentId = 10000L
    private val expectedNumLikes = 6
    private val defaultPageLenght = 20
    private val limitedPageLenght = 8

    private val pageInfo = PagingInfo(
        20,
        1
    )

    @Before
    fun setup() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        getLikesUseCase = GetLikesUseCase(
            networkUtilsWrapper,
            dispatcher,
            commentStore,
            postStore,
            accountStore
        )
    }

    @Test
    fun `dispatcher is registered on init`() {
        verify(dispatcher, times(1)).register(getLikesUseCase)
    }

    @Test
    fun `dispatcher is unregistered on clear`() {
        getLikesUseCase.clear()
        verify(dispatcher, times(1)).unregister(getLikesUseCase)
    }

    @Test
    fun `getLikesForPost emits Loading when not requesting next page`() = test {
        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onPostLikesChanged(
                OnPostLikesChanged(
                    FetchPostLikes,
                    siteId,
                    postId,
                    false
                )
            )
        }

        val flow = getLikesUseCase.getLikesForPost(
            LikeGroupFingerPrint(siteId, postId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList().firstOrNull() is Loading).isTrue
    }

    @Test
    fun `getLikesForPost does not emit Loading when requesting next page`() = test {
        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onPostLikesChanged(
                OnPostLikesChanged(
                    FetchPostLikes,
                    siteId,
                    postId,
                    false
                )
            )
        }

        val flow = getLikesUseCase.getLikesForPost(
            LikeGroupFingerPrint(siteId, postId, expectedNumLikes),
            PaginationParams(true, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList().firstOrNull() is Loading).isFalse
    }

    @Test
    fun `getLikesForPost emits no net failure when no net detected`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onPostLikesChanged(
                OnPostLikesChanged(
                    FetchPostLikes,
                    siteId,
                    postId,
                    false
                ).apply {
                    error = PostError("GENERIC_ERROR", "Error occurred")
                }
            )
        }

        val flow = getLikesUseCase.getLikesForPost(
            LikeGroupFingerPrint(siteId, postId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList()).isEqualTo(
            listOf(
                Loading,
                Failure(
                    failureType = NO_NETWORK,
                    error = UiStringRes(R.string.get_likes_no_network_error),
                    cachedLikes = listOf(),
                    emptyStateData = EmptyStateData(
                        showEmptyState = true,
                        title = UiStringRes(R.string.no_network_title)
                    ),
                    expectedNumLikes = expectedNumLikes,
                    hasMore = false,
                    pageInfo = pageInfo
                )
            )
        )
    }

    @Test
    fun `getLikesForPost emits failure when error detected`() = test {
        val errorMessage = "User not authorized"
        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onPostLikesChanged(
                OnPostLikesChanged(
                    FetchPostLikes,
                    siteId,
                    postId,
                    false
                ).apply {
                    error = PostError("UNAUTHORIZED", errorMessage)
                }
            )
        }

        val flow = getLikesUseCase.getLikesForPost(
            LikeGroupFingerPrint(siteId, postId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList()).isEqualTo(
            listOf(
                Loading,
                Failure(
                    failureType = GENERIC,
                    error = UiStringText(errorMessage),
                    cachedLikes = listOf(),
                    emptyStateData = EmptyStateData(
                        showEmptyState = true,
                        title = UiStringRes(R.string.get_likes_empty_state_title)
                    ),
                    expectedNumLikes = expectedNumLikes,
                    hasMore = false,
                    pageInfo = pageInfo
                )
            )
        )
    }

    @Test
    fun `getLikesForPost emits failure with default message when error has empty string`() = test {
        val errorMessage = ""
        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onPostLikesChanged(
                OnPostLikesChanged(
                    FetchPostLikes,
                    siteId,
                    postId,
                    false
                ).apply {
                    error = PostError("UNAUTHORIZED", errorMessage)
                }
            )
        }

        val flow = getLikesUseCase.getLikesForPost(
            LikeGroupFingerPrint(siteId, postId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList()).isEqualTo(
            listOf(
                Loading,
                Failure(
                    failureType = GENERIC,
                    error = UiStringRes(R.string.get_likes_unknown_error),
                    cachedLikes = listOf(),
                    emptyStateData = EmptyStateData(
                        showEmptyState = true,
                        title = UiStringRes(R.string.get_likes_empty_state_title)
                    ),
                    expectedNumLikes = expectedNumLikes,
                    hasMore = false,
                    pageInfo = pageInfo
                )
            )
        )
    }

    @Test
    fun `getLikesForPost emits likes data when available`() = test {
        val likeData = getDefaultLikers(expectedNumLikes, POST_LIKE, siteId, postId)

        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onPostLikesChanged(
                OnPostLikesChanged(
                    FetchPostLikes,
                    siteId,
                    postId,
                    false
                ).apply {
                    postLikes = likeData
                }
            )
        }

        val flow = getLikesUseCase.getLikesForPost(
            LikeGroupFingerPrint(siteId, postId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList()).isEqualTo(
            listOf(
                Loading,
                LikesData(
                    likes = likeData,
                    expectedNumLikes = expectedNumLikes,
                    hasMore = false,
                    pageInfo = pageInfo
                )
            )
        )
    }

    @Test
    fun `getLikesForPost emits likes data until limit`() = test {
        val likeData = getDefaultLikers(expectedNumLikes, POST_LIKE, siteId, postId)

        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onPostLikesChanged(
                OnPostLikesChanged(
                    FetchPostLikes,
                    siteId,
                    postId,
                    false
                ).apply {
                    postLikes = likeData
                }
            )
        }

        val flow = getLikesUseCase.getLikesForPost(
            LikeGroupFingerPrint(siteId, postId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList()).isEqualTo(
            listOf(
                Loading,
                LikesData(
                    likes = likeData.take(limitedPageLenght),
                    expectedNumLikes = expectedNumLikes,
                    hasMore = false,
                    pageInfo = pageInfo
                )
            )
        )
    }

    @Test
    fun `getLikesForComment emits no net failure when no net detected`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onCommentLikesChanged(
                OnCommentLikesChanged(
                    siteId,
                    commentId,
                    false
                ).apply {
                    causeOfChange = FETCHED_COMMENT_LIKES
                    error = CommentError(
                        CommentErrorType.AUTHORIZATION_REQUIRED,
                        "Error occurred"
                    )
                }
            )
        }

        val flow = getLikesUseCase.getLikesForComment(
            LikeGroupFingerPrint(siteId, commentId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList()).isEqualTo(
            listOf(
                Loading,
                Failure(
                    failureType = NO_NETWORK,
                    error = UiStringRes(R.string.get_likes_no_network_error),
                    cachedLikes = listOf(),
                    emptyStateData = EmptyStateData(
                        showEmptyState = true,
                        title = UiStringRes(R.string.no_network_title)
                    ),
                    expectedNumLikes = expectedNumLikes,
                    hasMore = false,
                    pageInfo = pageInfo
                )
            )
        )
    }

    @Test
    fun `getLikesForComment emits failure when error detected`() = test {
        val errorMessage = "User not authorized"
        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onCommentLikesChanged(
                OnCommentLikesChanged(
                    siteId,
                    commentId,
                    false
                ).apply {
                    causeOfChange = FETCHED_COMMENT_LIKES
                    error = CommentError(
                        CommentErrorType.AUTHORIZATION_REQUIRED,
                        errorMessage
                    )
                }
            )
        }

        val flow = getLikesUseCase.getLikesForComment(
            LikeGroupFingerPrint(siteId, commentId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList()).isEqualTo(
            listOf(
                Loading,
                Failure(
                    failureType = GENERIC,
                    error = UiStringText(errorMessage),
                    cachedLikes = listOf(),
                    emptyStateData = EmptyStateData(
                        showEmptyState = true,
                        title = UiStringRes(R.string.get_likes_empty_state_title)
                    ),
                    expectedNumLikes = expectedNumLikes,
                    hasMore = false,
                    pageInfo = pageInfo
                )
            )
        )
    }

    @Test
    fun `getLikesForComment emits failure with default message when error has empty string`() = test {
        val errorMessage = ""
        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onCommentLikesChanged(
                OnCommentLikesChanged(
                    siteId,
                    commentId,
                    false
                ).apply {
                    causeOfChange = FETCHED_COMMENT_LIKES
                    error = CommentError(
                        CommentErrorType.AUTHORIZATION_REQUIRED,
                        errorMessage
                    )
                }
            )
        }

        val flow = getLikesUseCase.getLikesForComment(
            LikeGroupFingerPrint(siteId, commentId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList()).isEqualTo(
            listOf(
                Loading,
                Failure(
                    failureType = GENERIC,
                    error = UiStringRes(R.string.get_likes_unknown_error),
                    cachedLikes = listOf(),
                    emptyStateData = EmptyStateData(
                        showEmptyState = true,
                        title = UiStringRes(R.string.get_likes_empty_state_title)
                    ),
                    expectedNumLikes = expectedNumLikes,
                    hasMore = false,
                    pageInfo = pageInfo
                )
            )
        )
    }

    @Test
    fun `getLikesForComment emits likes data when available`() = test {
        val likeData = getDefaultLikers(expectedNumLikes, COMMENT_LIKE, siteId, commentId)

        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onCommentLikesChanged(
                OnCommentLikesChanged(
                    siteId,
                    commentId,
                    false
                ).apply {
                    causeOfChange = FETCHED_COMMENT_LIKES
                    commentLikes = likeData
                }
            )
        }

        val flow = getLikesUseCase.getLikesForComment(
            LikeGroupFingerPrint(siteId, commentId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList()).isEqualTo(
            listOf(
                Loading,
                LikesData(
                    likes = likeData,
                    expectedNumLikes = expectedNumLikes,
                    hasMore = false,
                    pageInfo = pageInfo
                )
            )
        )
    }

    @Test
    fun `getLikesForComment emits likes data until limit`() = test {
        val likeData = getDefaultLikers(expectedNumLikes, COMMENT_LIKE, siteId, commentId)

        whenever(dispatcher.dispatch(any())).then {
            getLikesUseCase.onCommentLikesChanged(
                OnCommentLikesChanged(
                    siteId,
                    commentId,
                    false
                ).apply {
                    causeOfChange = FETCHED_COMMENT_LIKES
                    commentLikes = likeData
                }
            )
        }

        val flow = getLikesUseCase.getLikesForComment(
            LikeGroupFingerPrint(siteId, commentId, expectedNumLikes),
            PaginationParams(false, defaultPageLenght)
        )

        assertThat(flow.toList()).isNotEmpty
        assertThat(flow.toList()).isEqualTo(
            listOf(
                Loading,
                LikesData(
                    likes = likeData.take(limitedPageLenght),
                    expectedNumLikes = expectedNumLikes,
                    hasMore = false,
                    pageInfo = pageInfo
                )
            )
        )
    }
}
