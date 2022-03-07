package org.wordpress.android.ui.prefs.categories.detail

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher

@InternalCoroutinesApi
class CategoryDetailViewModelTest : BaseUnitTest(){
    private val dispatcher: Dispatcher = mock()

    private lateinit var viewModel: CategoryDetailViewModel

    @Before
    fun setUp() {
        viewModel = CategoryDetailViewModel(TEST_DISPATCHER,dispatcher)
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
