package org.wordpress.android.ui.engagement

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.LikeModel.LikeType.COMMENT_LIKE
import org.wordpress.android.fluxc.model.LikeModel.LikeType.POST_LIKE
import org.wordpress.android.ui.engagement.GetLikesUseCase.FailureType
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.Failure.EmptyStateData
import org.wordpress.android.ui.engagement.GetLikesUseCase.GetLikesState.LikesData
import org.wordpress.android.ui.engagement.GetLikesUseCase.LikeGroupFingerPrint
import org.wordpress.android.ui.engagement.GetLikesUseCase.PaginationParams
import org.wordpress.android.ui.engagement.GetLikesUseCase.PagingInfo
import org.wordpress.android.ui.engagement.utils.getDefaultLikers
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringText

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class GetLikesHandlerTest : BaseUnitTest() {
    @Mock lateinit var getLikesUseCase: GetLikesUseCase

    private lateinit var getLikesHandler: GetLikesHandler
    private var likesState: GetLikesState? = null
    private var holder: SnackbarMessageHolder? = null

    private val siteId = 100L
    private val postId = 1000L
    private val commentId = 10000L
    private val expectedNumLikes = 6
    private val defaultPageLenght = 20
    private val pageInfo = PagingInfo(
            20,
            1
    )

    @Before
    fun setup() {
        getLikesHandler = GetLikesHandler(
                getLikesUseCase,
                coroutinesTestRule.testDispatcher
        )
    }

    @Test
    fun `handleGetLikesForPost collects expected state`() = test {
        val fingerPrint = LikeGroupFingerPrint(siteId, postId, expectedNumLikes)
        val paginationParams = PaginationParams(false, defaultPageLenght)
        val likesData = getDefaultLikers(expectedNumLikes, POST_LIKE, siteId, postId)

        val state = LikesData(
                likes = likesData,
                expectedNumLikes = expectedNumLikes,
                hasMore = false,
                pageInfo = pageInfo
        )

        whenever(getLikesUseCase.getLikesForPost(fingerPrint, paginationParams)).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        getLikesHandler.handleGetLikesForPost(
                fingerPrint,
                paginationParams.requestNextPage,
                paginationParams.pageLength
        )

        requireNotNull(likesState).let {
            assertThat(it).isEqualTo(state)
        }

        assertThat(holder).isNull()
    }

    @Test
    fun `handleGetLikesForPost forwards failures signaling to snackbar`() = test {
        val error = UiStringText("An error occurred")
        val fingerPrint = LikeGroupFingerPrint(siteId, postId, expectedNumLikes)
        val paginationParams = PaginationParams(false, defaultPageLenght)
        val likesData = getDefaultLikers(expectedNumLikes, POST_LIKE, siteId, postId)

        val state = Failure(
                failureType = FailureType.GENERIC,
                error = error,
                cachedLikes = likesData,
                emptyStateData = EmptyStateData(false),
                expectedNumLikes = expectedNumLikes,
                hasMore = false,
                pageInfo = pageInfo
        )

        whenever(getLikesUseCase.getLikesForPost(fingerPrint, paginationParams)).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        getLikesHandler.handleGetLikesForPost(
                fingerPrint,
                paginationParams.requestNextPage,
                paginationParams.pageLength
        )

        requireNotNull(likesState).let {
            assertThat(it).isEqualTo(state)
        }

        requireNotNull(holder).let {
            assertThat(it.message).isEqualTo(error)
        }
    }

    @Test
    fun `handleGetLikesForComment collects expected state`() = test {
        val fingerPrint = LikeGroupFingerPrint(siteId, commentId, expectedNumLikes)
        val paginationParams = PaginationParams(false, defaultPageLenght)
        val likesData = getDefaultLikers(expectedNumLikes, COMMENT_LIKE, siteId, commentId)

        val state = LikesData(
                likes = likesData,
                expectedNumLikes = expectedNumLikes,
                hasMore = false,
                pageInfo = pageInfo
        )

        whenever(getLikesUseCase.getLikesForComment(fingerPrint, paginationParams)).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        getLikesHandler.handleGetLikesForComment(
                fingerPrint,
                paginationParams.requestNextPage,
                paginationParams.pageLength
        )

        requireNotNull(likesState).let {
            assertThat(it).isEqualTo(state)
        }

        assertThat(holder).isNull()
    }

    @Test
    fun `handleGetLikesForComment forwards failures signaling to snackbar`() = test {
        val error = UiStringText("An error occurred")
        val fingerPrint = LikeGroupFingerPrint(siteId, commentId, expectedNumLikes)
        val paginationParams = PaginationParams(false, defaultPageLenght)
        val likesData = getDefaultLikers(expectedNumLikes, COMMENT_LIKE, siteId, commentId)

        val state = Failure(
                failureType = FailureType.GENERIC,
                error = error,
                cachedLikes = likesData,
                emptyStateData = EmptyStateData(false),
                expectedNumLikes = expectedNumLikes,
                hasMore = false,
                pageInfo = pageInfo
        )

        whenever(getLikesUseCase.getLikesForComment(fingerPrint, paginationParams)).thenReturn(
                flow { emit(state) }
        )

        setupObservers()

        getLikesHandler.handleGetLikesForComment(
                fingerPrint,
                paginationParams.requestNextPage,
                paginationParams.pageLength
        )

        requireNotNull(likesState).let {
            assertThat(it).isEqualTo(state)
        }

        requireNotNull(holder).let {
            assertThat(it.message).isEqualTo(error)
        }
    }

    @Test
    fun `clear calls clear on use case`() {
        getLikesHandler.clear()

        verify(getLikesUseCase, times(1)).clear()
    }

    private fun setupObservers() {
        likesState = null

        getLikesHandler.likesStatusUpdate.observeForever {
            likesState = it
        }

        holder = null

        getLikesHandler.snackbarEvents.observeForever { event ->
            event.applyIfNotHandled {
                holder = this
            }
        }
    }
}
