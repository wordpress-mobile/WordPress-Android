package org.wordpress.android.fluxc.network.rest.wpcom.revisions

import com.google.gson.annotations.SerializedName

class RevisionsResponse(
    val diffs: List<DiffDetails>,
    val revisions: Map<String, Revision>
) {
    inner class DiffDetails(
        val from: Int,
        val to: Int,
        val diff: Diff
    )

    inner class Diff(
        val post_title: List<DiffContent>,
        val post_content: List<DiffContent>,
        val totals: Totals
    ) {
        inner class DiffContent(
            @SerializedName("op")
            val operation: DiffAction,
            val value: String
        )

        inner class Totals(
            val del: Int,
            val add: Int
        )
    }

    enum class DiffAction {
        @SerializedName("copy")
        COPY,
        @SerializedName("add")
        ADD,
        @SerializedName("del")
        DELETE
    }

    inner class Revision(
        val post_date_gmt: String,
        val post_modified_gmt: String,
        val post_author: String,
        val id: Int,
        val post_content: String,
        val post_excerpt: String,
        val post_title: String
    )
}
