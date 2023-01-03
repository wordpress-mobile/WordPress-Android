package org.wordpress.android.viewmodel.comments

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.`when`
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.CommentStore.CommentError
import org.wordpress.android.fluxc.store.CommentStore.CommentErrorType.GENERIC_ERROR
import org.wordpress.android.fluxc.store.CommentsStore
import org.wordpress.android.fluxc.store.CommentsStore.CommentsActionPayload
import org.wordpress.android.fluxc.store.CommentsStore.CommentsData.PagingData
import org.wordpress.android.models.usecases.BatchModerateCommentsUseCase
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateHandler
import org.wordpress.android.models.usecases.LocalCommentCacheUpdateUseCase
import org.wordpress.android.models.usecases.ModerateCommentWithUndoUseCase
import org.wordpress.android.models.usecases.ModerateCommentsResourceProvider
import org.wordpress.android.models.usecases.PaginateCommentsResourceProvider
import org.wordpress.android.models.usecases.PaginateCommentsUseCase
import org.wordpress.android.models.usecases.UnifiedCommentsListHandler
import org.wordpress.android.ui.comments.unified.CommentFilter.ALL
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.CommentList
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.CommentsListUiModel
import org.wordpress.android.ui.comments.unified.CommentListUiModelHelper.CommentsUiModel
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.Comment
import org.wordpress.android.ui.comments.unified.UnifiedCommentListItem.NextPageLoader
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel
import org.wordpress.android.ui.comments.utils.testComments
import org.wordpress.android.ui.comments.utils.testCommentsPayload30
import org.wordpress.android.ui.comments.utils.testCommentsPayload60
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@ExperimentalCoroutinesApi
class UnifiedCommentListViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: UnifiedCommentListViewModel
    private lateinit var unifiedCommentsListHandler: UnifiedCommentsListHandler
    private lateinit var commentListUiModelHelper: CommentListUiModelHelper

    @Mock
    private lateinit var selectedSiteRepository: SelectedSiteRepository

    @Mock
    private lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    @Mock
    private lateinit var analyticsTrackerWrapper: AnalyticsTrackerWrapper

    @Mock
    private lateinit var resourceProvider: ResourceProvider

    @Mock
    private lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper

    @Mock
    private lateinit var paginateCommentsResourceProvider: PaginateCommentsResourceProvider

    @Mock
    private lateinit var moderateCommentsResourceProvider: ModerateCommentsResourceProvider

    @Mock
    private lateinit var commentStore: CommentsStore
    private lateinit var localCommentCacheUpdateHandler: LocalCommentCacheUpdateHandler
    private lateinit var paginateCommentsUseCase: PaginateCommentsUseCase
    private lateinit var batchModerateCommentsUseCase: BatchModerateCommentsUseCase

    private lateinit var moderationWithUndoUseCase: ModerateCommentWithUndoUseCase
    private lateinit var localCommentCacheUpdateUseCase: LocalCommentCacheUpdateUseCase

    val site = SiteModel().also { it.id = 5 }.also { it.name = "Test Site" }

    @Before
    fun setUp() = test {
        whenever(dateTimeUtilsWrapper.javaDateToTimeSpan(anyOrNull())).thenReturn("Apr 19")
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        whenever(paginateCommentsResourceProvider.commentsStore).thenReturn(commentStore)
        whenever(paginateCommentsResourceProvider.networkUtilsWrapper).thenReturn(networkUtilsWrapper)
        whenever(paginateCommentsResourceProvider.resourceProvider).thenReturn(resourceProvider)
        whenever(selectedSiteRepository.getSelectedSite()).thenReturn(site)
        whenever(resourceProvider.getString(string.no_network_message)).thenReturn("No network")

        batchModerateCommentsUseCase = BatchModerateCommentsUseCase(moderateCommentsResourceProvider)
        moderationWithUndoUseCase = ModerateCommentWithUndoUseCase(moderateCommentsResourceProvider)
        paginateCommentsUseCase = PaginateCommentsUseCase(paginateCommentsResourceProvider)
        unifiedCommentsListHandler = UnifiedCommentsListHandler(
            paginateCommentsUseCase,
            batchModerateCommentsUseCase,
            moderationWithUndoUseCase
        )

        localCommentCacheUpdateUseCase = LocalCommentCacheUpdateUseCase()
        localCommentCacheUpdateHandler = LocalCommentCacheUpdateHandler(localCommentCacheUpdateUseCase)

        `when`(commentStore.fetchCommentsPage(any(), any(), eq(0), any(), any()))
            .thenReturn(testCommentsPayload30)
        `when`(commentStore.fetchCommentsPage(any(), any(), eq(30), any(), any()))
            .thenReturn(testCommentsPayload60)

        commentListUiModelHelper = CommentListUiModelHelper(resourceProvider, dateTimeUtilsWrapper, networkUtilsWrapper)

        viewModel = UnifiedCommentListViewModel(
            commentListUiModelHelper,
            selectedSiteRepository,
            networkUtilsWrapper,
            analyticsTrackerWrapper,
            testDispatcher(),
            testDispatcher(),
            unifiedCommentsListHandler,
            localCommentCacheUpdateHandler
        )
    }

    // Comment list state test

    @Test
    fun `when VM starts list shows loading screen and then comments`() = test {
        val result = mutableListOf<CommentsUiModel>()

        val job = launch {
            viewModel.uiState.toList(result)
        }

        viewModel.start(ALL)
        advanceUntilIdle()

        val intialState = result.first()

        assert(intialState.commentData == CommentList(emptyList(), false))
        assert(intialState.commentsListUiModel == CommentsListUiModel.Loading)

        val stateWithData = result[1]

        assertThat(stateWithData.commentsListUiModel).isEqualTo(CommentsListUiModel.WithData)
        assertThat(stateWithData.commentData.comments).isNotEmpty()
        assertThat(stateWithData.commentData.comments.filterIsInstance<Comment>()).size().isEqualTo(30)

        job.cancel()
    }

    @Test
    fun `reloading comment list shows PTR indicator and loads comments`() = test {
        val result = mutableListOf<CommentsUiModel>()

        val job = launch {
            viewModel.uiState.toList(result)
        }

        viewModel.start(ALL)
        viewModel.reload()
        advanceUntilIdle()

        val afterPtrState = result[1]
        assertThat(afterPtrState.commentsListUiModel).isEqualTo(CommentsListUiModel.WithData)
        assertThat(afterPtrState.commentData.comments.filterIsInstance<Comment>()).size().isEqualTo(30)

        job.cancel()
    }

    @Test
    fun `calling reload shows error toast when network is not available`() = test {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        viewModel.start(ALL)

        val result = ArrayList<SnackbarMessageHolder>()
        val job = launch {
            viewModel.onSnackbarMessage.collectLatest {
                result.add(it)
            }
        }

        viewModel.reload()

        assert(result.size == 1)
        assert(result.first().message == UiStringRes(string.no_network_message))

        job.cancel()
    }

    // Paging Test

    @Test
    fun `comment list contains load more row when there are more comments`() = test {
        whenever((commentStore.fetchCommentsPage(any(), any(), any(), any(), any())))
            .thenReturn(CommentsActionPayload(PagingData(comments = testComments.take(30), hasMore = true)))

        val result = mutableListOf<CommentsUiModel>()
        val job = launch {
            viewModel.uiState.collectLatest {
                result.add(it)
            }
        }
        viewModel.start(ALL)
        advanceUntilIdle()

        val stateWithData = result[1]
        val loadMoreFooter = stateWithData.commentData.comments.last()
        assertThat(loadMoreFooter).isInstanceOf(NextPageLoader::class.java)
        assertThat((loadMoreFooter as NextPageLoader).isLoading).isTrue()

        job.cancel()
    }

    @Test
    fun `comment list does not contain load more row when there are no more comments`() = test {
        whenever(commentStore.fetchCommentsPage(any(), any(), any(), any(), any())).thenReturn(
            CommentsActionPayload(
                PagingData(comments = testComments.take(15), hasMore = false)
            )
        )

        val result = mutableListOf<CommentsUiModel>()
        val job = launch {
            viewModel.uiState.collectLatest {
                result.add(it)
            }
        }
        viewModel.start(ALL)
        advanceUntilIdle()

        val stateWithData = result[1]
        assertThat(stateWithData.commentData.comments.any { it is NextPageLoader }).isFalse()

        job.cancel()
    }

    @Test
    fun `when load more action is triggered more comments are loaded`() = test {
        val result = mutableListOf<CommentsUiModel>()
        val job = launch {
            viewModel.uiState.collectLatest {
                result.add(it)
            }
        }
        viewModel.start(ALL)
        advanceUntilIdle()

        val stateWithData = result[1]

        val loadMoreFooter = stateWithData.commentData.comments.last()
        assertThat(loadMoreFooter).isInstanceOf(NextPageLoader::class.java)
        assertThat((loadMoreFooter as NextPageLoader).isLoading).isTrue()

        loadMoreFooter.loadAction.invoke()

        val stateWithSecondPage = result[2]
        assertThat(stateWithSecondPage.commentsListUiModel).isEqualTo(CommentsListUiModel.WithData)
        assertThat(stateWithSecondPage.commentData.comments.filterIsInstance<Comment>()).size().isEqualTo(60)

        job.cancel()
    }

    // Paging and Footer error tests

    @Test
    fun `footer shows retry button and error snackbar when there is an error when loading more`() = test {
        val result = mutableListOf<CommentsUiModel>()
        val job = launch {
            viewModel.uiState.collectLatest {
                result.add(it)
            }
        }

        val snackbarResults = mutableListOf<SnackbarMessageHolder>()
        val snackbarJob = launch {
            viewModel.onSnackbarMessage.collectLatest {
                snackbarResults.add(it)
            }
        }

        viewModel.start(ALL)
        advanceUntilIdle()

        val stateWithData = result[1]

        val loadMoreFooter = stateWithData.commentData.comments.last()
        assertThat(loadMoreFooter).isInstanceOf(NextPageLoader::class.java)

        whenever(commentStore.fetchCommentsPage(any(), any(), eq(30), any(), any()))
            .thenReturn(
                CommentsActionPayload(
                    CommentError(GENERIC_ERROR, "test error message"),
                    PagingData(comments = testComments.take(30), hasMore = true)
                )
            )

        (loadMoreFooter as NextPageLoader).loadAction.invoke()

        // since we got error there should be no extra data in the list
        val stateWithSecondPage = result[2]
        assertThat(stateWithSecondPage.commentsListUiModel).isInstanceOf(CommentsListUiModel.WithData::class.java)
        assertThat(stateWithSecondPage.commentData.comments.filterIsInstance<Comment>()).size().isEqualTo(30)

        // error footer should not be in loading state
        val errorFooter = stateWithSecondPage.commentData.comments.last()
        assertThat(errorFooter).isInstanceOf(NextPageLoader::class.java)
        assertThat((errorFooter as NextPageLoader).isLoading).isFalse()

        assertThat(snackbarResults.first().message).isEqualTo(UiStringText("test error message"))

        job.cancel()
        snackbarJob.cancel()
    }

    @Test
    fun `tapping retry button in footer shows error snackbar when there is no internet`() = test {
        whenever(commentStore.getCommentsForSite(anyOrNull(), anyOrNull(), anyOrNull(), anyOrNull()))
            .thenReturn(testComments.take(30))

        val result = mutableListOf<CommentsUiModel>()
        val job = launch {
            viewModel.uiState.collectLatest {
                result.add(it)
            }
        }

        val snackbarResults = mutableListOf<SnackbarMessageHolder>()
        val snackbarJob = launch {
            viewModel.onSnackbarMessage.collectLatest {
                snackbarResults.add(it)
            }
        }

        viewModel.start(ALL)
        advanceUntilIdle()

        val stateWithData = result[1]

        val loadMoreFooter = stateWithData.commentData.comments.last()
        assertThat(loadMoreFooter).isInstanceOf(NextPageLoader::class.java)

        whenever(commentStore.fetchCommentsPage(any(), any(), eq(30), any(), any()))
            .thenReturn(
                CommentsActionPayload(
                    CommentError(GENERIC_ERROR, "test error message"),
                    PagingData(comments = testComments.take(30), hasMore = true)
                )
            )

        // produce error footer
        (loadMoreFooter as NextPageLoader).loadAction.invoke()

        val stateWithSecondPage = result[2]

        // error footer should not be in loading state
        val errorFooter = stateWithSecondPage.commentData.comments.last()
        assertThat(errorFooter).isInstanceOf(NextPageLoader::class.java)
        assertThat((errorFooter as NextPageLoader).isLoading).isFalse()

        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)

        errorFooter.loadAction.invoke()

        val noInternetSnackbar = snackbarResults[1] // first one is test error snackabr

        assertThat(noInternetSnackbar.message).isEqualTo(UiStringText("No network"))

        job.cancel()
        snackbarJob.cancel()
    }
}
