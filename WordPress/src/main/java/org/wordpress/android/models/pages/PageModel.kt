package org.wordpress.android.models.pages

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel

data class PageModel(
    val site: SiteModel,
    val pageId: Int,
    val title: String,
    var status: PageStatus,
    var hasLocalChanges: Boolean,
    val parentId: Long,
    val remoteId: Long
) {
    var parent: PageModel? = null

    constructor(post: PostModel, site: SiteModel) : this(site, post.id, post.title, PageStatus.fromPost(post),
            post.isLocallyChanged, post.parentId, post.remotePostId)
}
