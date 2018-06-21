package org.wordpress.android.ui.pages

sealed class PageItem(open val id: Long, val viewType: Int) {
    data class Page(override val id: Long, val title: String, val icon: String?, val indent: Int = 0) :
            PageItem(id, PAGE)

    data class Divider(override val id: Long, val title: String) : PageItem(id, DIVIDER)

    companion object {
        const val PAGE = 1
        const val DIVIDER = 2
    }
}
