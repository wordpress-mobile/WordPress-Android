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
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.PrepublishingCategoriesViewModel.UiState.ContentUiState
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.util.NetworkUtilsWrapper
import org.wordpress.android.util.analytics.AnalyticsTrackerWrapper
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PrepublishingCategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val updatePostCategoriesUseCase: UpdatePostCategoriesUseCase,
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher
) : ScopedViewModel(bgDispatcher) {
    private var isStarted = false
    private lateinit var editPostRepository: EditPostRepository
    private lateinit var siteModel: SiteModel
    private var updateCategoriesJob: Job? = null

    private val _navigateToHomeScreen = MutableLiveData<Event<Unit>>()
    val navigateToHomeScreen: LiveData<Event<Unit>> = _navigateToHomeScreen

    private val _navigateToAddCategoryScreen = MutableLiveData<Event<Unit>>()
    val navigateToAddCategoryScreen: LiveData<Event<Unit>> = _navigateToAddCategoryScreen

    private val _toolbarTitleUiState = MutableLiveData<UiString>()
    val toolbarTitleUiState: LiveData<UiString> = _toolbarTitleUiState

    private val _snackbarEvents = MediatorLiveData<Event<SnackbarMessageHolder>>()
    val snackbarEvents: LiveData<Event<SnackbarMessageHolder>> = _snackbarEvents

    private val _uiState: MutableLiveData<UiState> = MutableLiveData()
    val uiState: LiveData<UiState> = _uiState

    fun start(editPostRepository: EditPostRepository, siteModel: SiteModel) {
        this.editPostRepository = editPostRepository
        this.siteModel = siteModel

        if (isStarted) return
        isStarted = true

        init()
    }

    private fun init() {
        setToolbarTitleUiState()

        val siteCategories = getSiteCategories()
        val postCategories = getPostCategories()
        _uiState.value = ContentUiState(
                siteCategories = siteCategories,
                selectedCategoryIds = postCategories.toHashSet()
        )
    }

    private fun setToolbarTitleUiState() {
        _toolbarTitleUiState.postValue(UiStringRes(string.prepublishing_nudges_toolbar_title_categories))
    }

    fun onBackButtonClicked() {
        _navigateToHomeScreen.postValue(Event(Unit))
    }

    fun onAddNewCategoryClicked() {
        _navigateToAddCategoryScreen.postValue(Event(Unit))
    }

    override fun onCleared() {
        super.onCleared()
        updateCategoriesJob?.cancel()
    }

    fun updateCategories() {
        if (!hasChanges()) return

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            _snackbarEvents.postValue(
                    Event(SnackbarMessageHolder(UiStringRes(string.no_network_message)))
            )
            return
        }

        val categoryList = (uiState.value as? ContentUiState)?.selectedCategoryIds ?: return
        updateCategoriesJob?.cancel()
        updateCategoriesJob = launch(bgDispatcher) {
            updatePostCategoriesUseCase.updateCategories(categoryList.toList(), editPostRepository)
        }
        trackCategoriesChangedEvent()
    }

    fun addSelectedCategory(categoryId: Long) {
        uiState.value.let { state ->
            if (state is ContentUiState) {
                state.selectedCategoryIds.add(categoryId)
                _uiState.value = state.copy()
            }
        }
    }

    fun removeSelectedCategory(categoryId: Long) {
        uiState.value.let { state ->
            if (state is ContentUiState) {
                state.selectedCategoryIds.remove(categoryId)
                _uiState.value = state.copy()
            }
        }
    }

    fun onTermUploadedComplete(event: OnTermUploaded) {
        val message = if (event.isError) {
            string.adding_cat_failed
        } else {
            string.adding_cat_success
        }
        _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiStringRes(message))))

        if (!event.isError) {
            val categoryLevels = getSiteCategories()
            val selectedCategoryIds = (uiState.value as? ContentUiState)?.selectedCategoryIds
                    ?: hashSetOf()
            selectedCategoryIds.add(event.term.remoteTermId)
            _uiState.value = ContentUiState(
                    siteCategories = categoryLevels,
                    selectedCategoryIds = selectedCategoryIds
            )
        }
    }

    private fun getSiteCategories() =
            getCategoriesUseCase.getSiteCategories(siteModel)

    private fun getPostCategories() =
            getCategoriesUseCase.getPostCategories(editPostRepository, siteModel)

    private fun trackCategoriesChangedEvent() {
        analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_CATEGORIES_ADDED)
    }

    private fun hasChanges(): Boolean {
        val postCategories = getPostCategories()
        val stateSelectedCategories =
                uiState.value.let { state ->
                    if (state is ContentUiState) {
                        state.selectedCategoryIds.toList()
                    } else {
                        listOf()
                    }
                }

        return (stateSelectedCategories != postCategories)
    }

    sealed class UiState {
        data class ContentUiState(
            val siteCategories: List<CategoryNode>,
            val selectedCategoryIds: HashSet<Long>
        ) : UiState()
    }
}
