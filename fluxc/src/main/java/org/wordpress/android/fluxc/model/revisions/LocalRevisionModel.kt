package org.wordpress.android.fluxc.model.revisions

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

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
}