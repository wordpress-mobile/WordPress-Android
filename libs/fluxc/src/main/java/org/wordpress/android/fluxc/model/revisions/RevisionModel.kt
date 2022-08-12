package org.wordpress.android.fluxc.model.revisions

import java.util.ArrayList
import java.util.Arrays

@Suppress("LongParameterList")
class RevisionModel(
    var revisionId: Long,

    var diffFromVersion: Long,

    var totalAdditions: Int,
    var totalDeletions: Int,

    var postContent: String?,
    var postExcerpt: String?,
    var postTitle: String?,

    var postDateGmt: String?,
    var postModifiedGmt: String?,
    var postAuthorId: String?,

    val titleDiffs: ArrayList<Diff>,
    val contentDiffs: ArrayList<Diff>
) {
    companion object {
        @JvmStatic
        fun fromLocalRevisionAndDiffs(
            localRevision: LocalRevisionModel,
            localDiffs: List<LocalDiffModel>
        ): RevisionModel {
            val titleDiffs = ArrayList<Diff>()
            val contentDiffs = ArrayList<Diff>()

            for (localDiff in localDiffs) {
                if (LocalDiffType.TITLE === LocalDiffType.fromString(localDiff.diffType)) {
                    titleDiffs.add(Diff(DiffOperations.fromString(localDiff.operation), localDiff.value))
                } else if (LocalDiffType.CONTENT === LocalDiffType.fromString(localDiff.diffType)) {
                    contentDiffs.add(Diff(DiffOperations.fromString(localDiff.operation), localDiff.value))
                }
            }

            return RevisionModel(
                    localRevision.revisionId,
                    localRevision.diffFromVersion,
                    localRevision.totalAdditions,
                    localRevision.totalDeletions,
                    localRevision.postContent,
                    localRevision.postExcerpt,
                    localRevision.postTitle,
                    localRevision.postDateGmt,
                    localRevision.postModifiedGmt,
                    localRevision.postAuthorId,
                    titleDiffs,
                    contentDiffs
            )
        }
    }

    @Suppress("ComplexMethod")
    override fun equals(other: Any?): Boolean {
        if (this === other) {
            return true
        }

        if (other == null || other !is RevisionModel) {
            return false
        }

        return revisionId == other.revisionId && diffFromVersion == other.diffFromVersion &&
                totalAdditions == other.totalAdditions && totalDeletions == other.totalDeletions &&
                postContent == other.postContent && postExcerpt == other.postExcerpt &&
                postTitle == other.postTitle && postAuthorId == other.postAuthorId &&
                postDateGmt == other.postDateGmt && postModifiedGmt == other.postModifiedGmt &&
                titleDiffs.toArray() contentDeepEquals other.titleDiffs.toArray() &&
                contentDiffs.toArray() contentDeepEquals other.contentDiffs.toArray()
    }

    override fun hashCode(): Int {
        var result = revisionId.hashCode()
        result = 31 * result + diffFromVersion.hashCode()
        result = 31 * result + totalAdditions
        result = 31 * result + totalDeletions
        result = 31 * result + (postContent?.hashCode() ?: 0)
        result = 31 * result + (postExcerpt?.hashCode() ?: 0)
        result = 31 * result + (postTitle?.hashCode() ?: 0)
        result = 31 * result + (postDateGmt?.hashCode() ?: 0)
        result = 31 * result + (postModifiedGmt?.hashCode() ?: 0)
        result = 31 * result + (postAuthorId?.hashCode() ?: 0)
        result = 31 * result + (Arrays.hashCode(contentDiffs.toArray()))
        result = 31 * result + (Arrays.hashCode(titleDiffs.toArray()))
        return result
    }
}
