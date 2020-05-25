package org.wordpress.android.ui.prefs

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.store.SiteOptionsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class HomepageSettingsViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val homepageSettingsDataLoader: HomepageSettingsDataLoader,
    private val siteStore: SiteStore,
    private val siteOptionsStore: SiteOptionsStore
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<HomepageSettingsUiState>()
    val uiState: LiveData<HomepageSettingsUiState> = _uiState
    private val _dismissDialogEvent = MutableLiveData<Event<Unit>>()
    val dismissDialogEvent: LiveData<Event<Unit>> = _dismissDialogEvent

    fun classicBlogSelected() {
        updateUiState { it.copy(isClassicBlogState = true) }
    }

    fun staticHomepageSelected() {
        updateUiState { it.copy(isClassicBlogState = false) }
    }

    fun onPageOnFrontSelected(id: Int): Boolean {
        return updatePageOnFrontState { requireNotNull(it).selectItem(id) }
    }

    fun onPageForPostsSelected(id: Int): Boolean {
        return updatePageForPostsState { requireNotNull(it).selectItem(id) }
    }

    fun onPageOnFrontDialogOpened() {
        onDialogStateChange(pageOnFrontDialogOpened = true)
    }

    fun onPageForPostsDialogOpened() {
        onDialogStateChange(pageForPostsDialogOpened = true)
    }

    fun onDialogHidden() {
        onDialogStateChange()
    }

    private fun onDialogStateChange(
        pageOnFrontDialogOpened: Boolean = false,
        pageForPostsDialogOpened: Boolean = false
    ) {
        updateUiState { pagesUiState ->
            pagesUiState.copy(
                    pageOnFrontModel = pagesUiState.pageOnFrontModel?.copy(
                            isClicked = pageOnFrontDialogOpened
                    ),
                    pageForPostsModel = pagesUiState.pageForPostsModel?.copy(
                            isClicked = pageForPostsDialogOpened
                    )
            )
        }
    }

    fun onAcceptClicked() {
        val currentUiState = _uiState.value
        if (currentUiState == null) {
            updateUiState { it.updateWithError(R.string.site_settings_failed_to_save_homepage_settings) }
            return
        }
        _uiState.value = currentUiState.copy(isDisabled = true, isLoading = true)
        launch {
            val siteHomepageSettings = if (currentUiState.isClassicBlogState) {
                SiteHomepageSettings.Posts
            } else {
                val pageForPostsModel = currentUiState.pageForPostsModel
                val pageOnFrontModel = currentUiState.pageOnFrontModel
                if (pageForPostsModel == null || pageOnFrontModel == null) {
                    updateUiState {
                        it.updateWithError(R.string.site_settings_cannot_save_homepage_settings_before_pages_loaded)
                    }
                    return@launch
                }

                SiteHomepageSettings.StaticPage(
                        pageForPostsId = pageForPostsModel.getSelectedItemRemoteId() ?: 0,
                        pageOnFrontId = pageOnFrontModel.getSelectedItemRemoteId() ?: 0
                )
            }
            val updateResult = siteOptionsStore.updateHomepage(currentUiState.siteModel, siteHomepageSettings)
            _uiState.value = currentUiState.copy(isDisabled = false, isLoading = false)
            when (updateResult.isError) {
                true -> {
                    updateUiState { it.updateWithError(R.string.site_settings_failed_update_homepage_settings) }
                }
                false -> _dismissDialogEvent.value = Event(Unit)
            }
        }
    }

    fun onDismissClicked() {
        _dismissDialogEvent.value = Event(Unit)
    }

    fun getSelectedPageOnFrontId(): Long? {
        return (_uiState.value?.pageOnFrontModel)?.getSelectedItemRemoteId()
    }

    fun getSelectedPageForPostsId(): Long? {
        return (_uiState.value?.pageForPostsModel)?.getSelectedItemRemoteId()
    }

    fun isClassicBlog(): Boolean? {
        return _uiState.value?.isClassicBlogState
    }

    fun start(siteId: Int, savedClassicBlogValue: Boolean?, savedPageForPostsId: Long?, savedPageOnFrontId: Long?) {
        launch {
            val siteModel = siteStore.getSiteByLocalId(siteId)
                    ?: throw IllegalStateException("SiteModel has to be present")
            val isClassicBlog = savedClassicBlogValue ?: siteModel.showOnFront == ShowOnFront.POSTS.value
            _uiState.value = HomepageSettingsUiState(
                    isClassicBlogState = isClassicBlog,
                    siteModel = siteModel
            )
            val pageOnFrontId = savedPageOnFrontId ?: siteModel.pageOnFront
            val pageForPostsId = savedPageForPostsId ?: siteModel.pageForPosts
            val loadPagesFlow = homepageSettingsDataLoader.loadPages(
                    siteModel,
                    pageOnFrontId,
                    pageForPostsId
            )

            withContext(bgDispatcher) {
                loadPagesFlow.collect { loadingResult ->
                    updateUiState { currentValue ->
                        currentValue.updateWithLoadingResult(loadingResult, pageForPostsId, pageOnFrontId)
                    }
                }
            }
        }
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onSiteChanged(event: OnSiteChanged) {
        val currentUiState = _uiState.value
        if (!event.isError && currentUiState != null) {
            val siteModel = currentUiState.siteModel
            val updatedSite = siteStore.getSiteByLocalId(siteModel.id)
                    ?: throw IllegalStateException("SiteModel has to be present")
            if (updatedSite.showOnFront != siteModel.showOnFront ||
                    updatedSite.pageOnFront != siteModel.pageOnFront ||
                    updatedSite.pageForPosts != siteModel.pageForPosts) {
                updateUiState { it.copy(siteModel = updatedSite) }
            }
        }
    }

    private fun updatePageForPostsState(copyFunction: (PageSelectorUiModel?) -> PageSelectorUiModel): Boolean {
        return updateUiState { uiState ->
            uiState.copy(pageForPostsModel = copyFunction(uiState.pageForPostsModel))
        }
    }

    private fun updatePageOnFrontState(copyFunction: (PageSelectorUiModel?) -> PageSelectorUiModel): Boolean {
        return updateUiState { uiState ->
            uiState.copy(pageOnFrontModel = copyFunction(uiState.pageOnFrontModel))
        }
    }

    private fun updateUiState(copyFunction: (HomepageSettingsUiState) -> HomepageSettingsUiState): Boolean {
        val currentState = _uiState.value
        return if (currentState != null) {
            _uiState.value = copyFunction(currentState)
            true
        } else {
            false
        }
    }
}
