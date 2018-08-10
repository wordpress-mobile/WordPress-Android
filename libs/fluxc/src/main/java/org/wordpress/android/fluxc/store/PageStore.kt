package org.wordpress.android.fluxc.store

import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.page.PageStatus
import org.wordpress.android.fluxc.model.page.PageStatus.DRAFT
import org.wordpress.android.fluxc.model.page.PageStatus.PUBLISHED
import org.wordpress.android.fluxc.model.page.PageStatus.SCHEDULED
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

@Singleton
class PageStore @Inject constructor(private val postStore: PostStore, private val dispatcher: Dispatcher) {
    private var postLoadContinuation: Continuation<OnPostChanged>? = null
    private var deletePostContinuation: Continuation<OnPostChanged>? = null
    private var site: SiteModel? = null

    init {
        dispatcher.register(this)
    }

    suspend fun getPageByLocalId(pageId: Int, site: SiteModel): PageModel? = withContext(CommonPool) {
        val post = postStore.getPostByLocalPostId(pageId)
        return@withContext post?.let {
            PageModel(it, site, getPageByRemoteId(it.parentId, site))
        }
    }

    suspend fun getPageByRemoteId(remoteId: Long, site: SiteModel): PageModel? = withContext(CommonPool) {
        val post = postStore.getPostByRemotePostId(remoteId, site)
        return@withContext post?.let {
            PageModel(it, site, getPageByRemoteId(it.parentId, site))
        }
    }

    suspend fun search(site: SiteModel, searchQuery: String): List<PageModel> = withContext(CommonPool) {
        getPagesFromDb(site).filter { it.title.toLowerCase().contains(searchQuery.toLowerCase()) }
    }

    suspend fun groupedSearch(
        site: SiteModel,
        searchQuery: String
    ): SortedMap<PageStatus, List<PageModel>> = withContext(CommonPool) {
        val list = search(site, searchQuery).groupBy { it.status }
        return@withContext list.toSortedMap(
                Comparator { previous, next ->
                    when {
                        previous == next -> 0
                        previous == PUBLISHED -> -1
                        next == PUBLISHED -> 1
                        previous == DRAFT -> -1
                        next == DRAFT -> 1
                        previous == SCHEDULED -> -1
                        next == SCHEDULED -> 1
                        else -> throw IllegalArgumentException("Unexpected page type")
                    }
                })
    }

    suspend fun getPagesFromDb(site: SiteModel): List<PageModel> = withContext(CommonPool) {
        val posts = postStore.getPagesForSite(site).filterNotNull().associateBy { it.remotePostId }
        posts.map { getPageFromPost(it.key, site, posts) }.filterNotNull().sortedBy { it.remoteId }
    }

    private fun getPageFromPost(postId: Long, site: SiteModel, posts: Map<Long, PostModel>): PageModel? {
        if (postId == 0L || !posts.containsKey(postId)) {
            return null
        }
        val post = posts[postId]!!
        return PageModel(post, site, getPageFromPost(post.parentId, site, posts))
    }

    suspend fun deletePage(page: PageModel): OnPostChanged = suspendCoroutine { cont ->
        deletePostContinuation = cont

        val post = postStore.getPostByLocalPostId(page.pageId)
        val payload = RemotePostPayload(post, page.site)
        dispatcher.dispatch(PostActionBuilder.newDeletePostAction(payload))
    }

    suspend fun requestPagesFromServer(site: SiteModel): OnPostChanged = suspendCoroutine { cont ->
        this.site = site
        postLoadContinuation = cont
        fetchPages(site, false)
    }

    private fun fetchPages(site: SiteModel, loadMore: Boolean) {
        val payload = FetchPostsPayload(site, loadMore,
                listOf(PostStatus.DRAFT, PostStatus.PUBLISHED, PostStatus.SCHEDULED, PostStatus.TRASHED))
        dispatcher.dispatch(PostActionBuilder.newFetchPagesAction(payload))
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        when (event.causeOfChange) {
            PostAction.FETCH_PAGES -> {
                if (event.canLoadMore && site != null) {
                    fetchPages(site!!, true)
                } else {
                    postLoadContinuation?.resume(event)
                    postLoadContinuation = null
                }
            }
            PostAction.DELETED_POST -> {
                deletePostContinuation?.resume(event)
            }
            else -> {}
        }
    }
}
