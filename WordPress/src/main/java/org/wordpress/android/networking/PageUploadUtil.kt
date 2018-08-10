package org.wordpress.android.networking

import android.content.Context
import kotlinx.coroutines.experimental.CommonPool
import kotlinx.coroutines.experimental.withContext
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.generated.PostActionBuilder
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.page.PageModel
import org.wordpress.android.fluxc.model.post.PostStatus
import org.wordpress.android.fluxc.store.PostStore
import org.wordpress.android.networking.PageUploadUtil.UploadRequestResult.ERROR_EMPTY_PAGE
import org.wordpress.android.networking.PageUploadUtil.UploadRequestResult.ERROR_NON_EXISTING_PAGE
import org.wordpress.android.networking.PageUploadUtil.UploadRequestResult.ERROR_NO_NETWORK
import org.wordpress.android.networking.PageUploadUtil.UploadRequestResult.SUCCESS
import org.wordpress.android.ui.posts.PostUtils
import org.wordpress.android.ui.uploads.UploadService
import org.wordpress.android.util.NetworkUtils
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PageUploadUtil @Inject constructor(
    private val postStore: PostStore,
    private val dispatcher: Dispatcher,
    private val context: Context
) {
    suspend fun uploadPage(page: PageModel): UploadRequestResult = withContext(CommonPool) {
        val post = postStore.getPostByRemotePostId(page.remoteId, page.site)
        if (post != null) {
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
        } else {
            return@withContext ERROR_NON_EXISTING_PAGE
        }
    }

    fun isPageUploading(pageId: Long, site: SiteModel): Boolean {
        val post = postStore.getPostByRemotePostId(pageId, site)
        return UploadService.isPostUploadingOrQueued(post)
    }

    enum class UploadRequestResult {
        SUCCESS,
        ERROR_NO_NETWORK,
        ERROR_EMPTY_PAGE,
        ERROR_NON_EXISTING_PAGE
    }
}

fun PostModel.updatePageData(page: PageModel) {
    this.id = page.pageId
    this.title = page.title
    this.status = page.status.toPostStatus().toString()
    this.parentId = page.parent?.remoteId ?: 0
    this.remotePostId = page.remoteId
}
