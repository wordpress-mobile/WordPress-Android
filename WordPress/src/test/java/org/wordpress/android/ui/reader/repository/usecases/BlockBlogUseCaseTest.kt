package org.wordpress.android.ui.reader.repository.usecases

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.reader.actions.ReaderBlogActionsWrapper
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.util.NetworkUtilsWrapper

@RunWith(MockitoJUnitRunner::class)
class BlockBlogUseCaseTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var mBlockBlogUseCase: BlockBlogUseCase
    @Mock private lateinit var networkUtilsWrapper: NetworkUtilsWrapper
    @Mock private lateinit var readerTracker: ReaderTracker
    @Mock private lateinit var readerBlogActionsWrapper: ReaderBlogActionsWrapper

    @Before
    fun setUp() {
        mBlockBlogUseCase = BlockBlogUseCase(
                networkUtilsWrapper,
                readerTracker,
                readerBlogActionsWrapper
        )
    }

    @Test
    fun foo() {
    }
}
