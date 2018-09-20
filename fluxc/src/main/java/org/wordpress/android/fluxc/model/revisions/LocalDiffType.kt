package org.wordpress.android.fluxc.model.revisions

enum class LocalDiffType constructor(private val string: String) {
    TITLE("title"),
    CONTENT("post"),
    UNKNOWN("unknown");

    override fun toString(): String {
        return string
    }

    companion object {
        @JvmStatic
        fun fromString(string: String?): LocalDiffType {
            return when (string) {
                "title" -> TITLE
                "post" -> CONTENT
                else -> {
                    UNKNOWN
                }
            }
        }
    }
}
