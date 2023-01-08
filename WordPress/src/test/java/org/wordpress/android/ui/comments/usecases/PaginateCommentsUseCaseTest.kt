package org.wordpress.android.ui.comments.usecases

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.CommentsStore.CommentsActionPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.PagingData
import org.wordpress.android.models.usecases.CommentsUseCaseType
import org.wordpress.android.models.usecases.CommentsUseCaseType.PAGINATE_USE_CASE
import org.wordpress.android.models.usecases.PaginateCommentsResourceProvider
import org.wordpress.android.models.usecases.PaginateCommentsUseCase
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnGetPage
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.PaginateCommentsAction.OnReloadFromCache
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.GetPageParameters
import org.wordpress.android.models.usecases.PaginateCommentsUseCase.Parameters.ReloadFromCacheParameters
import org.wordpress.android.ui.comments.unified.CommentFilter.ALL
import org.wordpress.android.ui.comments.unified.CommentFilter.PENDING
import org.wordpress.android.ui.comments.unified.CommentFilter.UNREPLIED
import org.wordpress.android.ui.comments.unified.UnrepliedCommentsUtils
import org.wordpress.android.ui.comments.utils.testComments
import org.wordpress.android.ui.comments.utils.testCommentsPayload30
import org.wordpress.android.ui.comments.utils.testCommentsPayload60
import org.wordpress.android.ui.comments.utils.testCommentsPayloadLastPage
import org.wordpress.android.usecase.UseCaseResult
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class PaginateCommentsUseCaseTest : BaseUnitTest() {
    @Mock private lateinit var commentStore: CommentsStore
    @Mock private lateinit var paginateCommentsResourceProvider: PaginateCommentsResourceProvider
    @Mock private lateinit var unrepliedCommentsUtils: UnrepliedCommentsUtils
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var paginateCommentsUseCase: PaginateCommentsUseCase

    val site = SiteModel().also { it.id = 5 }.also { it.name = "Test Site" }

    @Before
    fun setup() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(paginateCommentsResourceProvider.commentsStore).thenReturn(commentStore)
        whenever(paginateCommentsResourceProvider.unrepliedCommentsUtils).thenReturn(unrepliedCommentsUtils)
        whenever(paginateCommentsResourceProvider.networkUtilsWrapper).thenReturn(networkUtilsWrapper)

        `when`(commentStore.fetchCommentsPage(eq(site), any(), eq(0), any(), any()))
                .thenReturn(testCommentsPayload30)
        `when`(commentStore.fetchCommentsPage(eq(site), any(), eq(30), any(), any()))
                .thenReturn(testCommentsPayload60)
        `when`(commentStore.fetchCommentsPage(eq(site), any(), eq(60), any(), any()))
                .thenReturn(testCommentsPayloadLastPage)
        `when`(commentStore.getCachedComments(eq(site), any(), any()))
                .thenReturn(testCommentsPayload60)

        paginateCommentsUseCase = PaginateCommentsUseCase(paginateCommentsResourceProvider)
    }

    // getting a page with comments

    @Test
    fun `comment store is called when requesting comments`() = test {
        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 30, 0, ALL)))

        verify(commentStore, times(1)).fetchCommentsPage(
                site,
                30,
                0,
                ALL.toCommentStatus(),
                ALL.toCommentCacheStatuses()
        )

        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 40, 30, PENDING)))

        verify(commentStore, times(1)).fetchCommentsPage(
                site,
                40,
                30,
                PENDING.toCommentStatus(),
                PENDING.toCommentCacheStatuses()
        )
    }

    @Test
    fun `comments are filtered when they are requested with unreplied filter`() = test {
        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 30, 0, ALL)))

        verify(unrepliedCommentsUtils, times(0)).getUnrepliedComments(any())

        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 30, 0, UNREPLIED)))

        verify(unrepliedCommentsUtils, times(1)).getUnrepliedComments(any())
    }

    @Test
    fun `getting a page delivers result when there are no errors`() = test {
        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 30, 0, ALL)))

        val dataResult = result[1] // first one is loading

        assertThat(dataResult).isInstanceOf(UseCaseResult.Success::class.java)
        assertThat(dataResult.type).isEqualTo(PAGINATE_USE_CASE)
        assertThat((dataResult as UseCaseResult.Success).data).isInstanceOf(PagingData::class.java)

        val commentsPayload = dataResult.data

        assertThat(commentsPayload.hasMore).isTrue
        assertThat(commentsPayload.comments).isEqualTo(testComments.take(30))
        job.cancel()
    }

    @Test
    fun `getting a second page delivers result when there are no errors`() = test {
        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 30, 30, ALL)))

        val dataResult = result[0]

        assertThat(dataResult).isInstanceOf(UseCaseResult.Success::class.java)
        assertThat(dataResult.type).isEqualTo(PAGINATE_USE_CASE)
        assertThat((dataResult as UseCaseResult.Success).data).isInstanceOf(PagingData::class.java)

        val commentsPayload = dataResult.data

        assertThat(commentsPayload.hasMore).isTrue
        assertThat(commentsPayload.comments).isEqualTo(testComments.take(60))
        job.cancel()
    }

    @Test
    fun `getting last page correctly indicates that there are no more comments`() = test {
        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 30, 60, ALL)))

        val dataResult = result[0]

        assertThat(dataResult).isInstanceOf(UseCaseResult.Success::class.java)
        assertThat(dataResult.type).isEqualTo(PAGINATE_USE_CASE)
        assertThat((dataResult as UseCaseResult.Success).data).isInstanceOf(PagingData::class.java)

        val commentsPayload = dataResult.data

        assertThat(commentsPayload.hasMore).isFalse
        assertThat(commentsPayload.comments).isEqualTo(testComments.take(90))
        job.cancel()
    }

    @Test
    fun `getting first page (offset = 0) emits loading`() = test {
        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 30, 0, ALL)))

        val loadingEvent = result.first()

        assertThat(loadingEvent).isInstanceOf(UseCaseResult.Loading::class.java)
        assertThat(loadingEvent.type).isEqualTo(PAGINATE_USE_CASE)

        job.cancel()
    }

    @Test
    fun `getting second+ page (offset is more than 0) does not emits loading`() = test {
        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 30, 30, ALL)))

        assertThat(result.any { it is UseCaseResult.Loading }).isFalse
        job.cancel()
    }

    @Test
    fun `encountering error without any cache emits error event without cache`() = test {
        val error = CommentError(GENERIC_ERROR, "test error message")
        val cachedData = PagingData.empty()

        whenever(commentStore.fetchCommentsPage(any(), any(), eq(0), any(), any()))
                .thenReturn(
                        CommentsActionPayload(
                                error,
                                cachedData
                        )
                )

        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 30, 0, ALL)))

        val errorResult = result[1] // first one is loading

        assertThat(errorResult).isInstanceOf(UseCaseResult.Failure::class.java)
        assertThat(errorResult.type).isEqualTo(PAGINATE_USE_CASE)
        assertThat((errorResult as UseCaseResult.Failure).cachedData).isEqualTo(cachedData)
        assertThat(errorResult.error).isEqualTo(error)

        job.cancel()
    }

    @Test
    fun `encountering error with cache emits error event with cache`() = test {
        val error = CommentError(GENERIC_ERROR, "test error message")
        val cachedData = PagingData(comments = testComments.take(30), hasMore = true)

        whenever(commentStore.fetchCommentsPage(any(), any(), eq(0), any(), any()))
                .thenReturn(
                        CommentsActionPayload(
                                error,
                                cachedData
                        )
                )

        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(OnGetPage(GetPageParameters(site, 30, 0, ALL)))

        val errorResult = result[1] // first one is loading

        assertThat(errorResult).isInstanceOf(UseCaseResult.Failure::class.java)
        assertThat(errorResult.type).isEqualTo(PAGINATE_USE_CASE)
        assertThat((errorResult as UseCaseResult.Failure).cachedData).isEqualTo(cachedData)
        assertThat(errorResult.error).isEqualTo(error)

        job.cancel()
    }

    // getting a page with comments from local cache

    @Test
    fun `comment store is called when requesting cached comments`() = test {
        paginateCommentsUseCase.manageAction(
                OnReloadFromCache(
                        ReloadFromCacheParameters(
                                GetPageParameters(
                                        site,
                                        30,
                                        0,
                                        ALL
                                ), true
                        )
                )
        )

        verify(commentStore, times(1)).getCachedComments(
                site,
                ALL.toCommentCacheStatuses(),
                true
        )

        paginateCommentsUseCase.manageAction(
                OnReloadFromCache(
                        ReloadFromCacheParameters(
                                GetPageParameters(
                                        site, 430,
                                        0,
                                        PENDING
                                ), false
                        )
                )
        )

        verify(commentStore, times(1)).getCachedComments(
                site,
                PENDING.toCommentCacheStatuses(),
                false
        )
    }

    @Test
    fun `getting cache does not emit loading events`() = test {
        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(
                OnReloadFromCache(
                        ReloadFromCacheParameters(
                                GetPageParameters(
                                        site,
                                        30,
                                        0,
                                        ALL
                                ), true
                        )
                )
        )

        paginateCommentsUseCase.manageAction(
                OnReloadFromCache(
                        ReloadFromCacheParameters(
                                GetPageParameters(
                                        site,
                                        30,
                                        30,
                                        ALL
                                ), true
                        )
                )
        )

        assertThat(result.any { it is UseCaseResult.Loading }).isFalse

        job.cancel()
    }

    @Test
    fun `getting cache delivers all the cache regardless of GetPageParameters, when there are no errors`() = test {
        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(
                OnReloadFromCache(
                        ReloadFromCacheParameters(
                                GetPageParameters(
                                        site,
                                        30,
                                        0,
                                        ALL
                                ), true
                        )
                )
        )

        val dataResult = result[0]

        assertThat(dataResult).isInstanceOf(UseCaseResult.Success::class.java)
        assertThat(dataResult.type).isEqualTo(PAGINATE_USE_CASE)
        assertThat((dataResult as UseCaseResult.Success).data).isInstanceOf(PagingData::class.java)

        val commentsPayload = dataResult.data

        assertThat(commentsPayload.hasMore).isTrue
        assertThat(commentsPayload.comments).isEqualTo(testComments.take(60))
        job.cancel()
    }

    @Test
    fun `encountering error when getting cache emits error event without cache when there are no cache`() = test {
        val error = CommentError(GENERIC_ERROR, "test error message")
        val cachedData = PagingData.empty()

        whenever(commentStore.getCachedComments(any(), any(), any()))
                .thenReturn(
                        CommentsActionPayload(
                                error,
                                cachedData
                        )
                )

        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(
                OnReloadFromCache(
                        ReloadFromCacheParameters(
                                GetPageParameters(
                                        site,
                                        30,
                                        0,
                                        ALL
                                ), true
                        )
                )
        )

        val errorResult = result[0]

        assertThat(errorResult).isInstanceOf(UseCaseResult.Failure::class.java)
        assertThat(errorResult.type).isEqualTo(PAGINATE_USE_CASE)
        assertThat((errorResult as UseCaseResult.Failure).cachedData).isEqualTo(cachedData)
        assertThat(errorResult.error).isEqualTo(error)

        job.cancel()
    }

    @Test
    fun `encountering error when getting cache emits error event with cache when there is cache`() = test {
        val error = CommentError(GENERIC_ERROR, "test error message")
        val cachedData = PagingData(comments = testComments.take(60), hasMore = true)

        whenever(commentStore.getCachedComments(any(), any(), any()))
                .thenReturn(
                        CommentsActionPayload(
                                error,
                                cachedData
                        )
                )

        val result = mutableListOf<UseCaseResult<CommentsUseCaseType, CommentError, PagingData>>()

        val job = launch {
            paginateCommentsUseCase.subscribe().collectLatest {
                result.add(it)
            }
        }

        paginateCommentsUseCase.manageAction(
                OnReloadFromCache(
                        ReloadFromCacheParameters(
                                GetPageParameters(
                                        site,
                                        30,
                                        0,
                                        ALL
                                ), true
                        )
                )
        )

        val errorResult = result[0]

        assertThat(errorResult).isInstanceOf(UseCaseResult.Failure::class.java)
        assertThat(errorResult.type).isEqualTo(PAGINATE_USE_CASE)
        assertThat((errorResult as UseCaseResult.Failure).cachedData).isEqualTo(cachedData)
        assertThat(errorResult.error).isEqualTo(error)

        job.cancel()
    }
}
