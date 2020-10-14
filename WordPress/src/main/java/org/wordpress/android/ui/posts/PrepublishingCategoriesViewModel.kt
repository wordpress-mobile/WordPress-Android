package org.wordpress.android.ui.posts

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.wordpress.android.R.string
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class PrepublishingCategoriesViewModel @Inject constructor(
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val updatePostCategoriesUseCase: UpdatePostCategoriesUseCase,
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

        setToolbarTitleUiState()
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

    fun getCategoryLevels(): ArrayList<CategoryNode> =
            getCategoriesUseCase.getCategoryLevels(siteModel)

    fun fetchNewCategories() = launch { getCategoriesUseCase.fetchNewCategories(siteModel) }

    fun updateCategories(categoryList: List<Long>?) {
        if (categoryList == null) {
            return
        }
        updateCategoriesJob?.cancel()
        updateCategoriesJob = launch(bgDispatcher) {
            updatePostCategoriesUseCase.updateCategories(categoryList, editPostRepository)
        }
    }

    private fun trackCategoriesChangedEvent() {
        // todo: Annmarie add tracking
        Log.d(javaClass.simpleName, "***=> trackCategoriesChangedEvent here")
//        if (wereCategoriesChanged()) {
//            analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_TAGS_CHANGED)
//        }
    }

    sealed class UiState(
        open val selectedCategories: HashSet<Long> = hashSetOf()) {
        data class InitialLoadUiState(
            override val selectedCategories: HashSet<Long>) : UiState()

        data class ContentUiState(
            val categories: ArrayList<CategoryNode>
        ) : UiState()
    }
}
