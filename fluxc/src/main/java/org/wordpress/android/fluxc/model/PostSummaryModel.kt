package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.RawConstraints
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.post.PostStatus

data class PostSummary(val remoteId: Long, val localSiteId: Int, val status: PostStatus) {
    companion object {
        @JvmStatic
        fun fromPostSummaryModel(postSummaryModel: PostSummaryModel): PostSummary {
            // Both remoteId and localSiteId should never be null, so it's worth crashing here to catch the bug
            return PostSummary(
                    postSummaryModel.remoteId!!,
                    postSummaryModel.localSiteId!!,
                    PostStatus.(postSummaryModel.status)
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

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }

    companion object {
        @JvmStatic
        fun newInstance(site: SiteModel, remoteId: Long, postStatus: String?): PostSummaryModel {
            return PostSummaryModel().apply {
                this.localSiteId = site.id
                this.remoteId = remoteId
                this.status = postStatus
            }
        }
    }
}
