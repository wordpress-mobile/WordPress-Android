package org.wordpress.android.ui.prefs.categories

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.never
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction.FETCH_CATEGORIES
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType.GENERIC_ERROR
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Content
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Error.GenericError
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Error.NoConnection
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Loading
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
class CategoriesListViewModelTest : BaseUnitTest() {
    private val getCategoriesUseCase: GetCategoriesUseCase = mock()
    private val networkUtilsWrapper: NetworkUtilsWrapper = mock()
    private val siteModel: SiteModel = mock()
    private val dispatcher: Dispatcher = mock()

    private lateinit var viewModel: CategoriesListViewModel

    private val uiStates = mutableListOf<UiState>()

    @Before
    fun setUp() {
        viewModel = CategoriesListViewModel(
                getCategoriesUseCase,
                networkUtilsWrapper,
                TEST_DISPATCHER,
                dispatcher
        )
        viewModel.uiState.observeForever { if (it != null) uiStates += it }
    }

    @Test
    fun `when vm starts, then loading screen state is displayed`() {
        viewModel.start(siteModel)
        assertTrue(uiStates.first() is Loading)
    }

    @Test
    fun `when vm starts, then list of items from db is displayed`() {
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(mock())
        viewModel.start(siteModel)
        assertTrue(uiStates.last() is Content)
    }

    @Test
    fun `given no items in db with no internet available, when vm starts, then no connection error is displayed`() {
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(arrayListOf())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        viewModel.start(siteModel)

        verify(getCategoriesUseCase, never()).fetchSiteCategories(siteModel)
        verify(getCategoriesUseCase, times(1)).getSiteCategories(siteModel)

        assertTrue(uiStates.last() is NoConnection)
    }

    @Test
    fun `given no items in db with internet available, when vm starts, then loading is displayed until callback`() {
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(arrayListOf())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        viewModel.start(siteModel)

        verify(getCategoriesUseCase, times(1)).fetchSiteCategories(siteModel)
        verify(getCategoriesUseCase, times(1)).getSiteCategories(siteModel)

        assertTrue(uiStates.first() is Loading)
    }

    @Test
    fun `given no items in db with internet available, when vm starts, then list of items from network is displayed`() {
        whenever(getCategoriesUseCase.getSiteCategories(siteModel))
                .thenReturn(arrayListOf(), mock())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel.start(siteModel)
        viewModel.onTaxonomyChanged(getTaxonomyChangedCallback())

        verify(getCategoriesUseCase, times(1)).fetchSiteCategories(siteModel)
        verify(getCategoriesUseCase, times(2)).getSiteCategories(siteModel)

        assertTrue(uiStates.last() is Content)
    }

    @Test
    fun `given no items in db and api error occurs, when vm starts, then error is displayed`() {
        whenever(getCategoriesUseCase.getSiteCategories(siteModel))
                .thenReturn(arrayListOf())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        viewModel.start(siteModel)

        verify(getCategoriesUseCase, times(1)).fetchSiteCategories(siteModel)
        verify(getCategoriesUseCase, times(1)).getSiteCategories(siteModel)

        viewModel.onTaxonomyChanged(getGenericTaxonomyError())
        assertTrue(uiStates.last() is GenericError)
    }

    private fun getGenericTaxonomyError(): OnTaxonomyChanged {
        val taxonomyChanged = OnTaxonomyChanged(0)
        taxonomyChanged.causeOfChange = FETCH_CATEGORIES
        taxonomyChanged.error = TaxonomyError(GENERIC_ERROR)
        return taxonomyChanged
    }

    private fun getTaxonomyChangedCallback(): OnTaxonomyChanged {
        val taxonomyChanged = OnTaxonomyChanged(0)
        taxonomyChanged.causeOfChange = FETCH_CATEGORIES
        return taxonomyChanged
    }
}
