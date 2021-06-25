package org.wordpress.android.ui.comments

import androidx.paging.CombinedLoadStates
import androidx.paging.LoadState
import androidx.paging.LoadState.Loading
import androidx.paging.LoadState.NotLoading
import androidx.paging.LoadStates
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runBlockingTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.MainCoroutineScopeRule
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.CommentsUiModel
import org.wordpress.android.ui.comments.unified.PagedListLoadingState
import org.wordpress.android.ui.comments.unified.PagedListLoadingState.EmptyError
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.CommentsListUiModel
import org.wordpress.android.ui.comments.unified.UnifiedCommentListViewModel.CommentsListUiModel.WithData
import org.wordpress.android.ui.comments.unified.toPagedListLoadingState
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DateTimeUtilsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper

@ExperimentalCoroutinesApi
class UnifiedCommentListViewModelTest : BaseUnitTest() {
    @Rule @JvmField val coroutineScopeRule = MainCoroutineScopeRule()

    @Mock lateinit var dateTimeUtilsWrapper: DateTimeUtilsWrapper
    @Mock lateinit var networkUtilsWrapper: NetworkUtilsWrapper

    private lateinit var viewModel: UnifiedCommentListViewModel

    @InternalCoroutinesApi
    @Before
    fun setUp() {
        viewModel = UnifiedCommentListViewModel(
                dateTimeUtilsWrapper,
                networkUtilsWrapper,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `SnackBar shown on loading error when there is content`() = runBlockingTest {
        val snackbars: MutableList<SnackbarMessageHolder> = mutableListOf()
        val uiState: MutableList<CommentsUiModel> = mutableListOf()

        val job = launch {
            viewModel.onSnackbarMessage.toList(snackbars)
            viewModel.uiState.toList(uiState)
        }

        viewModel.onLoadStateChanged(PagedListLoadingState.Error(Error("test error message")))

        assertThat(snackbars).hasSize(1)
        assertThat(snackbars.first().message).isEqualTo(UiStringText("test error message"))

        job.cancel()
    }

    @Test
    fun `error view shown on loading error when there is no content`() = runBlockingTest {
        val uiState: MutableList<CommentsUiModel> = mutableListOf()
        val expectedState =
                CommentsListUiModel.Empty(
                        UiStringText("test error message"),
                        R.drawable.img_illustration_empty_results_216dp
                )

        val job = launch {
            viewModel.uiState.toList(uiState)
        }

        viewModel.onLoadStateChanged(EmptyError(Error("test error message")))
        assertThat(uiState).hasSize(2)
        assertThat(uiState.last().commentsListUiModel).isEqualTo(expectedState)

        job.cancel()
    }

    @Test
    fun `default error message is used when no error message is specified`() = runBlockingTest {
        val uiState: MutableList<CommentsUiModel> = mutableListOf()
        val expectedState =
                CommentsListUiModel.Empty(
                        UiStringRes(string.error_refresh_comments),
                        R.drawable.img_illustration_empty_results_216dp
                )

        val job = launch {
            viewModel.uiState.toList(uiState)
        }

        viewModel.onLoadStateChanged(EmptyError(Error("")))
        assertThat(uiState).hasSize(2)
        assertThat(uiState.last().commentsListUiModel).isEqualTo(expectedState)

        job.cancel()
    }

    @Test
    fun `error view is not shown when error occurs and there is content`() = runBlockingTest {
        val uiState: MutableList<CommentsUiModel> = mutableListOf()

        val job = launch {
            viewModel.uiState.toList(uiState)
        }

        viewModel.onLoadStateChanged(PagedListLoadingState.Error(Error("test error message")))
        assertThat(uiState).hasSize(2)
        assertThat(uiState.last().commentsListUiModel).isInstanceOf(WithData::class.java)

        job.cancel()
    }

    @Test
    fun `when content is loading into an empty list, loading indicator is visible`() = runBlockingTest {
        val uiState: MutableList<CommentsUiModel> = mutableListOf()

        val job = launch {
            viewModel.uiState.toList(uiState)
        }

        viewModel.onLoadStateChanged(PagedListLoadingState.Loading)
        assertThat(uiState).hasSize(1) // Initial value is Load, so the subsequent load is ignored
        assertThat(uiState.last().commentsListUiModel).isInstanceOf(CommentsListUiModel.Loading::class.java)

        job.cancel()
    }

    @Test
    fun `when content is loading into a list with existing content, PTR indicator is visible`() = runBlockingTest {
        val uiState: MutableList<CommentsUiModel> = mutableListOf()

        val job = launch {
            viewModel.uiState.toList(uiState)
        }

        viewModel.onLoadStateChanged(PagedListLoadingState.Refreshing)
        assertThat(uiState).hasSize(2)
        assertThat(uiState.last().commentsListUiModel).isInstanceOf(CommentsListUiModel.Refreshing::class.java)

        job.cancel()
    }

    @Test
    fun `when there is not content during initial load, empty view is visible`() = runBlockingTest {
        val uiState: MutableList<CommentsUiModel> = mutableListOf()

        val expectedState = CommentsListUiModel.Empty(
                UiStringRes(string.comments_empty_list),
                R.drawable.img_illustration_empty_results_216dp
        )

        val job = launch {
            viewModel.uiState.toList(uiState)
        }

        viewModel.onLoadStateChanged(PagedListLoadingState.Empty)
        assertThat(uiState).hasSize(2)
        assertThat(uiState.last().commentsListUiModel).isEqualTo(expectedState)

        job.cancel()
    }

    @Test
    fun `list is visible whe it has content`() = runBlockingTest {
        val uiState: MutableList<CommentsUiModel> = mutableListOf()

        val job = launch {
            viewModel.uiState.toList(uiState)
        }

        viewModel.onLoadStateChanged(PagedListLoadingState.Idle)
        assertThat(uiState).hasSize(2)
        assertThat(uiState.last().commentsListUiModel).isInstanceOf(WithData::class.java)

        job.cancel()
    }

    @Test
    fun `toPagedListLoadingState correctly maps Error when there is content`() = runBlockingTest {
        val loadState = CombinedLoadStates(
                refresh = LoadState.Error(Error("test error message")),
                prepend = NotLoading(endOfPaginationReached = false),
                append = NotLoading(endOfPaginationReached = false),
                source = LoadStates(
                        refresh = NotLoading(endOfPaginationReached = false),
                        prepend = NotLoading(endOfPaginationReached = false),
                        append = NotLoading(endOfPaginationReached = false)
                )
        )

        val result = loadState.toPagedListLoadingState(true)

        assertThat(result).isInstanceOf(PagedListLoadingState.Error::class.java)
        assertThat((result as PagedListLoadingState.Error).throwable).isInstanceOf(Error::class.java)
        assertThat(result.throwable.message).isEqualTo("test error message")
    }

    @Test
    fun `toPagedListLoadingState correctly maps Error when encountering error during pagination`() = runBlockingTest {
        val loadState = CombinedLoadStates(
                refresh = NotLoading(endOfPaginationReached = false),
                prepend = NotLoading(endOfPaginationReached = false),
                append = LoadState.Error(Error("test error message")),
                source = LoadStates(
                        refresh = NotLoading(endOfPaginationReached = false),
                        prepend = NotLoading(endOfPaginationReached = false),
                        append = NotLoading(endOfPaginationReached = false)
                )
        )

        val result = loadState.toPagedListLoadingState(true)

        assertThat(result).isInstanceOf(PagedListLoadingState.Error::class.java)
        assertThat((result as PagedListLoadingState.Error).throwable).isInstanceOf(Error::class.java)
        assertThat(result.throwable.message).isEqualTo("test error message")
    }

    @Test
    fun `toPagedListLoadingState correctly maps EmptyError`() = runBlockingTest {
        val loadState = CombinedLoadStates(
                refresh = LoadState.Error(Error("test error message")),
                prepend = NotLoading(endOfPaginationReached = false),
                append = NotLoading(endOfPaginationReached = false),
                source = LoadStates(
                        refresh = NotLoading(endOfPaginationReached = false),
                        prepend = NotLoading(endOfPaginationReached = false),
                        append = NotLoading(endOfPaginationReached = false)
                )
        )

        val result = loadState.toPagedListLoadingState(false)

        assertThat(result).isInstanceOf(EmptyError::class.java)
        assertThat((result as EmptyError).throwable).isInstanceOf(Error::class.java)
        assertThat(result.throwable.message).isEqualTo("test error message")
    }

    @Test
    fun `toPagedListLoadingState correctly maps EmptyError without message`() = runBlockingTest {
        val loadState = CombinedLoadStates(
                refresh = LoadState.Error(Error("")),
                prepend = NotLoading(endOfPaginationReached = false),
                append = NotLoading(endOfPaginationReached = false),
                source = LoadStates(
                        refresh = NotLoading(endOfPaginationReached = false),
                        prepend = NotLoading(endOfPaginationReached = false),
                        append = NotLoading(endOfPaginationReached = false)
                )
        )

        val result = loadState.toPagedListLoadingState(false)

        assertThat(result).isInstanceOf(EmptyError::class.java)
        assertThat((result as EmptyError).throwable).isInstanceOf(Error::class.java)
        assertThat(result.throwable.message).isEqualTo("")
    }

    @Test
    fun `toPagedListLoadingState correctly maps Loading`() = runBlockingTest {
        val loadState = CombinedLoadStates(
                refresh = Loading,
                prepend = NotLoading(endOfPaginationReached = false),
                append = NotLoading(endOfPaginationReached = false),
                source = LoadStates(
                        refresh = Loading,
                        prepend = NotLoading(endOfPaginationReached = false),
                        append = NotLoading(endOfPaginationReached = false)
                )
        )

        val result = loadState.toPagedListLoadingState(false)

        assertThat(result).isInstanceOf(PagedListLoadingState.Loading::class.java)
    }

    @Test
    fun `toPagedListLoadingState correctly maps Refreshing`() = runBlockingTest {
        val loadState = CombinedLoadStates(
                refresh = Loading,
                prepend = NotLoading(endOfPaginationReached = false),
                append = NotLoading(endOfPaginationReached = false),
                source = LoadStates(
                        refresh = Loading,
                        prepend = NotLoading(endOfPaginationReached = false),
                        append = NotLoading(endOfPaginationReached = false)
                )
        )

        val result = loadState.toPagedListLoadingState(true)

        assertThat(result).isInstanceOf(PagedListLoadingState.Refreshing::class.java)
    }

    @Test
    fun `toPagedListLoadingState correctly maps Idle`() = runBlockingTest {
        val loadState = CombinedLoadStates(
                refresh = NotLoading(endOfPaginationReached = false),
                prepend = NotLoading(endOfPaginationReached = false),
                append = NotLoading(endOfPaginationReached = false),
                source = LoadStates(
                        refresh = NotLoading(endOfPaginationReached = false),
                        prepend = NotLoading(endOfPaginationReached = false),
                        append = NotLoading(endOfPaginationReached = false)
                )
        )

        val resultWithContent = loadState.toPagedListLoadingState(true)
        assertThat(resultWithContent).isInstanceOf(PagedListLoadingState.Idle::class.java)

        val resultWithoutContent = loadState.toPagedListLoadingState(false)
        assertThat(resultWithoutContent).isInstanceOf(PagedListLoadingState.Idle::class.java)
    }

    @Test
    fun `toPagedListLoadingState correctly maps Idle for a list with no more content to load`() = runBlockingTest {
        val loadState = CombinedLoadStates(
                refresh = NotLoading(endOfPaginationReached = true),
                prepend = NotLoading(endOfPaginationReached = false),
                append = NotLoading(endOfPaginationReached = false),
                source = LoadStates(
                        refresh = NotLoading(endOfPaginationReached = true),
                        prepend = NotLoading(endOfPaginationReached = false),
                        append = NotLoading(endOfPaginationReached = false)
                )
        )

        val result = loadState.toPagedListLoadingState(true)
        assertThat(result).isInstanceOf(PagedListLoadingState.Idle::class.java)
    }

    @Test
    fun `toPagedListLoadingState correctly maps Empty for a list with no content`() = runBlockingTest {
        val loadState = CombinedLoadStates(
                refresh = NotLoading(endOfPaginationReached = false),
                prepend = NotLoading(endOfPaginationReached = false),
                append = NotLoading(endOfPaginationReached = true),
                source = LoadStates(
                        refresh = NotLoading(endOfPaginationReached = false),
                        prepend = NotLoading(endOfPaginationReached = false),
                        append = NotLoading(endOfPaginationReached = true)
                )
        )

        val result = loadState.toPagedListLoadingState(false)
        assertThat(result).isInstanceOf(PagedListLoadingState.Empty::class.java)
    }
}
