package org.wordpress.android.fluxc.model.page

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

data class PageModel(
    val post: PostModel,
    val site: SiteModel,
    val pageId: Int,
    val title: String,
    val status: PageStatus,
    val date: Date,
    val hasLocalChanges: Boolean,
    val remoteId: Long,
    val parent: PageModel?,
    val featuredImageId: Long
) {
    constructor(post: PostModel, site: SiteModel, parent: PageModel? = null) : this(post, site, post.id, post.title,
            PageStatus.fromPost(post), Date(DateTimeUtils.timestampFromIso8601Millis(post.dateCreated)),
            post.isLocalDraft || post.isLocallyChanged, post.remotePostId, parent, post.featuredImageId)
}
