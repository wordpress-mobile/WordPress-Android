package org.wordpress.android.ui.prefs.categories

import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.InternalCoroutinesApi
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.TEST_DISPATCHER
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction.FETCH_CATEGORIES
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyError
import org.wordpress.android.fluxc.store.TaxonomyStore.TaxonomyErrorType.GENERIC_ERROR
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.prefs.categories.list.CategoriesListViewModel
import org.wordpress.android.ui.prefs.categories.list.UiState
import org.wordpress.android.ui.prefs.categories.list.UiState.Content
import org.wordpress.android.ui.prefs.categories.list.UiState.Error.GenericError
import org.wordpress.android.ui.prefs.categories.list.UiState.Error.NoConnection
import org.wordpress.android.ui.prefs.categories.list.UiState.Loading
import org.wordpress.android.util.NetworkUtilsWrapper

@InternalCoroutinesApi
@ExperimentalCoroutinesApi
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
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(arrayListOf(), mock())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)

        viewModel.start(siteModel)
        viewModel.onTaxonomyChanged(getTaxonomyChangedCallback())

        verify(getCategoriesUseCase, times(1)).fetchSiteCategories(siteModel)
        verify(getCategoriesUseCase, times(2)).getSiteCategories(siteModel)

        assertTrue(uiStates.last() is Content)
    }

    @Test
    fun `given no items in db and api error occurs, when vm starts, then error is displayed`() {
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(arrayListOf())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        viewModel.start(siteModel)

        verify(getCategoriesUseCase, times(1)).fetchSiteCategories(siteModel)
        verify(getCategoriesUseCase, times(1)).getSiteCategories(siteModel)

        viewModel.onTaxonomyChanged(getGenericTaxonomyError())
        assertTrue(uiStates.last() is GenericError)
    }

    @Test
    fun `given error occurs, when retry is invoked, then loading is displayed`() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true)
        viewModel.start(siteModel)

        viewModel.onTaxonomyChanged(getGenericTaxonomyError())
        (uiStates.last() as GenericError).action.invoke()

        verify(getCategoriesUseCase, times(2)).fetchSiteCategories(siteModel)
        assertTrue(uiStates.last() is Loading)
    }

    @Test
    fun `given api error occurs, when retry is invoked on no network, then no network is displayed`() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(true).thenReturn(false)
        viewModel.start(siteModel)

        viewModel.onTaxonomyChanged(getGenericTaxonomyError())
        (uiStates.last() as GenericError).action.invoke()

        verify(getCategoriesUseCase, times(1)).fetchSiteCategories(siteModel)
        assertTrue(uiStates.last() is NoConnection)
    }

    @Test
    fun `given no network, when retry is invoked, then no connection error is displayed`() {
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false)
        viewModel.start(siteModel)

        (uiStates.last() as NoConnection).action.invoke()

        verify(getCategoriesUseCase, never()).fetchSiteCategories(siteModel)
        assertTrue(uiStates.last() is NoConnection)
    }

    @Test
    fun `given network available, when retry is invoked, then list of items from network is displayed`() {
        whenever(getCategoriesUseCase.getSiteCategories(siteModel)).thenReturn(arrayListOf(), mock())
        whenever(networkUtilsWrapper.isNetworkAvailable()).thenReturn(false).thenReturn(true)
        viewModel.start(siteModel)

        (uiStates.last() as NoConnection).action.invoke()

        verify(getCategoriesUseCase, times(1)).fetchSiteCategories(siteModel)
        viewModel.onTaxonomyChanged(getTaxonomyChangedCallback())
        assertTrue(uiStates.last() is Content)
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
