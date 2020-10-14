package org.wordpress.android.ui.reader.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.InternalCoroutinesApi

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.datasets.wrappers.ReaderPostTableWrapper
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionsHandler
import org.wordpress.android.ui.reader.discover.ReaderPostMoreButtonUiStateBuilder
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostDetailViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ReaderPostDetailViewModel

    @Mock private lateinit var readerPostCardActionsHandler: ReaderPostCardActionsHandler
    @Mock private lateinit var readerUtilsWrapper: ReaderUtilsWrapper
    @Mock private lateinit var postDetailsUiStateBuilder: ReaderPostDetailUiStateBuilder
    @Mock private lateinit var readerPostTableWrapper: ReaderPostTableWrapper
    @Mock private lateinit var menuUiStateBuilder: ReaderPostMoreButtonUiStateBuilder

    @Before
    fun setUp() {
        viewModel = ReaderPostDetailViewModel(
                readerPostCardActionsHandler,
                readerUtilsWrapper,
                readerPostTableWrapper,
                menuUiStateBuilder,
                postDetailsUiStateBuilder,
                TEST_DISPATCHER,
                TEST_DISPATCHER
        )
    }

    @Test
    fun `blank test`() {
    }
}
