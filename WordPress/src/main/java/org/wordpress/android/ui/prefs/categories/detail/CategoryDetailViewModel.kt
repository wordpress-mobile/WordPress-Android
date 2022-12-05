package org.wordpress.android.ui.prefs.categories.detail

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.AddCategoryUseCase
import org.wordpress.android.ui.posts.EditCategoryUseCase
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
import java.util.ArrayList
import javax.inject.Inject
import javax.inject.Named

class CategoryDetailViewModel @Inject constructor(
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val editCategoryUseCase: EditCategoryUseCase,
    resourceProvider: ResourceProvider,
    private val dispatcher: Dispatcher,
    selectedSiteRepository: SelectedSiteRepository
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private val siteModel: SiteModel = requireNotNull(selectedSiteRepository.getSelectedSite())
    private var existingCategoryname: String = ""

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

    fun start(categoryId: Long? = null) {
        Log.e("start: categoryId", categoryId.toString())
        if (isStarted) return
        isStarted = true

        initCategories(categoryId)
    }

    private fun initCategories(categoryId: Long?) {
        launch {
            val siteCategories = getCategoriesUseCase.getSiteCategories(siteModel)
            siteCategories.add(0, topLevelCategory)
            categoryId?.let { initializeEditCategoryState(siteCategories, categoryId) }
                    ?: initializeAddCategoryState(siteCategories)
        }
    }

    private fun initializeAddCategoryState(siteCategories: ArrayList<CategoryNode>) {
        _uiState.postValue(
                UiState(
                        categories = siteCategories,
                        selectedParentCategoryPosition = 0,
                        categoryName = ""
                )
        )
    }

    private fun initializeEditCategoryState(siteCategories: ArrayList<CategoryNode>, categoryId: Long) {
        Log.e("initializeEditCategoryState: ", categoryId.toString())
        val existingCategory = siteCategories.filter { it.categoryId == categoryId }[0]
        var parentCategoryPosition = siteCategories.indexOfFirst { it.categoryId == existingCategory.parentId }
        if (parentCategoryPosition == -1) parentCategoryPosition = 0
        Log.e("initializeEditCategoryState: parentCategoryPosition ", parentCategoryPosition.toString())
        Log.e("initializeEditCategoryState: existing category", existingCategory.toString())
        existingCategoryname = existingCategory.name
        _uiState.postValue(
                UiState(
                        categories = siteCategories,
                        selectedParentCategoryPosition = parentCategoryPosition,
                        categoryName = existingCategory.name,
                        categoryId = categoryId
                )
        )
    }

    fun onSubmitButtonClick() {
        _uiState.value?.let { state ->
            state.categoryId?.let {
                editCategory(it, state.categoryName, state.categories[state.selectedParentCategoryPosition])
            } ?: addCategory(state.categoryName, state.categories[state.selectedParentCategoryPosition])
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

    private fun editCategory(categoryId: Long, categoryText: String, parentCategory: CategoryNode) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onCategoryPush.postValue(
                    Event(Failure(UiStringRes(R.string.no_network_message)))
            )
            return
        }
        launch {
            _onCategoryPush.postValue(Event(InProgress))
            editCategoryUseCase.editCategory(
                    categoryId,
                    existingCategoryname,
                    categoryText,
                    parentCategory.categoryId,
                    siteModel
            )
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
