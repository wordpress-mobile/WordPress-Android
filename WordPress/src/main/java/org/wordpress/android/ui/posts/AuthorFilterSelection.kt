package org.wordpress.android.ui.posts

enum class AuthorFilterSelection(val id: Int) {
    ME(id = 0), EVERYONE(id = 1);

    companion object {
        @JvmStatic
        val defaultValue = EVERYONE

        @JvmStatic
        fun fromId(id: Int): AuthorFilterSelection {
            return values().firstOrNull { it.id == id } ?: defaultValue
        }
    }
}
