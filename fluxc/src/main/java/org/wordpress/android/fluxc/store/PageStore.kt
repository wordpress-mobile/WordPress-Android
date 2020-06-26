package org.wordpress.android.fluxc.store

import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.CauseOfOnPostChanged
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.persistence.PostSqlUtils
import org.wordpress.android.fluxc.store.PageStore.UploadRequestResult.ERROR_NON_EXISTING_PAGE
import org.wordpress.android.fluxc.store.PageStore.UploadRequestResult.SUCCESS
import org.wordpress.android.fluxc.store.PostStore.FetchPostsPayload
import org.wordpress.android.fluxc.store.PostStore.OnPostChanged
import org.wordpress.android.fluxc.store.PostStore.PostDeleteActionType.DELETE
import org.wordpress.android.fluxc.store.PostStore.PostError
import org.wordpress.android.fluxc.store.PostStore.PostErrorType.UNKNOWN_POST
import org.wordpress.android.fluxc.store.PostStore.RemotePostPayload
import org.wordpress.android.fluxc.tools.CoroutineEngine
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.DateTimeUtils
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class PageStore @Inject constructor(
    private val postStore: PostStore,
    private val postSqlUtils: PostSqlUtils,
    private val dispatcher: Dispatcher,
    private val coroutineEngine: CoroutineEngine
) {
    companion object {
        val PAGE_TYPES = listOf(
                PostStatus.DRAFT,
                PostStatus.PUBLISHED,
                PostStatus.SCHEDULED,
                PostStatus.PENDING,
                PostStatus.PRIVATE,
                PostStatus.TRASHED
        )
    }

    private var postLoadContinuations: MutableList<Continuation<OnPostChanged>> = mutableListOf()
    private var deletePostContinuation: Continuation<OnPostChanged>? = null
    private var updatePostContinuation: Continuation<OnPostChanged>? = null

    private var fetchingSite: SiteModel? = null

    init {
        dispatcher.register(this)
    }

    suspend fun getPageByLocalId(pageId: Int, site: SiteModel): PageModel? =
            coroutineEngine.withDefaultContext(AppLog.T.POSTS, this, "getPageByLocalId") {
                val post = postStore.getPostByLocalPostId(pageId)
                return@withDefaultContext post?.let {
                    PageModel(it, site, getPageByRemoteId(it.parentId, site))
                }
            }

    suspend fun getPageByRemoteId(remoteId: Long, site: SiteModel): PageModel? =
            coroutineEngine.withDefaultContext(AppLog.T.POSTS, this, "getPageByRemoteId") {
                if (remoteId <= 0L) {
                    return@withDefaultContext null
                }
                val post = postStore.getPostByRemotePostId(remoteId, site)
                return@withDefaultContext post?.let {
                    PageModel(it, site, getPageByRemoteId(it.parentId, site))
                }
            }

    suspend fun search(site: SiteModel, searchQuery: String): List<PageModel> =
            coroutineEngine.withDefaultContext(AppLog.T.POSTS, this, "search") {
                getPagesFromDb(site).filter {
                    it.title.toLowerCase(Locale.ROOT)
                            .contains(searchQuery.toLowerCase(Locale.ROOT))
                }
            }

    suspend fun updatePageInDb(page: PageModel): OnPostChanged = suspendCoroutine { cont ->
        val post = postStore.getPostByRemotePostId(page.remoteId, page.site)
                ?: postStore.getPostByLocalPostId(page.pageId)
        if (post != null) {
            post.updatePageData(page)

            val updateAction = PostActionBuilder.newUpdatePostAction(post)
            updatePostContinuation = cont
            dispatcher.dispatch(updateAction)
        } else {
            val event = OnPostChanged(CauseOfOnPostChanged.UpdatePost(page.pageId, page.remoteId), 0)
            event.error = PostError(UNKNOWN_POST)
            cont.resume(event)
        }
    }

    suspend fun uploadPageToServer(page: PageModel): UploadRequestResult =
            coroutineEngine.withDefaultContext(AppLog.T.POSTS, this, "uploadPageToServer") {
                val post = postStore.getPostByRemotePostId(page.remoteId, page.site)
                        ?: postStore.getPostByLocalPostId(page.pageId)
                if (post != null) {
                    post.updatePageData(page)

                    val action = PostActionBuilder.newPushPostAction(RemotePostPayload(post, page.site))
                    dispatcher.dispatch(action)

                    return@withDefaultContext SUCCESS
                } else {
                    return@withDefaultContext ERROR_NON_EXISTING_PAGE
                }
            }

    enum class UploadRequestResult {
        SUCCESS,
        ERROR_NON_EXISTING_PAGE
    }

    suspend fun getPagesFromDb(site: SiteModel): List<PageModel> =
            coroutineEngine.withDefaultContext(AppLog.T.POSTS, this, "getPagesFromDb") {
                val posts = postStore.getPagesForSite(site)
                        .asSequence()
                        .filterNotNull()
                        .filter { PAGE_TYPES.contains(PostStatus.fromPost(it)) }
                        .map {
                            // local DB pages have a non-unique remote ID value of 0
                            // to keep the apart we replace it with page ID (still unique)
                            // and make it negative (to easily tell it's a temporary value)
                            if (it.remotePostId == 0L) {
                                /**
                                 * This hack is breaking the approach which we use for making sure we upload only changes which
                                 * were explicitly confirmed by the user. We are modifying the PostModel and we need to make
                                 * sure to retain the confirmation.
                                 */
                                val changesConfirmed = it.contentHashcode() == it.changesConfirmedContentHashcode
                                it.setRemotePostId(-it.id.toLong())
                                if (changesConfirmed) {
                                    it.setChangesConfirmedContentHashcode(it.contentHashcode())
                                }
                            }
                            it
                        }
                        .associateBy { it.remotePostId }

                return@withDefaultContext posts.map { getPageFromPost(it.key, site, posts, false) }
                        .filterNotNull()
                        .sortedBy { it.remoteId }
            }

    private fun getPageFromPost(
        postId: Long,
        site: SiteModel,
        posts: Map<Long, PostModel>,
        skipLocalPages: Boolean = true
    ): PageModel? {
        if (skipLocalPages && (postId <= 0L || !posts.containsKey(postId))) {
            return null
        }
        val post = posts[postId]!!
        return PageModel(post, site, getPageFromPost(post.parentId, site, posts))
    }

    suspend fun deletePageFromServer(page: PageModel): OnPostChanged = suspendCoroutine { cont ->
        val post = postStore.getPostByLocalPostId(page.pageId)
        if (post != null) {
            deletePostContinuation = cont
            val payload = RemotePostPayload(post, page.site)
            dispatcher.dispatch(PostActionBuilder.newDeletePostAction(payload))
        } else {
            val event = OnPostChanged(
                    CauseOfOnPostChanged.DeletePost(
                            localPostId = page.pageId,
                            remotePostId = page.remoteId,
                            postDeleteActionType = DELETE
                    ), 0
            )
            event.error = PostError(UNKNOWN_POST)
            cont.resume(event)
        }
    }

    suspend fun deletePageFromDb(page: PageModel): Boolean =
            coroutineEngine.withDefaultContext(AppLog.T.POSTS, this, "deletePageFromDb") {
                val post = postStore.getPostByLocalPostId(page.pageId)
                return@withDefaultContext if (post != null) {
                    postSqlUtils.deletePost(post) > 0
                } else {
                    false
                }
            }

    suspend fun requestPagesFromServer(site: SiteModel): OnPostChanged = suspendCoroutine { cont ->
        fetchingSite = site
        if (postLoadContinuations.isEmpty()) {
            fetchPages(site, false)
        }
        postLoadContinuations.add(cont)
    }

    /**
     * Get local draft pages that have not been uploaded to the server yet.
     *
     * This returns [PostModel] instead of [PageModel] to accommodate the `UploadService` in WPAndroid which relies
     * heavily on [PostModel]. When `UploadService` gets refactored, we should change this back to using [PageModel].
     */
    suspend fun getLocalDraftPages(site: SiteModel): List<PostModel> =
            coroutineEngine.withDefaultContext(AppLog.T.POSTS, this, "getLocalDraftPages") {
                return@withDefaultContext postSqlUtils.getLocalDrafts(site.id, true)
            }

    /**
     * Get pages that have not been uploaded to the server yet.
     *
     * This returns [PostModel] instead of [PageModel] to accommodate the `UploadService` in WPAndroid which relies
     * heavily on [PostModel]. When `UploadService` gets refactored, we should change this back to using [PageModel].
     */
    suspend fun getPagesWithLocalChanges(site: SiteModel): List<PostModel> =
            coroutineEngine.withDefaultContext(AppLog.T.POSTS, this, "getPagesWithLocalChanges") {
                return@withDefaultContext postSqlUtils.getPostsWithLocalChanges(site.id, true)
            }

    private fun fetchPages(site: SiteModel, loadMore: Boolean) {
        val payload = FetchPostsPayload(site, loadMore, PAGE_TYPES)
        dispatcher.dispatch(PostActionBuilder.newFetchPagesAction(payload))
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onPostChanged(event: OnPostChanged) {
        when (event.causeOfChange) {
            is CauseOfOnPostChanged.FetchPages -> {
                if (event.canLoadMore && fetchingSite != null) {
                    fetchPages(fetchingSite!!, true)
                } else {
                    postLoadContinuations.forEach { it.resume(event) }
                    postLoadContinuations.clear()
                    fetchingSite = null
                }
            }
            is CauseOfOnPostChanged.DeletePost -> {
                deletePostContinuation?.resume(event)
                deletePostContinuation = null
            }
            is CauseOfOnPostChanged.UpdatePost -> {
                updatePostContinuation?.resume(event)
                updatePostContinuation = null
            }
            else -> {
            }
        }
    }

    private fun PostModel.updatePageData(page: PageModel) {
        this.setId(page.pageId)
        this.setTitle(page.title)
        this.setStatus(page.status.toPostStatus().toString())
        this.setParentId(page.parent?.remoteId ?: 0)
        this.setRemotePostId(page.remoteId)
        this.setDateCreated(DateTimeUtils.iso8601FromDate(page.date))
    }
}
