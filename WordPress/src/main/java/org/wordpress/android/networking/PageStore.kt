package org.wordpress.android.networking

import android.content.Context
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.models.pages.PageModel
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.action.PostAction
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.networking.PageStore.UploadRequestResult.ERROR_EMPTY_PAGE
import org.wordpress.android.networking.PageStore.UploadRequestResult.ERROR_NO_NETWORK
import org.wordpress.android.networking.PageStore.UploadRequestResult.SUCCESS
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.NetworkUtils
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

    init {
        dispatcher.register(this)
    }

    suspend fun uploadPage(page: PageModel): UploadRequestResult = withContext(CommonPool) {
        val post = postStore.getPostByLocalPostId(page.pageId)

        if (!NetworkUtils.isNetworkAvailable(context)) {
            return@withContext ERROR_NO_NETWORK
        }

        if (!PostUtils.isPublishable(post)) {
            return@withContext ERROR_EMPTY_PAGE
        }

        PostUtils.updatePublishDateIfShouldBePublishedImmediately(post)
        val isFirstTimePublish = PostStatus.fromPost(post) == PostStatus.DRAFT ||
                PostStatus.fromPost(post) == PostStatus.PUBLISHED && post.isLocalDraft
        post.status = PostStatus.PUBLISHED.toString()

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

    enum class UploadRequestResult {
        SUCCESS,
        ERROR_NO_NETWORK,
        ERROR_EMPTY_PAGE
    }
}
