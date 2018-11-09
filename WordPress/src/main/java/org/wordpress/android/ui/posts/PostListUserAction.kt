package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel

sealed class PostListUserAction {
    class EditPost(val site: SiteModel, val post: PostModel) : PostListUserAction()
    class NewPost(val site: SiteModel, val isPromo: Boolean = false) : PostListUserAction()
    class PreviewPost(val site: SiteModel, val post: PostModel) : PostListUserAction()
    class RetryUpload(
        val post: PostModel,
        val trackAnalytics: Boolean = PostUtils.isFirstTimePublish(post),
        val publish: Boolean = false,
        val retry: Boolean = true
    ) : PostListUserAction()

    class ViewStats(val site: SiteModel, val post: PostModel) : PostListUserAction()
    class ViewPost(val site: SiteModel, val post: PostModel) : PostListUserAction()
    class ShowGutenbergWarningDialog(val site: SiteModel, val post: PostModel) : PostListUserAction()
}
