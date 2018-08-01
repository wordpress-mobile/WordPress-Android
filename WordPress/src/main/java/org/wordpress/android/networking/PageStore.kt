package org.wordpress.android.networking

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.models.pages.PageModel
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

@Singleton
class PageStore @Inject constructor(private val postStore: PostStore, private val dispatcher: Dispatcher) {
    private var postLoadContinuation: Continuation<OnPostChanged>? = null

    init {
        dispatcher.register(this)
    }

    suspend fun search(site: SiteModel, searchQuery: String): List<PageModel> = withContext(CommonPool) {
        postStore.getPagesForSite(site)
                .filter { it != null && it.title.toLowerCase().contains(searchQuery.toLowerCase()) }
                .map { PageModel(it) }
    }

    suspend fun loadPagesFromDb(site: SiteModel): List<PageModel> = withContext(CommonPool) {
        val pages = postStore.getPagesForSite(site).filter { it != null }
        pages.map { PageModel(it) }
    }

    suspend fun requestPagesFromServer(site: SiteModel, loadMore: Boolean): OnPostChanged = suspendCoroutine { cont ->
        val payload = FetchPostsPayload(site, loadMore)
        postLoadContinuation = cont
        dispatcher.dispatch(PostActionBuilder.newFetchPagesAction(payload))
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        if (event.causeOfChange == PostAction.FETCH_PAGES) {
            postLoadContinuation?.resume(event)
            postLoadContinuation = null
        }
    }
}
