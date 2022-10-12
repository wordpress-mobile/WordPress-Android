package org.wordpress.android.ui.prefs.categories.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.AddCategoryUseCase
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.Failure
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.InProgress
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.Success
import org.wordpress.android.ui.prefs.categories.detail.SubmitButtonUiState.SubmitButtonDisabledUiState
import org.wordpress.android.ui.prefs.categories.detail.SubmitButtonUiState.SubmitButtonEnabledUiState
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class CategoryDetailViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    resourceProvider: ResourceProvider,
    private val dispatcher: Dispatcher,
    selectedSiteRepository: SelectedSiteRepository
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private val siteModel: SiteModel = requireNotNull(selectedSiteRepository.getSelectedSite())

    private val topLevelCategory = CategoryNode(0, 0, resourceProvider.getString(R.string.top_level_category_name))

    private val _onCategoryPush = MutableLiveData<Event<CategoryUpdateUiState>>()
    val onCategoryPush: LiveData<Event<CategoryUpdateUiState>> = _onCategoryPush

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    init {
        dispatcher.register(this)
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
    }

    fun start() {
        if (isStarted) return
        isStarted = true

        initCategories()
    }

    private fun initCategories() {
        launch {
            val siteCategories = getCategoriesUseCase.getSiteCategories(siteModel)
            siteCategories.add(0, topLevelCategory)
            _uiState.postValue(
                    UiState(
                            categories = siteCategories,
                            selectedParentCategoryPosition = 0,
                            categoryName = ""
                    )
            )
        }
    }

    fun onSubmitButtonClick() {
        _uiState.value?.let { state ->
            addCategory(state.categoryName, state.categories[state.selectedParentCategoryPosition])
        }
    }

    private fun addCategory(categoryText: String, parentCategory: CategoryNode) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onCategoryPush.postValue(
                    Event(Failure(UiStringRes(R.string.no_network_message)))
            )
            return
        }
        launch {
            _onCategoryPush.postValue(Event(InProgress))
            addCategoryUseCase.addCategory(categoryText, parentCategory.categoryId, siteModel)
        }
    }

    fun onCategoryNameUpdated(inputValue: String) {
        uiState.value?.let { state ->
            val submitButtonUiState = if (inputValue.isNotEmpty()) {
                SubmitButtonEnabledUiState
            } else {
                SubmitButtonDisabledUiState
            }
            _uiState.value = state.copy(
                    categoryName = inputValue,
                    submitButtonUiState = submitButtonUiState
            )
        }
    }

    fun onParentCategorySelected(position: Int) {
        _uiState.value?.let { state ->
            _uiState.value = state.copy(selectedParentCategoryPosition = position)
        }
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onTermUploaded(event: OnTermUploaded) {
        if (event.isError) AppLog.e(
                T.SETTINGS,
                "An error occurred while uploading taxonomy with type: " + event.error.type
        )
        val categoryUiState = if (event.isError) {
            Failure(UiStringRes(R.string.adding_cat_failed))
        } else {
            Success(UiStringRes(R.string.adding_cat_success))
        }
        _onCategoryPush.postValue(Event(categoryUiState))
    }
}
