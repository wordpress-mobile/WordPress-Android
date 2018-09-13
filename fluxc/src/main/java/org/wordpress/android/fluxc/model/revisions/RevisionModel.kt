package org.wordpress.android.fluxc.model.revisions

class RevisionModel(
        var id: Long,

        var diffFromVersion: Long,

        var totalAdditions: Int,
        var totalDeletions: Int,

        var postContent: String?,
        var postExcerpt: String?,
        var postTitle: String?,

        var postDateGmt: String?,
        var postModifiedGmt: String?,
        var postAuthorId: String?,

        val titleDiffs: List<Diff>,
        val contentDiffs: List<Diff>
)
