package org.wordpress.android.ui.posts

import android.os.Bundle
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.PrepublishingAddCategoryViewModel.SubmitButtonUiState.SubmitButtonDisabledUiState
import org.wordpress.android.ui.posts.PrepublishingAddCategoryViewModel.SubmitButtonUiState.SubmitButtonEnabledUiState
import org.wordpress.android.ui.posts.PrepublishingCategoriesFragment.Companion.ADD_CATEGORY_REQUEST
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ResourceProvider
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PrepublishingAddCategoryViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val resourceProvider: ResourceProvider,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private var closeKeyboard = true
    private lateinit var siteModel: SiteModel

    private val _navigateBack = MutableLiveData<Bundle?>()
    val navigateBack: LiveData<Bundle?> = _navigateBack

    private val _dismissKeyboard = MutableLiveData<Event<Unit>>()
    val dismissKeyboard: LiveData<Event<Unit>> = _dismissKeyboard

    private val _toolbarTitleUiState = MutableLiveData<UiString>()
    val toolbarTitleUiState: LiveData<UiString> = _toolbarTitleUiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    // Public
    fun start(
        siteModel: SiteModel,
        closeKeyboard: Boolean = false
    ) {
        this.closeKeyboard = closeKeyboard
        this.siteModel = siteModel

        if (isStarted) return
        isStarted = true

        init()
    }

    fun categoryNameUpdated(inputValue: String) {
        _uiState.value?.let { state ->
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

    fun parentCategorySelected(position: Int) {
        _uiState.value?.let { state ->
            _uiState.value = state.copy(selectedParentCategoryPosition = position)
        }
    }

    fun onSubmitButtonClick() {
        _uiState.value?.let { state ->
            addCategory(
                state.categoryName,
                state.categories[state.selectedParentCategoryPosition]
            )
        }
    }

    fun onBackButtonClick() {
        cleanupAndFinish()
    }

    // private
    private fun init() {
        setToolbarTitleUiState()
        initCategories()
    }

    private fun setToolbarTitleUiState() {
        _toolbarTitleUiState.value = UiStringRes(R.string.prepublishing_nudges_toolbar_title_add_categories)
    }

    private fun initCategories() {
        val categoryLevels = getCategoryLevels()
        categoryLevels.add(
            0, CategoryNode(
                0, 0,
                resourceProvider.getString(R.string.top_level_category_name)
            )
        )
        _uiState.value = UiState(
            categories = categoryLevels,
            selectedParentCategoryPosition = 0,
            categoryName = ""
        )
    }

    private fun addCategory(categoryText: String, parentCategory: CategoryNode) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _dismissKeyboard.postValue(Event(Unit))
            _snackbarEvents.postValue(
                Event(SnackbarMessageHolder(UiStringRes(R.string.no_network_message)))
            )
            return
        }

        val bundle = Bundle().apply {
            putSerializable(
                ADD_CATEGORY_REQUEST,
                PrepublishingAddCategoryRequest(categoryText, parentCategory.categoryId)
            )
        }

        cleanupAndFinish(bundle)
    }

    private fun cleanupAndFinish(bundle: Bundle? = null) {
        if (closeKeyboard) {
            _dismissKeyboard.postValue(Event(Unit))
        }

        _navigateBack.postValue(bundle)
    }

    private fun getCategoryLevels(): ArrayList<CategoryNode> =
        getCategoriesUseCase.getSiteCategories(siteModel)

    // States
    data class UiState(
        val categories: ArrayList<CategoryNode>,
        val selectedParentCategoryPosition: Int,
        val categoryName: String,
        val submitButtonUiState: SubmitButtonUiState = SubmitButtonDisabledUiState
    )

    sealed class SubmitButtonUiState(
        val visibility: Boolean = true,
        val enabled: Boolean = false
    ) {
        object SubmitButtonEnabledUiState : SubmitButtonUiState(
            enabled = true
        )

        object SubmitButtonDisabledUiState : SubmitButtonUiState(
            enabled = false
        )
    }
}
