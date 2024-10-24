package org.wordpress.android.fluxc.model.revisions

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.PostModel
import org.wordpress.android.fluxc.model.SiteModel

@Table
class LocalRevisionModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var revisionId: Long = 0
    @Column var postId: Long = 0
    @Column var siteId: Long = 0

    @Column var diffFromVersion: Long = 0

    @Column var totalAdditions: Int = 0
    @Column var totalDeletions: Int = 0

    @Column var postContent: String? = null
    @Column var postExcerpt: String? = null
    @Column var postTitle: String? = null

    @Column var postDateGmt: String? = null
    @Column var postModifiedGmt: String? = null
    @Column var postAuthorId: String? = null

    override fun getId(): Int {
        return this.id
    }

    override fun setId(id: Int) {
        this.id = id
    }

    companion object {
        @JvmStatic
        fun fromRevisionModel(revisionModel: RevisionModel, site: SiteModel, post: PostModel): LocalRevisionModel {
            val localRevisionModel = LocalRevisionModel()
            localRevisionModel.revisionId = revisionModel.revisionId
            localRevisionModel.postId = post.remotePostId
            localRevisionModel.siteId = site.siteId

            localRevisionModel.diffFromVersion = revisionModel.diffFromVersion

            localRevisionModel.totalAdditions = revisionModel.totalAdditions
            localRevisionModel.totalDeletions = revisionModel.totalDeletions

            localRevisionModel.postContent = revisionModel.postContent
            localRevisionModel.postExcerpt = revisionModel.postExcerpt
            localRevisionModel.postTitle = revisionModel.postTitle

            localRevisionModel.postDateGmt = revisionModel.postDateGmt
            localRevisionModel.postModifiedGmt = revisionModel.postModifiedGmt
            localRevisionModel.postAuthorId = revisionModel.postAuthorId

            return localRevisionModel
        }
    }
}
