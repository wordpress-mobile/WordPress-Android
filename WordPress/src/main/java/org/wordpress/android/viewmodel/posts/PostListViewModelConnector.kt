package org.wordpress.android.viewmodel.posts

import android.text.TextUtils
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.posts.AuthorFilterSelection
import org.wordpress.android.ui.posts.PostActionHandler
import org.wordpress.android.ui.posts.PostListType
import org.wordpress.android.ui.posts.PostListType.SEARCH

class PostListViewModelConnector(
    val site: SiteModel,
    val postListType: PostListType,
    val authorFilter: AuthorFilterSelection,
    val postActionHandler: PostActionHandler,
    val getUploadStatus: (PostModel) -> PostListItemUploadStatus,
    val doesPostHaveUnhandledConflict: (PostModel) -> Boolean,
    val postFetcher: PostFetcher,
    val searchQuery: String?,
    private val getFeaturedImageUrl: (site: SiteModel, featuredImageId: Long, postContent: String) -> String?
) {
    fun getFeaturedImageUrl(featuredImageId: Long, postContent: String): String? {
        return getFeaturedImageUrl.invoke(site, featuredImageId, postContent)
    }

    fun isEmptySearch(): Boolean {
        return postListType == SEARCH && TextUtils.isEmpty(searchQuery)
    }
}
