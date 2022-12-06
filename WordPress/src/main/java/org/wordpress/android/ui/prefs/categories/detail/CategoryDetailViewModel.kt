package org.wordpress.android.ui.prefs.categories.detail

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.TaxonomyAction.REMOVE_TERM
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.TermModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.mysite.SelectedSiteRepository
import org.wordpress.android.ui.posts.AddCategoryUseCase
import org.wordpress.android.ui.posts.DeleteCategoryUseCase
import org.wordpress.android.ui.posts.EditCategoryUseCase
import org.wordpress.android.ui.posts.GetCategoriesUseCase
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.Failure
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.InProgress
import org.wordpress.android.ui.prefs.categories.detail.CategoryUpdateUiState.Success
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
    private val editCategoryUseCase: EditCategoryUseCase,
    private val deleteCategoryUseCase: DeleteCategoryUseCase,
    resourceProvider: ResourceProvider,
    private val dispatcher: Dispatcher,
    selectedSiteRepository: SelectedSiteRepository
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private val siteModel: SiteModel = requireNotNull(selectedSiteRepository.getSelectedSite())
    var existingCategory: TermModel? = null

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
        existingCategory = getCategoriesUseCase.getCategoriesForSite(siteModel)
                .find { it.remoteTermId == categoryId }
        var parentCategoryPosition = siteCategories.indexOfFirst { it.categoryId == existingCategory!!.parentRemoteId }
        if (parentCategoryPosition == -1) parentCategoryPosition = 0
        _uiState.postValue(
                UiState(
                        categories = siteCategories,
                        selectedParentCategoryPosition = parentCategoryPosition,
                        categoryName = existingCategory!!.name,
                        categoryId = categoryId,
                        submitButtonUiState = SubmitButtonUiState(buttonText = UiStringRes(R.string.update_category))
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
            _onCategoryPush.postValue(Event(InProgress(R.string.adding_cat)))
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
            _onCategoryPush.postValue(Event(InProgress(R.string.updating_cat)))
            editCategoryUseCase.editCategory(
                    categoryId,
                    existingCategory!!.slug,
                    categoryText,
                    parentCategory.categoryId,
                    siteModel
            )
        }
    }

    fun onCategoryNameUpdated(inputValue: String) {
        existingCategory?.let {
            if (inputValue.trim() == it.name.trim())
                return@onCategoryNameUpdated
        }
        uiState.value?.let { state ->
            val submitButtonUiState = if (inputValue.isNotEmpty()) {
                state.submitButtonUiState.copy(enabled = true)
            } else {
                state.submitButtonUiState.copy(enabled = false)
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
        val categoryUiState = if (event.isError) Failure(UiStringRes(getTermUploadErrorMessage()))
        else Success(UiStringRes(getTermUploadSuccessMessage()))
        _onCategoryPush.postValue(Event(categoryUiState))
    }

    @Suppress("unused")
    @Subscribe(threadMode = MAIN)
    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        if (event.isError) AppLog.e(
                T.SETTINGS,
                "An error occurred while deleting taxonomy with type: " + event.error.type
        )
        when (event.causeOfChange) {
            REMOVE_TERM -> showDeleteStatusMessage(event.isError)
            else -> return
        }
    }

    private fun showDeleteStatusMessage(isError: Boolean) {
        val categoryUiState = if (isError) Failure(UiStringRes(R.string.deleting_cat_failed))
        else Success(UiStringRes(R.string.deleting_cat_success))
        _onCategoryPush.postValue(Event(categoryUiState))
    }

    private fun getTermUploadErrorMessage(): Int {
        return existingCategory?.let { R.string.updating_cat_failed } ?: R.string.adding_cat_failed
    }

    private fun getTermUploadSuccessMessage(): Int {
        return existingCategory?.let { R.string.updating_cat_success } ?: R.string.adding_cat_success
    }

    fun deleteCategory() {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _onCategoryPush.postValue(
                    Event(Failure(UiStringRes(R.string.no_network_message)))
            )
            return
        }
        launch {
            _onCategoryPush.postValue(Event(InProgress(R.string.deleting_cat)))
            deleteCategoryUseCase.deleteCategory(existingCategory!!, siteModel)
        }
    }
}
