package org.wordpress.android.models.pages

import org.wordpress.android.fluxc.model.PostModel

data class PageModel(
    val pageId: Int,
    val title: String,
    var status: PageStatus,
    val parentId: Long,
    val remoteId: Long
) {
    companion object {
        fun fromPost(post: PostModel): PageModel {
            return PageModel(post.id, post.title, PageStatus.fromPost(post), post.parentId, post.remotePostId)
        }
    }

    var parent: PageModel? = null
}
