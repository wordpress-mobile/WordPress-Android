package org.wordpress.android.viewmodel.reader

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Rule
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.reader.reblog.ReblogUseCase
import org.wordpress.android.ui.reader.tracker.ReaderTracker
import org.wordpress.android.ui.reader.viewmodels.ReaderPostListViewModel

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostListViewModelTest {
    @Rule
    @JvmField
    val rule = InstantTaskExecutorRule()

    @Mock private lateinit var reblogUseCase: ReblogUseCase
    @Mock private lateinit var readerTracker: ReaderTracker

    private lateinit var viewModel: ReaderPostListViewModel

    @Before
    fun setUp() {
        viewModel = ReaderPostListViewModel(reblogUseCase, readerTracker)
    }
}
