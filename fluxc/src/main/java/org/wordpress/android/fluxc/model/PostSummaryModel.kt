package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.post.PostStatus

/**
 * When we fetch the post list, we don't fetch the whole model and only keep the ids of posts in ListStore's
 * ListItemModelTable. However, some fields such as `status` and `dateCreated` are necessary to be able to section the
 * list properly.
 *
 * `PostSummaryModel` is a pattern we can utilize in situations like this. It works as a look up table where records
 * in its table are updated each time posts are fetched. This way the information necessary for a list to work is
 * always available, but we still don't need to fetch & update the whole model on each fetch.
 *
 * // TODO: We can add a link to the wiki for ListStore when that's available.
 * See `ListStore` components for more details.
 */

/**
 * Immutable version of PostSummaryModel that should be used by the clients.
 */
data class PostSummary(val remoteId: Long, val localSiteId: Int, val status: PostStatus) {
    companion object {
        @JvmStatic
        fun fromPostSummaryModel(postSummaryModel: PostSummaryModel): PostSummary {
            // Both remoteId and localSiteId should never be null, so it's worth crashing here to catch the bug
            return PostSummary(
                    postSummaryModel.remoteId!!,
                    postSummaryModel.localSiteId!!,
                    PostStatus.fromPostSummary(postSummaryModel)
            )
        }
    }
}

@Table
@RawConstraints(
        "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE",
        "UNIQUE(REMOTE_ID) ON CONFLICT REPLACE"
)
class PostSummaryModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var remoteId: Long? = null
    @Column var localSiteId: Int? = null
    @Column var status: String? = null
    @Column var dateCreated: String? = null // ISO 8601-formatted date in UTC, e.g. 1955-11-05T14:15:00Z

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }

    constructor(site: SiteModel, remoteId: Long, postStatus: String?, dateCreated: String?) : this() {
        this.localSiteId = site.id
        this.remoteId = remoteId
        this.status = postStatus
        this.dateCreated = dateCreated
    }
}
