package org.wordpress.android.models.pages

import org.wordpress.android.fluxc.model.PostModel

data class PageModel(val pageId: Int, val title: String, var status: PageStatus, var parentId: Long) {
    constructor(post: PostModel) : this(post.id, post.title, PageStatus.fromPost(post), post.parentId)
}
