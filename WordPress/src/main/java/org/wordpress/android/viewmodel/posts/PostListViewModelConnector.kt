package org.wordpress.android.viewmodel.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostActionHandler
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostModelUploadStatusTracker

class PostListViewModelConnector(
    val site: SiteModel,
    val postListType: PostListType,
    val postActionHandler: PostActionHandler,
    val doesPostHaveUnhandledConflict: (PostModel) -> Boolean,
    val hasAutoSave: (PostModel) -> Boolean,
    val postFetcher: PostFetcher,
    val uploadStatusTracker: PostModelUploadStatusTracker,
    private val getFeaturedImageUrl: (site: SiteModel, featuredImageId: Long) -> String?
) {
    fun getFeaturedImageUrl(featuredImageId: Long): String? {
        return getFeaturedImageUrl.invoke(site, featuredImageId)
    }
}
