package org.wordpress.android.ui.posts

import androidx.annotation.DimenRes
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode.MAIN
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.analytics.AnalyticsTracker.Stat
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.TaxonomyStore.OnTermUploaded
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.ui.pages.SnackbarMessageHolder
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult
import org.wordpress.android.ui.posts.EditPostRepository.UpdatePostResult.Updated
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
    private val analyticsTrackerWrapper: AnalyticsTrackerWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper,
    private val dispatcher: Dispatcher,
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

    init {
        dispatcher.register(this)
    }

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
        _uiState.value = UiState(
                categoriesListItemUiState = createListItemUiState(
                        siteCategories = siteCategories,
                        selectedCategoryIds = postCategories
                )
        )
    }

    private fun createListItemUiState(
        siteCategories: ArrayList<CategoryNode>,
        selectedCategoryIds: List<Long>
    ): List<PrepublishingCategoriesListItemUiState> {
        val items: ArrayList<PrepublishingCategoriesListItemUiState> = arrayListOf()
        siteCategories.forEachIndexed { index, categoryNode ->
            val itemUiState = if (selectedCategoryIds.contains(categoryNode.categoryId)) {
                PrepublishingCategoriesListItemUiState(
                        categoryNode = categoryNode, checked = true, onItemTapped = { onCategoryToggled(index, false) }
                )
            } else {
                PrepublishingCategoriesListItemUiState(
                        categoryNode = categoryNode, checked = false, onItemTapped = { onCategoryToggled(index, true) }
                )
            }
            items.add(itemUiState)
        }

        return items
    }

    fun onBackButtonClick() {
        saveAndFinish()
    }

    fun onAddCategoryClick() {
        _navigateToAddCategoryScreen.postValue(Event(Unit))
    }

    override fun onCleared() {
        super.onCleared()
        dispatcher.unregister(this)
    }

    private fun onCategoryToggled(position: Int, checked: Boolean) {
        uiState.value?.let {
            val currentUiState = it
            val updatedUiState = getUpdatedListState(position, checked)

            _uiState.value = currentUiState.copy(categoriesListItemUiState = updatedUiState)
        }
    }

    private fun getUpdatedListState(
        position: Int,
        checked: Boolean
    ): List<PrepublishingCategoriesListItemUiState> {
        val currentUiState = uiState.value as UiState
        val newListItemUiState = currentUiState.categoriesListItemUiState.toMutableList()
        newListItemUiState[position] = currentUiState.categoriesListItemUiState[position].copy(
                checked = checked
        )
        return newListItemUiState
    }

    private fun setToolbarTitleUiState() {
        _toolbarTitleUiState.value = UiStringRes(string.prepublishing_nudges_toolbar_title_categories)
    }

    private fun saveAndFinish() {
        updateCategories()
        _navigateToHomeScreen.postValue(Event(Unit))
    }

    private fun updateCategories() {
        if (!hasChanges()) return

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

    private fun postUpdatedCategories(categoryList: List<Long>, editPostRepository: EditPostRepository) {
        editPostRepository.updateAsync({ postModel ->
            postModel.setCategoryIdList(categoryList)
            true
        }, { _: PostImmutableModel?, result: UpdatePostResult ->
            if (result == Updated) {
                analyticsTrackerWrapper.trackPrepublishingNudges(Stat.EDITOR_POST_CATEGORIES_ADDED)
            }
        })
    }

    private fun getSelectedIds(): List<Long> {
        val uiState = uiState.value as UiState
        return uiState.categoriesListItemUiState.filter { x -> x.checked }
                .map { id -> id.categoryNode.categoryId }
                .toList()
    }

    private fun getSiteCategories() =
            getCategoriesUseCase.getSiteCategories(siteModel)

    private fun getPostCategories() =
            getCategoriesUseCase.getPostCategories(editPostRepository, siteModel)

    private fun hasChanges(): Boolean {
        val postCategories = getPostCategories()
        val stateSelectedCategories = getSelectedIds()
        return (stateSelectedCategories != postCategories)
    }

    private fun onTermUploadedComplete(event: OnTermUploaded) {
        val message = if (event.isError) {
            string.adding_cat_failed
        } else {
            string.adding_cat_success
        }
        _snackbarEvents.postValue(Event(SnackbarMessageHolder(UiStringRes(message))))

        if (!event.isError) {
            val categoryLevels = getSiteCategories()
            val selectedCategoryIds = getSelectedIds().toMutableList()
            selectedCategoryIds.add(event.term.remoteTermId)
            _uiState.value = UiState(
                    categoriesListItemUiState = createListItemUiState(
                            categoryLevels,
                            selectedCategoryIds
                    )
            )
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = MAIN)
    fun onTermUploaded(event: OnTermUploaded) {
        onTermUploadedComplete(event)
    }

    data class UiState(
        val categoriesListItemUiState: List<PrepublishingCategoriesListItemUiState> = listOf()
    )

    data class PrepublishingCategoriesListItemUiState(
        val onItemTapped: ((Int) -> Unit)? = null,
        val clickable: Boolean = true,
        val categoryNode: CategoryNode,
        val checked: Boolean = false,
        @DimenRes val verticalPaddingResId: Int = R.dimen.margin_large,
        @DimenRes val horizontalPaddingResId: Int = R.dimen.margin_extra_large
    )
}
