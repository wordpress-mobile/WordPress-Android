package org.wordpress.android.ui.reader.viewmodels

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import kotlinx.coroutines.InternalCoroutinesApi

import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.TEST_DISPATCHER

@InternalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class ReaderPostDetailViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ReaderPostDetailViewModel

    @Before
    fun setUp() {
        viewModel = ReaderPostDetailViewModel(TEST_DISPATCHER)
    }

    @Test
    fun `blank test`() {
    }
}
