package org.wordpress.android.ui.posts

enum class AuthorFilterSelection(val id: Int) {
    ME(id = 0), EVERYONE(id = 1);

    companion object {
        @JvmStatic
        fun fromId(id: Int, default: AuthorFilterSelection): AuthorFilterSelection {
            return values().firstOrNull { it.id == id } ?: default
        }
    }
}
