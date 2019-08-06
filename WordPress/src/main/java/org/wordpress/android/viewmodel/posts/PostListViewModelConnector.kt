package org.wordpress.android.viewmodel.posts

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.PostActionHandler
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.uploads.UploadStarter

class PostListViewModelConnector(
    val site: SiteModel,
    val postListType: PostListType,
    val postActionHandler: PostActionHandler,
    val getUploadStatus: (PostModel, UploadStarter, SiteModel) -> PostListItemUploadStatus,
    val doesPostHaveUnhandledConflict: (PostModel) -> Boolean,
    val postFetcher: PostFetcher,
    private val getFeaturedImageUrl: (site: SiteModel, featuredImageId: Long) -> String?
) {
    fun getFeaturedImageUrl(featuredImageId: Long): String? {
        return getFeaturedImageUrl.invoke(site, featuredImageId)
    }
}
