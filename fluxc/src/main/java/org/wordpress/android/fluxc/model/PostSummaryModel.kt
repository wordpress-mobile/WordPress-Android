package org.wordpress.android.fluxc.model

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.post.PostStatus

data class PostSummary(val remoteId: Long, val localSiteId: Int, val status: PostStatus)

@Table
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
