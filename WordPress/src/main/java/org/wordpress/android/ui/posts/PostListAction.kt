package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.push.NativeNotificationsUtils
import org.wordpress.android.ui.ActivityLauncher
import org.wordpress.android.ui.uploads.UploadService

sealed class PostListAction {
    class EditPost(val site: SiteModel, val post: PostModel) : PostListAction()
    class NewPost(val site: SiteModel, val isPromo: Boolean = false) : PostListAction()
    class PreviewPost(val site: SiteModel, val post: PostModel) : PostListAction()
    class RetryUpload(
        val post: PostModel,
        val trackAnalytics: Boolean = PostUtils.isFirstTimePublish(post),
        val publish: Boolean = false,
        val retry: Boolean = true
    ) : PostListAction()

    class ViewStats(val site: SiteModel, val post: PostModel) : PostListAction()
    class ViewPost(val site: SiteModel, val post: PostModel) : PostListAction()
    class DismissPendingNotification(val pushId: Int) : PostListAction()
}

fun handlePostListAction(activity: androidx.fragment.app.FragmentActivity, action: PostListAction) {
    when (action) {
        is PostListAction.EditPost -> {
            ActivityLauncher.editPostOrPageForResult(activity, action.site, action.post)
        }
        is PostListAction.NewPost -> {
            ActivityLauncher.addNewPostForResult(activity, action.site, action.isPromo)
        }
        is PostListAction.PreviewPost -> {
            ActivityLauncher.viewPostPreviewForResult(activity, action.site, action.post)
        }
        is PostListAction.RetryUpload -> {
            // restart the UploadService with retry parameters
            val intent = UploadService.getUploadPostServiceIntent(
                    activity,
                    action.post,
                    action.trackAnalytics,
                    action.publish,
                    action.retry
            )
            activity.startService(intent)
        }
        is PostListAction.ViewStats -> {
            ActivityLauncher.viewStatsSinglePostDetails(activity, action.site, action.post)
        }
        is PostListAction.ViewPost -> {
            ActivityLauncher.browsePostOrPage(activity, action.site, action.post)
        }
        is PostListAction.DismissPendingNotification -> {
            NativeNotificationsUtils.dismissNotification(action.pushId, activity)
        }
    }
}
