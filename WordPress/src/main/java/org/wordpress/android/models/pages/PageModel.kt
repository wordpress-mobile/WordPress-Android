package org.wordpress.android.models.pages

import org.wordpress.android.fluxc.model.PostModel

data class PageModel(val pageId: Int, val title: String, var status: PageStatus, var parentId: Long) {
    companion object {
        fun fromPost(post: PostModel?): PageModel? {
            return post?.let { PageModel(post.id, post.title, PageStatus.fromPost(post), post.parentId) }
        }
    }
}
