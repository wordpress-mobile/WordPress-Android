package org.wordpress.android.fluxc.model

import org.wordpress.android.fluxc.network.rest.wpcom.revisions.RevisionsResponse.Diff.DiffContent

class RevisionModel(
    var id: Long,
    var postId: Long,

    var diffFromVersion: Long,

    var totalAdditions: Int,
    var totalDeletions: Int,

    var postContent: String,
    var postExcerpt: String,
    var postTitle: String,

    var postDateGmt: String,
    var postModifiedGmt: String,
    var postAuthorId: String,

    val titleDiffs: List<DiffContent>,
    val contentDifs: List<DiffContent>
)
