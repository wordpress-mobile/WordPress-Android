package org.wordpress.android.fluxc.model

class RevisionModel(
    var id: Long,

    var diffFromVersion: Long,

    var totalAdditions: Int,
    var totalDeletions: Int,

    var postContent: String,
    var postExcerpt: String,
    var postTitle: String,

    var postDateGmt: String,
    var postModifiedGmt: String,
    var postAuthorId: String,

    val titleDiffs: List<Diff>,
    val contentDifs: List<Diff>
) {
    class Diff(
        val operation: DiffOperations,
        val value: String
    )

    enum class DiffOperations {
        COPY,
        ADD,
        DELETE,
        UNKNOWN;

        companion object {
            @JvmStatic
            fun fromResponseString(string: String): DiffOperations {
                return when (string) {
                    "copy" -> COPY
                    "add" -> ADD
                    "del" -> DELETE
                    else -> { // Note the block
                        UNKNOWN
                    }
                }
            }
        }
    }
}
