package org.wordpress.android.models.pages

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.post.PostStatus

data class PageModel(val pageId: Int, val title: String, var status: PostStatus, var parentId: Long) {
    companion object {
        fun fromPost(post: PostModel?): PageModel? {
            return post?.let { PageModel(post.id, post.title, PostStatus.fromPost(post), post.parentId) }
        }
    }
}
