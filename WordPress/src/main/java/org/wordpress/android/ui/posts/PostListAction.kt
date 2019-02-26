package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel

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
