package org.wordpress.android.ui.posts

enum class ViewLayoutType(val id: Long) {
    STANDARD(id = 0),
    COMPACT(id = 1);

    companion object {
        @JvmStatic
        val defaultValue = STANDARD

        @JvmStatic
        fun fromId(id: Long): ViewLayoutType {
            return values().firstOrNull { it.id == id } ?: defaultValue
        }
    }
}