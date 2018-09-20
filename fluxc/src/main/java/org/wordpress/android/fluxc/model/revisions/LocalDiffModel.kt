package org.wordpress.android.fluxc.model.revisions

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

@Table
class LocalDiffModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var revisionId: Long = 0
    @Column var postId: Long = 0
    @Column var siteId: Long = 0

    @Column var operation: String? = null
    @Column var value: String? = null

    @Column var diffType: String? = null

    override fun getId(): Int {
        return this.id
    }

    override fun setId(id: Int) {
        this.id = id
    }

    companion object {
        @JvmStatic
        fun fromDiffAndLocalRevision(
            diff: Diff,
            diffType: LocalDiffType,
            localRevisionModel: LocalRevisionModel
        ): LocalDiffModel {
            val localDiff = LocalDiffModel()

            localDiff.revisionId = localRevisionModel.revisionId
            localDiff.postId = localRevisionModel.postId
            localDiff.siteId = localRevisionModel.siteId

            localDiff.operation = diff.operation.toString()
            localDiff.value = diff.value

            localDiff.diffType = diffType.toString()
            return localDiff
        }
    }
}
