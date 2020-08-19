package org.wordpress.android.ui.reader.repository.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.nhaarman.mockitokotlin2.anyOrNull
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import kotlinx.coroutines.CoroutineScope
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.anyLong
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.R
import org.wordpress.android.test
import org.wordpress.android.ui.reader.actions.ReaderActions.ActionListener
import org.wordpress.android.ui.reader.actions.ReaderBlogActions.BlockedBlogResult
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class BlockSiteUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var blockSiteUseCase: BlockSiteUseCase
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var analyticsUtilsWrapper: AnalyticsUtilsWrapper
    @Mock private lateinit var readerBlogActionsWrapper: ReaderBlogActionsWrapper

    @Before
    fun setUp() {
        blockSiteUseCase = BlockSiteUseCase(networkUtilsWrapper, analyticsUtilsWrapper, readerBlogActionsWrapper)
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
    }

    private fun <T> testWithSuccess(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(readerBlogActionsWrapper.blockBlogFromReader(anyLong(), anyOrNull())).thenAnswer {
                (it.getArgument(1) as ActionListener).onActionResult(true)
                BlockedBlogResult()
            }
            block()
        }
    }

    private fun <T> testWithError(block: suspend CoroutineScope.() -> T) {
        test {
            whenever(readerBlogActionsWrapper.blockBlogFromReader(anyLong(), anyOrNull())).thenAnswer {
                (it.getArgument(1) as ActionListener).onActionResult(false)
                BlockedBlogResult()
            }
            block()
        }
    }

    @Test
    fun `Error SnackBar shown when the device is offline`() = testWithError {
        // Arrange
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        // Act
        val result = blockSiteUseCase.blockSite(1L)
        // Assert
        assertThat(result!!.messageRes).isEqualTo(R.string.reader_toast_err_block_blog)
    }

    @Test
    fun `Success SnackBar shown when the operation succeeds`() = testWithSuccess {
        // Act
        val result = blockSiteUseCase.blockSite(1L)
        // Assert
        assertThat(result!!.messageRes).isEqualTo(R.string.reader_toast_blog_blocked)
    }

    @Test
    fun `Undo action available when the operation succeeds`() = testWithSuccess {
        // Act
        val result = blockSiteUseCase.blockSite(1L)
        // Assert
        assertThat(result!!.buttonTitleRes).isEqualTo(R.string.undo)
    }

    @Test
    fun `Undo invoked when the user clicks on undo button`() = testWithSuccess {
        // Act
        val result = blockSiteUseCase.blockSite(1L)
        result!!.buttonAction.invoke()
        // Assert
        verify(readerBlogActionsWrapper).undoBlockBlogFromReader(anyOrNull())
    }

    @Test
    fun `Error SnackBar shown when the operation fails`() = testWithError {
        // Act
        val result = blockSiteUseCase.blockSite(1L)
        // Assert
        assertThat(result!!.messageRes).isEqualTo(R.string.reader_toast_err_block_blog)
    }

    @Test
    fun `Undo invoked when the operation fails`() = testWithError {
        // Act
        blockSiteUseCase.blockSite(1L)
        // Assert
        verify(readerBlogActionsWrapper).undoBlockBlogFromReader(anyOrNull())
    }
}
