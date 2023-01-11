package org.wordpress.android.ui.posts

import android.os.Bundle
import androidx.annotation.DimenRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.action.TaxonomyAction
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTaxonomyChanged
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult.Updated
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.AppLog.T
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PrepublishingCategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var siteModel: SiteModel
    private var updateCategoriesJob: Job? = null
    private var addCategoryJob: Job? = null
    private lateinit var selectedCategoryIds: List<Long>

    private val _navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val navigateToHomeScreen: LiveData<Event<Unit>> = _navigateToHomeScreen

    private val _navigateToAddCategoryScreen = MutableLiveData<Bundle>()
    val navigateToAddCategoryScreen: LiveData<Bundle> = _navigateToAddCategoryScreen

    private val _toolbarTitleUiState = MutableLiveData<UiString>()
    val toolbarTitleUiState: LiveData<UiString> = _toolbarTitleUiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    fun start(
        editPostRepository: EditPostRepository,
        siteModel: SiteModel,
        addCategoryRequest: PrepublishingAddCategoryRequest? = null,
        selectedCategoryIds: List<Long>
    ) {
        this.editPostRepository = editPostRepository
        this.siteModel = siteModel
        this.selectedCategoryIds = selectedCategoryIds

        if (isStarted) return
        isStarted = true

        initialize(addCategoryRequest)
    }

    private fun initialize(
        addCategoryRequest: PrepublishingAddCategoryRequest?
    ) {
        setToolbarTitleUiState()

        updateCategoriesListItemUiState(addCategoryRequest)

        addCategoryRequest?.let {
            addCategoryJob?.cancel()
            addCategoryJob = launch(bgDispatcher) {
                addCategoryUseCase.addCategory(it.categoryText, it.categoryParentId, siteModel)
            }
        } ?: run {
            getCategoriesUseCase.fetchSiteCategories(siteModel)
        }
    }

    private fun updateCategoriesListItemUiState(addCategoryRequest: PrepublishingAddCategoryRequest? = null) {
        val selectedIds = if (selectedCategoryIds.isNotEmpty()) {
            selectedCategoryIds
        } else {
            getPostCategories()
        }

        val siteCategories = getSiteCategories()
        _uiState.value = UiState(
            categoriesListItemUiState = buildListOfCategoriesItemUiState(
                siteCategories = siteCategories,
                selectedCategoryIds = selectedIds
            ), progressVisibility = addCategoryRequest != null
        )
    }

    private fun setToolbarTitleUiState() {
        _toolbarTitleUiState.value = UiStringRes(string.prepublishing_nudges_toolbar_title_categories)
    }

    private fun saveAndFinish() {
        if (hasChanges()) {
            _uiState.value = uiState.value?.copy(progressVisibility = true)
            updateCategories()
        } else {
            _navigateToHomeScreen.postValue(Event(Unit))
        }
    }

    private fun updateCategories() {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _snackbarEvents.postValue(
                Event(SnackbarMessageHolder(UiStringRes(string.no_network_message)))
            )
            return
        }

        val categoryList = getSelectedIds()
        updateCategoriesJob?.cancel()
        updateCategoriesJob = launch(bgDispatcher) {
            postUpdatedCategories(categoryList.toList(), editPostRepository)
        }
    }

    private fun postUpdatedCategories(
        categoryList: List<Long>,
        editPostRepository: EditPostRepository
    ) {
        editPostRepository.updateAsync({ postModel ->
            postModel.setCategoryIdList(categoryList)
            true
        }, { _: PostImmutableModel?, result: UpdatePostResult ->
            if (result == Updated) {
                analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_CATEGORIES_ADDED)
                _uiState.value = uiState.value?.copy(progressVisibility = false)
            }
            _navigateToHomeScreen.postValue(Event(Unit))
        })
    }

    private fun getSelectedIds(): List<Long> {
        val uiState = uiState.value as UiState
        return uiState.categoriesListItemUiState.toMutableList()
            .filter { x -> x.checked }
            .map { id -> id.categoryNode.categoryId }
            .toList()
    }

    private fun getSiteCategories() =
        getCategoriesUseCase.getSiteCategories(siteModel)

    private fun getPostCategories() =
        getCategoriesUseCase.getPostCategories(editPostRepository)

    private fun hasChanges(): Boolean {
        val postCategories = getPostCategories()
        val stateSelectedCategories = getSelectedIds()
        return (stateSelectedCategories != postCategories)
    }

    private fun onCategoryToggled(position: Int, checked: Boolean) {
        uiState.value?.let {
            val currentUiState = it
            val updatedUiState = getUpdatedListState(position, checked)

            _uiState.value = currentUiState.copy(categoriesListItemUiState = updatedUiState)
        }
    }

    fun onBackButtonClick() {
        saveAndFinish()
    }

    fun onAddCategoryClick() {
        val bundle = Bundle().apply {
            putSerializable(
                PrepublishingCategoriesFragment.SELECTED_CATEGORY_IDS,
                getSelectedIds().toLongArray()
            )
        }
        _navigateToAddCategoryScreen.postValue(bundle)
    }

    fun onTermUploadedComplete(event: OnTermUploaded) {
        // Sometimes the API will return a success response with a null name which we will
        // treat as an error because without a name, there is no category
        val isError = event.isError || event.term?.name == null
        val message = if (isError) {
            string.adding_cat_failed
        } else {
            string.adding_cat_success
        }
        _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiStringRes(message))))

        if (!isError) {
            val currentState = uiState.value as UiState
            val selectedIds = currentState.categoriesListItemUiState.toMutableList()
                .filter { x -> x.checked }
                .map { id -> id.categoryNode.categoryId }
                .toMutableList()
            selectedIds.add(event.term.remoteTermId)
            val categoryLevels = getSiteCategories()
            val recreatedListItemUiState = buildListOfCategoriesItemUiState(
                categoryLevels,
                selectedIds
            )
            _uiState.value = uiState.value?.copy(
                categoriesListItemUiState = recreatedListItemUiState, progressVisibility = false
            )
        } else {
            _uiState.value = uiState.value?.copy(progressVisibility = false)
        }
    }

    fun onTaxonomyChanged(event: OnTaxonomyChanged) {
        if (event.isError) {
            AppLog.e(T.POSTS, "An error occurred while updating taxonomy with type: " + event.error.type)
            return
        }

        if (event.causeOfChange == TaxonomyAction.FETCH_CATEGORIES) {
            updateCategoriesListItemUiState()
        }
    }

    // State UI modifiers
    private fun getUpdatedListState(
        position: Int,
        checked: Boolean
    ): List<PrepublishingCategoriesListItemUiState> {
        val currentUiState = uiState.value as UiState
        val newListItemUiState = currentUiState.categoriesListItemUiState.toMutableList()
        newListItemUiState[position] = currentUiState.categoriesListItemUiState[position].copy(
            checked = checked, onItemTapped = { onCategoryToggled(position, !checked) }
        )
        return newListItemUiState
    }

    private fun buildListOfCategoriesItemUiState(
        siteCategories: ArrayList<CategoryNode>,
        selectedCategoryIds: List<Long>
    ): List<PrepublishingCategoriesListItemUiState> {
        val items: ArrayList<PrepublishingCategoriesListItemUiState> = arrayListOf()
        siteCategories.forEachIndexed { index, categoryNode ->
            val itemUiState =
                buildCategoriesListItemUiState(
                    categoryNode,
                    selectedCategoryIds.contains(categoryNode.categoryId),
                    index
                )
            items.add(itemUiState)
        }

        return items
    }

    private fun buildCategoriesListItemUiState(
        categoryNode: CategoryNode,
        checked: Boolean,
        index: Int
    ): PrepublishingCategoriesListItemUiState =
        PrepublishingCategoriesListItemUiState(
            categoryNode = categoryNode,
            checked = checked,
            onItemTapped = { onCategoryToggled(index, !checked) }
        )

    data class UiState(
        val addCategoryActionButtonVisibility: Boolean = true,
        val categoriesListItemUiState: List<PrepublishingCategoriesListItemUiState> = listOf(),
        val categoryListVisibility: Boolean = true,
        val progressVisibility: Boolean = false
    )

    data class PrepublishingCategoriesListItemUiState(
        val onItemTapped: ((Int) -> Unit),
        val clickable: Boolean = true,
        val categoryNode: CategoryNode,
        val checked: Boolean = false,
        @DimenRes val verticalPaddingResId: Int = R.dimen.margin_large,
        @DimenRes val horizontalPaddingResId: Int = R.dimen.margin_extra_large
    )
}
