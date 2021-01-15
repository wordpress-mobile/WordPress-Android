package org.wordpress.android.ui.jetpack.restore

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.jetpack.restore.details.RestoreDetailsViewModel

@RunWith(MockitoJUnitRunner::class)
class RestoreDetailsViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: RestoreDetailsViewModel

    @Before
    fun setUp() {
        viewModel = RestoreDetailsViewModel()
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
