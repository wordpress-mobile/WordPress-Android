package org.wordpress.android.ui.jetpack.scan

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class ScanViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: ScanViewModel

    @Before
    fun setUp() {
        viewModel = ScanViewModel()
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
