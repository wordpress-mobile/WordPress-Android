package org.wordpress.android.ui.reader.discover.interests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.reader.repository.ReaderTagRepository

@RunWith(MockitoJUnitRunner::class)
class ReaderInterestsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ReaderInterestsViewModel

    @Mock lateinit var readerTagRepository: ReaderTagRepository

    @Before
    fun setUp() {
        viewModel = ReaderInterestsViewModel(readerTagRepository)
    }

    @Test
    fun `sample test`() {} // TODO:
}
