package org.wordpress.android.ui.reader.discover.interests

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ReaderInterestsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ReaderInterestsViewModel

    @Before
    fun setUp() {
        viewModel = ReaderInterestsViewModel()
    }

    @Test
    fun `sample test`() {} // TODO:
}
