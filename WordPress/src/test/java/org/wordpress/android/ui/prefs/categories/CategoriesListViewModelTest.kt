package org.wordpress.android.ui.prefs.categories

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

@RunWith(MockitoJUnitRunner::class)
class CategoriesListViewModelTest {
    @Rule
    @JvmField val rule = InstantTaskExecutorRule()

    private lateinit var viewModel: CategoriesListViewModel

    @Before
    fun setUp() {
        viewModel = CategoriesListViewModel()
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
