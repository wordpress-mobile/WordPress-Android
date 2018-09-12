package org.wordpress.android.fluxc.model.page

import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.util.DateTimeUtils
import java.util.Date

data class PageModel(
    val site: SiteModel,
    val pageId: Int,
    val title: String,
    var status: PageStatus,
    var date: Date,
    var hasLocalChanges: Boolean,
    val remoteId: Long,
    var parent: PageModel?
) {
    constructor(post: PostModel, site: SiteModel, parent: PageModel? = null) : this(site, post.id, post.title,
            PageStatus.fromPost(post), Date(DateTimeUtils.timestampFromIso8601Millis(post.dateCreated)),
            post.isLocallyChanged, post.remotePostId, parent)
}
