package org.wordpress.android.ui.prefs.homepage

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.R
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteHomepageSettings
import org.wordpress.android.fluxc.model.SiteHomepageSettings.ShowOnFront
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.store.SiteOptionsStore
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged
import org.wordpress.android.modules.BG_THREAD
import org.wordpress.android.modules.UI_THREAD
import org.wordpress.android.ui.prefs.homepage.HomepageSettingsDataLoader.LoadingResult.Data
import org.wordpress.android.viewmodel.Event
import org.wordpress.android.viewmodel.ScopedViewModel
import javax.inject.Inject
import javax.inject.Named

class HomepageSettingsViewModel
@Inject constructor(
    @Named(UI_THREAD) private val mainDispatcher: CoroutineDispatcher,
    @Named(BG_THREAD) private val bgDispatcher: CoroutineDispatcher,
    private val dispatcher: Dispatcher,
    private val homepageSettingsDataLoader: HomepageSettingsDataLoader,
    private val siteStore: SiteStore,
    private val siteOptionsStore: SiteOptionsStore
) : ScopedViewModel(mainDispatcher) {
    private val _uiState = MutableLiveData<HomepageSettingsUiState>()
    val uiState: LiveData<HomepageSettingsUiState> = _uiState
    private val _dismissDialogEvent = MutableLiveData<Event<Unit>>()
    val dismissDialogEvent: LiveData<Event<Unit>> = _dismissDialogEvent

    fun classicBlogSelected() {
        updateUiStateFromMainThread { it.copy(isClassicBlogState = true) }
    }

    fun staticHomepageSelected() {
        updateUiStateFromMainThread { it.copy(isClassicBlogState = false) }
    }

    fun onPageOnFrontSelected(id: Int): Boolean {
        return updateUiStateFromMainThread { it.updateWithPageOnFront(id) }
    }

    fun onPageForPostsSelected(id: Int): Boolean {
        return updateUiStateFromMainThread { it.updateWithPageForPosts(id) }
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
        updateUiStateFromMainThread { pagesUiState ->
            pagesUiState.copy(
                    pageOnFrontState = pagesUiState.pageOnFrontState?.copy(
                            isExpanded = pageOnFrontDialogOpened
                    ),
                    pageForPostsState = pagesUiState.pageForPostsState?.copy(
                            isExpanded = pageForPostsDialogOpened
                    )
            )
        }
    }

    fun onAcceptClicked() {
        val currentUiState = _uiState.value
        if (currentUiState == null) {
            updateUiStateFromMainThread { it.updateWithError(R.string.site_settings_failed_to_save_homepage_settings) }
            return
        }
        updateUiStateFromMainThread { it.copy(isDisabled = true, isLoading = true) }
        launch {
            val siteHomepageSettings = if (currentUiState.isClassicBlogState) {
                SiteHomepageSettings.Posts
            } else {
                val pageForPostsModel = currentUiState.pageForPostsState
                val pageOnFrontModel = currentUiState.pageOnFrontState
                if (pageForPostsModel == null || pageOnFrontModel == null) {
                    updateUiState {
                        it.updateWithError(R.string.site_settings_cannot_save_homepage_settings_before_pages_loaded)
                    }
                    return@launch
                }
                if (pageForPostsModel == pageOnFrontModel) {
                    updateUiState {
                        it.updateWithError(R.string.site_settings_page_for_posts_and_homepage_cannot_be_equal)
                    }
                    return@launch
                }

                SiteHomepageSettings.StaticPage(
                        pageForPostsId = pageForPostsModel.getSelectedItemRemoteId() ?: 0,
                        pageOnFrontId = pageOnFrontModel.getSelectedItemRemoteId() ?: 0
                )
            }
            val updateResult = siteOptionsStore.updateHomepage(currentUiState.siteModel, siteHomepageSettings)
            updateUiState { it.copy(isDisabled = false, isLoading = false) }
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
        return _uiState.value?.pageOnFrontState?.getSelectedItemRemoteId()
    }

    fun getSelectedPageForPostsId(): Long? {
        return _uiState.value?.pageForPostsState?.getSelectedItemRemoteId()
    }

    fun isClassicBlog(): Boolean? {
        return _uiState.value?.isClassicBlogState
    }

    fun start(siteId: Int, savedClassicBlogValue: Boolean?, savedPageForPostsId: Long?, savedPageOnFrontId: Long?) {
        dispatcher.register(this)
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
                    siteModel
            )

            withContext(bgDispatcher) {
                loadPagesFlow.filter { loadingResult ->
                    loadingResult !is Data || loadingResult.pages.isValid(pageOnFrontId, pageForPostsId)
                }
                        .collect { loadingResult ->
                            updateUiState { currentValue ->
                                currentValue.updateWithLoadingResult(loadingResult, pageForPostsId, pageOnFrontId)
                            }
                        }
            }
        }
    }

    override fun onCleared() {
        dispatcher.unregister(this)
        super.onCleared()
    }

    private fun List<PageModel>.isValid(
        pageOnFrontRemoteId: Long,
        pageForPostsRemoteId: Long
    ): Boolean {
        return this.isNotEmpty() && this.hasPage(pageOnFrontRemoteId) && this.hasPage(pageForPostsRemoteId)
    }

    private fun List<PageModel>.hasPage(remoteId: Long): Boolean {
        return remoteId <= 0 || this.any { it.remoteId == remoteId }
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
                updateUiStateFromMainThread { it.copy(siteModel = updatedSite) }
            }
        }
    }

    private suspend fun updateUiState(copyFunction: (HomepageSettingsUiState) -> HomepageSettingsUiState): Boolean {
        return withContext(mainDispatcher) {
            updateUiStateFromMainThread { copyFunction(it) }
        }
    }

    private fun updateUiStateFromMainThread(copyFunction: (HomepageSettingsUiState) -> HomepageSettingsUiState): Boolean {
        val currentState = _uiState.value
        return if (currentState != null) {
            _uiState.value = copyFunction(currentState)
            true
        } else {
            false
        }
    }
}
