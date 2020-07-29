package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.PostImmutableModel
import org.wordpress.android.fluxc.model.SiteModel
import javax.inject.Inject

class PostEditorAnalyticsSessionWrapper @Inject constructor() {
    fun getNewPostEditorAnalyticsSession(
        editor: PostEditorAnalyticsSession.Editor,
        post: PostImmutableModel?,
        site: SiteModel?,
        isNewPost: Boolean
    ): PostEditorAnalyticsSession = PostEditorAnalyticsSession.getNewPostEditorAnalyticsSession(
            editor,
            post, site, isNewPost
    )
}
