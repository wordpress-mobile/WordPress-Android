package org.wordpress.android.ui.reader.discover

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ReaderDiscoverViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ReaderDiscoverViewModel

    @Before
    fun setUp() {
        viewModel = ReaderDiscoverViewModel()
    }

    @Test
    fun foo() {
    }
}
