package org.wordpress.android.ui.prefs.categories.list

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.delay
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.prefs.categories.list.CategoryDetailNavigation.CreateCategory
import org.wordpress.android.ui.prefs.categories.list.CategoryDetailNavigation.EditCategory
import org.wordpress.android.ui.prefs.categories.list.UiState.Content
import org.wordpress.android.ui.prefs.categories.list.UiState.Error.GenericError
import org.wordpress.android.ui.prefs.categories.list.UiState.Error.NoConnection
import org.wordpress.android.ui.prefs.categories.list.UiState.Loading
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import org.wordpress.android.viewmodel.SingleLiveEvent
import javax.inject.Inject
import javax.inject.Named

private const val RETRY_DELAY = 300L

@HiltViewModel
class CategoriesListViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    lateinit var siteModel: SiteModel

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _navigation: SingleLiveEvent<CategoryDetailNavigation> = SingleLiveEvent()
    val navigation: SingleLiveEvent<CategoryDetailNavigation> = _navigation

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
    }

    fun start(siteModel: SiteModel) {
        if (isStarted) return
        isStarted = true

        this.siteModel = siteModel
        getCategoriesFromDb()
        launch {
            fetchCategoriesFromNetwork(isInvokedFromInit = true)
        }
    }

    private fun getCategoriesFromDb() {
        _uiState.value = Loading
        launch {
            val siteCategories = getCategoriesUseCase.getSiteCategories(siteModel)
            if (siteCategories.isNotEmpty()) _uiState.postValue(Content(siteCategories))
        }
    }

    private fun onRetryClicked() {
        launch { fetchCategoriesFromNetwork() }
    }

    private suspend fun fetchCategoriesFromNetwork(isInvokedFromInit: Boolean = false) {
        if (!isInvokedFromInit) {
            _uiState.postValue(Loading)
            delay(RETRY_DELAY)
        }
        if (networkUtilsWrapper.isNetworkAvailable()) {
            getCategoriesUseCase.fetchSiteCategories(siteModel)
        } else if (_uiState.value is Loading) _uiState.postValue(NoConnection(::onRetryClicked))
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        if (event.isError) AppLog.e(
                T.SETTINGS,
                "An error occurred while updating taxonomy with type: " + event.error.type
        )

        if (event.causeOfChange == TaxonomyAction.FETCH_CATEGORIES)
            processFetchCategoriesCallback(event)
        if (event.causeOfChange == TaxonomyAction.UPDATE_TERM)
            launch { fetchCategoriesFromNetwork() }
        if (event.causeOfChange == TaxonomyAction.REMOVE_TERM)
            launch { fetchCategoriesFromNetwork() }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onTermUploaded(event: OnTermUploaded) {
        if (!event.isError) getCategoriesFromDb()
    }

    fun createCategory() {
        _navigation.postValue(CreateCategory)
    }

    @Suppress("unused", "UNUSED_PARAMETER")
    fun onCategoryClicked(categoryNode: CategoryNode) {
        _navigation.postValue(EditCategory(categoryNode.categoryId))
    }

    private fun processFetchCategoriesCallback(event: OnTaxonomyChanged) {
        if (event.isError) {
            if (_uiState.value is Loading) _uiState.value = GenericError(::onRetryClicked)
            return
        }
        launch {
            val siteCategories = getCategoriesUseCase.getSiteCategories(siteModel)
            _uiState.postValue(Content(siteCategories))
        }
    }
}
