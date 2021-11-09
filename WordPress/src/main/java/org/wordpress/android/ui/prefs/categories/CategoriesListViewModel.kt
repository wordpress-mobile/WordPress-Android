package org.wordpress.android.ui.prefs.categories

import androidx.annotation.DrawableRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.action.TaxonomyAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Content
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Error.GenericError
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Error.NoConnection
import org.wordpress.android.ui.prefs.categories.CategoriesListViewModel.UiState.Loading
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

//todo check whether analyticsTrackerWrapper needs to be used and how
class CategoriesListViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher
) : ScopedViewModel(mainDispatcher) {
    private var isStarted = false
    lateinit var siteModel: SiteModel

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    private val _navigateToAddScreen: MutableLiveData<Boolean> = MutableLiveData()
    val navigateToAddScreen: LiveData<Boolean> = _navigateToAddScreen

    fun start(siteModel: SiteModel) {
        if (isStarted) return
        isStarted = true

        this.siteModel = siteModel
        getCategoriesFromDb()
        fetchCategoriesFromNetwork(isInvokedFromInit = true)
    }

    private fun getCategoriesFromDb() {
        _uiState.postValue(Loading)
        launch(bgDispatcher) {
            val siteCategories = getCategoriesUseCase.getSiteCategories(siteModel)
            if (siteCategories.isNotEmpty())
                _uiState.postValue(Content(siteCategories))
        }
    }

    private fun fetchCategoriesFromNetwork(isInvokedFromInit: Boolean = false) {
        launch(bgDispatcher) {
            if (networkUtilsWrapper.isNetworkAvailable()) {
                if (!isInvokedFromInit)
                    _uiState.postValue(Loading)
                getCategoriesUseCase.fetchSiteCategories(siteModel)
            } else
                if (_uiState.value is Loading)
                    _uiState.postValue(NoConnection(::fetchCategoriesFromNetwork))
        }
    }

    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        if (event.isError) {
            AppLog.e(T.SETTINGS, "An error occurred while updating taxonomy with type: " + event.error.type)
        }

        if (event.causeOfChange == TaxonomyAction.FETCH_CATEGORIES) {
            processFetchCategoriesCallback(event)
        }
    }

    fun createCategory() {
        _navigateToAddScreen.postValue(true)
    }

    private fun processFetchCategoriesCallback(event: OnTaxonomyChanged) {
        if (event.isError) {
            if (_uiState.value is Loading)
                _uiState.postValue(GenericError(::fetchCategoriesFromNetwork))
            return
        }
        launch(bgDispatcher) {
            val siteCategories = getCategoriesUseCase.getSiteCategories(siteModel)
            if (siteCategories.isEmpty()) {
                _uiState.postValue(UiState.Error.NoCategories(::createCategory))
            } else {
                _uiState.postValue(Content(siteCategories))
            }
        }
    }

    sealed class UiState {
        data class Content(val list: List<CategoryNode>) : UiState()
        object Loading : UiState()
        sealed class Error : UiState() {
            abstract val image: Int
            abstract val title: UiString
            abstract val subtitle: UiString
            open val buttonText: UiString? = null
            open val action: (() -> Unit)? = null

            data class NoConnection(override val action: () -> Unit) : Error() {
                @DrawableRes override val image = R.drawable.img_illustration_cloud_off_152dp
                override val title = UiStringRes(string.site_settings_categories_no_network_title)
                override val subtitle = UiStringRes(string.site_settings_categories_no_network_subtitle)
                override val buttonText = UiStringRes(string.retry)
            }

            data class NoCategories(override val action: () -> Unit) : Error() {
                @DrawableRes override val image = R.drawable.img_illustration_empty_results_216dp
                override val title = UiStringRes(string.site_settings_categories_empty_title)
                override val subtitle = UiStringRes(string.site_settings_categories_empty_subtitle)
                override val buttonText = UiStringRes(string.site_settings_categories_empty_button)
            }

            // todo add custom message for generic error
            data class GenericError(override val action: () -> Unit) : Error() {
                @DrawableRes override val image = R.drawable.img_illustration_empty_results_216dp
                override val title = UiStringRes(string.site_settings_categories_empty_title)
                override val subtitle = UiStringRes(string.site_settings_categories_empty_subtitle)
                override val buttonText = UiStringRes(string.button_retry)
            }
        }
    }
}
