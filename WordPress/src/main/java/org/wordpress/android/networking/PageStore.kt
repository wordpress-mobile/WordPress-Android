package org.wordpress.android.networking

import android.content.Context
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.models.pages.PageStatus
import org.wordpress.android.models.pages.updatePageData
import org.wordpress.android.networking.PageStore.UploadRequestResult.ERROR_EMPTY_PAGE
import org.wordpress.android.networking.PageStore.UploadRequestResult.ERROR_NO_NETWORK
import org.wordpress.android.networking.PageStore.UploadRequestResult.SUCCESS
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.NetworkUtils
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.models.pages.PageStatus.DRAFT
import org.wordpress.android.models.pages.PageStatus.PUBLISHED
import org.wordpress.android.models.pages.PageStatus.SCHEDULED
import java.util.SortedMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.experimental.Continuation
import kotlin.coroutines.experimental.suspendCoroutine

@Singleton
class PageStore @Inject constructor(
    private val postStore: PostStore,
    private val dispatcher: Dispatcher,
    private val context: Context
) {
    private var postLoadContinuation: Continuation<OnPostChanged>? = null
    private var site: SiteModel? = null

    init {
        dispatcher.register(this)
    }

    suspend fun savePage(page: PageModel): UploadRequestResult = withContext(CommonPool) {
        val post = postStore.getPostByRemotePostId(page.remoteId, page.site)
        post.updatePageData(page)

        if (!NetworkUtils.isNetworkAvailable(context)) {
            return@withContext ERROR_NO_NETWORK
        }

        if (!PostUtils.isPublishable(post)) {
            return@withContext ERROR_EMPTY_PAGE
        }

        PostUtils.updatePublishDateIfShouldBePublishedImmediately(post)
        val isFirstTimePublish = PostStatus.fromPost(post) == PostStatus.DRAFT ||
                PostStatus.fromPost(post) == PostStatus.PUBLISHED && post.isLocalDraft

        // save the post in the DB so the UploadService will get the latest change
        dispatcher.dispatch(PostActionBuilder.newUpdatePostAction(post))

        if (isFirstTimePublish) {
            UploadService.uploadPostAndTrackAnalytics(context, post)
        } else {
            UploadService.uploadPost(context, post)
        }

        PostUtils.trackSavePostAnalytics(post, page.site)

        return@withContext SUCCESS
    }

    suspend fun getPageByLocalId(pageId: Int, site: SiteModel): PageModel? = withContext(CommonPool) {
        val post = postStore.getPostByLocalPostId(pageId)
        return@withContext post?.let {
            val page = PageModel(it, site)
            if (page.parentId != 0L) {
                page.parent = getPageByRemoteId(page.parentId, site)
            }
            page
        }
    }

    suspend fun getPageByRemoteId(remoteId: Long, site: SiteModel): PageModel? = withContext(CommonPool) {
        val post = postStore.getPostByRemotePostId(remoteId, site)
        return@withContext post?.let {
            val page = PageModel(it, site)
            if (page.parentId != 0L) {
                page.parent = getPageByRemoteId(page.parentId, site)
            }
            page
        }
    }

    suspend fun getPages(site: SiteModel): List<PageModel> = withContext(CommonPool) {
        val posts = postStore.getPagesForSite(site).filter { it != null }
        val pages = posts.map { PageModel(it, site) }
        pages.forEach { page ->
            if (page.parentId != 0L) {
                page.parent = pages.firstOrNull { it.remoteId == page.parentId }
                if (page.parent == null) {
                    page.parent = getPageByRemoteId(page.parentId, site)
                }
            }
        }
        pages.sortedBy { it.remoteId }
    }

    suspend fun search(site: SiteModel, searchQuery: String): List<PageModel> = withContext(CommonPool) {
        postStore.getPagesForSite(site)
                .filterNotNull()
                .map { PageModel(it, site) }
                .filter { it.title.toLowerCase().contains(searchQuery.toLowerCase()) }
    }

    suspend fun groupedSearch(
        site: SiteModel,
        searchQuery: String
    ): SortedMap<PageStatus, List<PageModel>> = withContext(CommonPool) {
        val list = search(site, searchQuery)
                .groupBy { it.status }
        list
                .toSortedMap(Comparator { previous, next ->
                    when {
                        previous == next -> 0
                        previous == PUBLISHED -> -1
                        next == PUBLISHED -> 1
                        previous == DRAFT -> -1
                        next == DRAFT -> 1
                        previous == SCHEDULED -> -1
                        next == SCHEDULED -> 1
                        else -> {
                            throw IllegalArgumentException("Unexpected page type")
                        }
                    }
                })
    }

    suspend fun loadPagesFromDb(site: SiteModel): List<PageModel> = withContext(CommonPool) {
        val pages = postStore.getPagesForSite(site).filter { it != null }
        pages.map { PageModel(it, site) }
    }

    suspend fun requestPagesFromServer(site: SiteModel): OnPostChanged = suspendCoroutine { cont ->
        this.site = site
        postLoadContinuation = cont
        requestMore(site, false)
    }

    private fun requestMore(site: SiteModel, loadMore: Boolean) {
        val payload = FetchPostsPayload(site, loadMore)
        dispatcher.dispatch(PostActionBuilder.newFetchPagesAction(payload))
    }

    fun isPageUploading(pageId: Long, site: SiteModel): Boolean {
        val post = postStore.getPostByRemotePostId(pageId, site)
        return UploadService.isPostUploadingOrQueued(post)
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        if (event.causeOfChange == PostAction.FETCH_PAGES) {
            if (event.canLoadMore && site != null) {
                requestMore(site!!, true)
            } else {
                postLoadContinuation?.resume(event)
                postLoadContinuation = null
            }
        }
    }

    enum class UploadRequestResult {
        SUCCESS,
        ERROR_NO_NETWORK,
        ERROR_EMPTY_PAGE
    }
}
