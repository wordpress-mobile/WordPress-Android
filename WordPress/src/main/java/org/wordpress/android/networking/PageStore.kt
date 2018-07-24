package org.wordpress.android.networking

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
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

    suspend fun getPageByLocalId(pageId: Int, site: SiteModel): PageModel? = withContext(CommonPool) {
        val post = postStore.getPostByLocalPostId(pageId)
        return@withContext post?.let {
            val page = PageModel.fromPost(it, site)
            page.parent = getPageByRemoteId(page.parentId, site)
            page
        }
    }

    suspend fun getPageByRemoteId(remoteId: Long, site: SiteModel): PageModel? = withContext(CommonPool) {
        val post = postStore.getPostByRemotePostId(remoteId, site)
        return@withContext post?.let {
            val page = PageModel.fromPost(it, site)
            page.parent = getPageByRemoteId(page.parentId, site)
            page
        }
    }

    suspend fun getPages(site: SiteModel): List<PageModel> = withContext(CommonPool) {
        val posts = postStore.getPagesForSite(site).filter { it != null }
        val pages = posts.map { PageModel.fromPost(it, site) }
        pages.forEach { page ->
            page.parent = pages.firstOrNull { it.remoteId == page.parentId }
            if (page.parentId != 0L && page.parent == null) {
                page.parent = getPageByRemoteId(page.parentId, site)
            }
        }
        pages.sortedBy { it.remoteId }
    }

    suspend fun requestPagesFromServer(site: SiteModel, loadMore: Boolean): OnPostChanged = suspendCoroutine { cont ->
        val payload = FetchPostsPayload(site, loadMore)
        postLoadContinuation = cont
        dispatcher.dispatch(PostActionBuilder.newFetchPagesAction(payload))
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        postLoadContinuation?.resume(event)
        postLoadContinuation = null
    }
}
