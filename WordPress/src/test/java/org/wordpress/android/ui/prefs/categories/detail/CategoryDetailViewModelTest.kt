package org.wordpress.android.ui.prefs.categories.detail

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.AddCategoryUseCase
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ResourceProvider

@InternalCoroutinesApi
class CategoryDetailViewModelTest : BaseUnitTest() {
    private lateinit var viewModel: CategoryDetailViewModel

    private val dispatcher: Dispatcher = mock()
    private val networkUtilsWrapper: NetworkUtilsWrapper = mock()
    private val getCategoriesUseCase: GetCategoriesUseCase = mock()
    private val addCategoryUseCase: AddCategoryUseCase = mock()
    private val resourceProvider: ResourceProvider = mock()
    private val selectedSiteRepository: SelectedSiteRepository = mock()

    @Before
    fun setUp() {
        viewModel = CategoryDetailViewModel(
                TEST_DISPATCHER,
                networkUtilsWrapper,
                getCategoriesUseCase,
                addCategoryUseCase,
                resourceProvider,
                dispatcher,
                selectedSiteRepository
        )
    }

    @Test
    fun `sample test`() {
    } // TODO:
}
