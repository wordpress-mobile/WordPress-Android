package org.wordpress.android.fluxc.network.rest.wpcom.revisions

@Suppress("ConstructorParameterNaming")
class RevisionsResponse(
    val diffs: List<DiffResponse>,
    val revisions: Map<String, RevisionResponse>
) {
    inner class DiffResponse(
        val from: Int,
        val to: Int,
        val diff: DiffResponseContent
    )

    inner class DiffResponseContent(
        val post_title: List<DiffResponsePart>,
        val post_content: List<DiffResponsePart>,
        val totals: DiffResponseTotals
    )

    inner class DiffResponsePart(
        val op: String,
        val value: String
    )

    inner class DiffResponseTotals(
        val del: Int,
        val add: Int
    )

    @Suppress("LongParameterList")
    inner class RevisionResponse(
        val post_date_gmt: String,
        val post_modified_gmt: String,
        val post_author: String,
        val id: Int,
        val post_content: String,
        val post_excerpt: String,
        val post_title: String
    )
}
