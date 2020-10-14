package org.wordpress.android.ui.posts

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.PrepublishingAddCategoryViewModel.UiState.ContentUiState
import org.wordpress.android.ui.posts.PrepublishingAddCategoryViewModel.UiState.InitialLoadUiState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PrepublishingAddCategoryViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val addCategoryUseCase: AddCategoryUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private var closeKeyboard = true
    private lateinit var siteModel: SiteModel
    private var addCategoryJob: Job? = null

    private val _navigateBack = MutableLiveData<Event<Unit>>()
    val navigateBack: LiveData<Event<Unit>> = _navigateBack

    private val _dismissKeyboard = MutableLiveData<Event<Unit>>()
    val dismissKeyboard: LiveData<Event<Unit>> = _dismissKeyboard

    private val _toolbarTitleUiState = MutableLiveData<UiString>()
    val toolbarTitleUiState: LiveData<UiString> = _toolbarTitleUiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    fun start(
        siteModel: SiteModel,
        closeKeyboard: Boolean = false
    ) {
        this.closeKeyboard = closeKeyboard
        this.siteModel = siteModel

        if (isStarted) return
        isStarted = true

        setToolbarTitleUiState()
        loadCategories()
    }

    private fun setToolbarTitleUiState() {
        _toolbarTitleUiState.postValue(UiStringRes(string.prepublishing_nudges_toolbar_title_add_categories))
    }

    fun onBackButtonClicked() {
        if (closeKeyboard) {
            _dismissKeyboard.postValue(Event(Unit))
        }
        _navigateBack.postValue(Event(Unit))
    }

    override fun onCleared() {
        super.onCleared()
        addCategoryJob?.cancel()
    }

    private fun loadCategories() {
        updateUiState(InitialLoadUiState)
        val newUiState = ContentUiState(categories = getCategoryLevels())
        updateUiState(newUiState)
    }

    fun addCategory(categoryText: String, parentCategory: CategoryNode?) {
        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _dismissKeyboard.postValue(Event(Unit))
            _snackbarEvents.postValue(
                    Event(SnackbarMessageHolder(UiStringRes(string.no_network_message)))
            )
            return
        }

        if (categoryText.isNotEmpty()) {
            trackCategoryAddedEvent()
            val parentCategoryId = parentCategory?.categoryId ?: 0
            addCategoryJob?.cancel()
            addCategoryJob = launch(bgDispatcher) {
                addCategoryUseCase.addCategory(categoryText, parentCategoryId, siteModel)
            }
        }
    }

    private fun getCategoryLevels(): ArrayList<CategoryNode> =
            getCategoriesUseCase.getCategoryLevels(siteModel)

    private fun trackCategoryAddedEvent() {
        analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_CATEGORIES_ADDED)
    }

    private fun updateUiState(uiState: UiState) {
        _uiState.value = uiState
    }

    sealed class UiState(
        val closeButtonVisible: Boolean = true
    ) {
        object InitialLoadUiState : UiState()

        data class ContentUiState(
            val categories: ArrayList<CategoryNode>
        ) : UiState()
    }
}
