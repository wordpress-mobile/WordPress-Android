package org.wordpress.android.ui.posts

enum class PostListViewLayoutType(val id: Long) {
    STANDARD(id = 0),
    COMPACT(id = 1);

    companion object {
        @JvmStatic
        val defaultValue = STANDARD

        @JvmStatic
        fun fromId(id: Long): PostListViewLayoutType {
            return values().firstOrNull { it.id == id } ?: defaultValue
        }
    }
}
