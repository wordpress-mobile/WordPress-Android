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
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PageStore.UploadRequestResult.ERROR_NON_EXISTING_PAGE
import org.wordpress.android.fluxc.store.PageStore.UploadRequestResult.SUCCESS
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.util.DateTimeUtils
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine
import org.wordpress.android.fluxc.persistence.PostSqlUtils

@Singleton
class PageStore @Inject constructor(private val postStore: PostStore, private val dispatcher: Dispatcher) {
    companion object {
        val PAGE_TYPES = listOf(
                PostStatus.DRAFT,
                PostStatus.PUBLISHED,
                PostStatus.SCHEDULED,
                PostStatus.PENDING,
                PostStatus.PRIVATE,
                PostStatus.TRASHED)
    }
    private var postLoadContinuation: Continuation<OnPostChanged>? = null
    private var deletePostContinuation: Continuation<OnPostChanged>? = null
    private var updatePostContinuation: Continuation<OnPostChanged>? = null
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
        if (remoteId == 0L) {
            return@withContext null
        }
        val post = postStore.getPostByRemotePostId(remoteId, site)
        return@withContext post?.let {
            PageModel(it, site, getPageByRemoteId(it.parentId, site))
        }
    }

    suspend fun search(site: SiteModel, searchQuery: String): List<PageModel> = withContext(CommonPool) {
        getPagesFromDb(site).filter { it.title.toLowerCase().contains(searchQuery.toLowerCase()) }
    }

    suspend fun updatePageInDb(page: PageModel): OnPostChanged = suspendCoroutine { cont ->
        updatePostContinuation = cont

        val post = postStore.getPostByRemotePostId(page.remoteId, page.site)
        post.updatePageData(page)

        val updateAction = PostActionBuilder.newUpdatePostAction(post)
        dispatcher.dispatch(updateAction)
    }

    suspend fun uploadPageToServer(page: PageModel): UploadRequestResult = withContext(CommonPool) {
        val post = postStore.getPostByRemotePostId(page.remoteId, page.site)
        if (post != null) {
            post.updatePageData(page)

            val action = PostActionBuilder.newPushPostAction(RemotePostPayload(post, page.site))
            dispatcher.dispatch(action)

            return@withContext SUCCESS
        } else {
            return@withContext ERROR_NON_EXISTING_PAGE
        }
    }

    enum class UploadRequestResult {
        SUCCESS,
        ERROR_NON_EXISTING_PAGE
    }

    suspend fun getPagesFromDb(site: SiteModel): List<PageModel> = withContext(CommonPool) {
        val posts = postStore.getPagesForSite(site).asSequence().filterNotNull().associateBy { it.remotePostId }
        posts.map { getPageFromPost(it.key, site, posts) }.filterNotNull().sortedBy { it.remoteId }
    }

    private fun getPageFromPost(postId: Long, site: SiteModel, posts: Map<Long, PostModel>): PageModel? {
        if (postId == 0L || !posts.containsKey(postId)) {
            return null
        }
        val post = posts[postId]!!
        return PageModel(post, site, getPageFromPost(post.parentId, site, posts))
    }

    suspend fun deletePageFromServer(page: PageModel): OnPostChanged = suspendCoroutine { cont ->
        deletePostContinuation = cont

        val post = postStore.getPostByLocalPostId(page.pageId)
        if (post != null) {
            val payload = RemotePostPayload(post, page.site)
            dispatcher.dispatch(PostActionBuilder.newDeletePostAction(payload))
        } else {
            val event = OnPostChanged(0)
            event.error = PostError(PostErrorType.UNKNOWN_POST)
            cont.resume(event)
        }
    }

    suspend fun deletePageFromDb(page: PageModel): Boolean = withContext(CommonPool) {
        val post = postStore.getPostByLocalPostId(page.pageId)
        return@withContext if (post != null) {
            PostSqlUtils.deletePost(post) > 0
        } else {
            false
        }
    }

    suspend fun requestPagesFromServer(site: SiteModel): OnPostChanged = suspendCoroutine { cont ->
        this.site = site
        postLoadContinuation = cont
        fetchPages(site, false)
    }

    private fun fetchPages(site: SiteModel, loadMore: Boolean) {
        val payload = FetchPostsPayload(site, loadMore, PAGE_TYPES)
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
            PostAction.DELETE_POST -> {
                deletePostContinuation?.resume(event)
                deletePostContinuation = null
            }
            PostAction.UPDATE_POST -> {
                updatePostContinuation?.resume(event)
                updatePostContinuation = null
            }
            else -> {}
        }
    }

    private fun PostModel.updatePageData(page: PageModel) {
        this.id = page.pageId
        this.title = page.title
        this.status = page.status.toPostStatus().toString()
        this.parentId = page.parent?.remoteId ?: 0
        this.remotePostId = page.remoteId
        this.dateCreated = DateTimeUtils.iso8601FromDate(page.date)
    }
}
