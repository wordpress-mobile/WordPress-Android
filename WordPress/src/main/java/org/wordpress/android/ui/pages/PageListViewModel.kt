package org.wordpress.android.ui.pages

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import javax.inject.Inject

class PageListViewModel
@Inject constructor(
    val dispatcher: Dispatcher,
    private val postStore: PostStore
) : ViewModel() {
    private var initialized: Boolean = false
    private val mutableData = MutableLiveData<UiModel>()
    private val mutableState = MutableLiveData<UiState>()
    private var site: SiteModel? = null

    val data: LiveData<UiModel> = mutableData
    val state: LiveData<UiState> = mutableState

    fun attach() {
        dispatcher.register(this)
    }

    fun start(site: SiteModel) {
        this.site = site
        mutableData.postValue(UiModel(pages = postStore.getPagesForSite(site)))
        if (!initialized) {
            mutableState.postValue(UiState(loading = true))
            dispatcher.dispatch(PostActionBuilder.newFetchPagesAction(FetchPostsPayload(site)))
            initialized = true
        }
    }

    fun nextPage() {
        if (mutableState.value?.hasMore == true) {
            mutableState.postValue(UiState(loadingNext = true))
            dispatcher.dispatch(PostActionBuilder.newFetchPagesAction(FetchPostsPayload(site, true)))
        }
    }

    fun refresh() {
        mutableState.postValue(UiState(refreshing = true))
        dispatcher.dispatch(PostActionBuilder.newFetchPagesAction(FetchPostsPayload(site)))
    }

    fun detach() {
        this.site = null
        dispatcher.unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        if (event.causeOfChange == PostAction.FETCH_PAGES) {
            mutableState.postValue(UiState(error = event.error?.message, hasMore = event.canLoadMore))
            if (!event.isError) {
                mutableData.postValue(UiModel(pages = postStore.getPagesForSite(site)))
            }
        }
    }

    data class UiModel(
        val pages: List<PostModel>
    )

    data class UiState(
        val loading: Boolean = false,
        val refreshing: Boolean = false,
        val loadingNext: Boolean = false,
        val hasMore: Boolean = false,
        val error: String? = null
    )
}
