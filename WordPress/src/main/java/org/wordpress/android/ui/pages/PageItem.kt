package org.wordpress.android.ui.pages

import org.wordpress.android.R
import org.wordpress.android.ui.pages.PageItem.Type.DIVIDER
import org.wordpress.android.ui.pages.PageItem.Type.EMPTY
import org.wordpress.android.ui.pages.PageItem.Type.PAGE

sealed class PageItem(open val id: Long? = null, val type: Type) {
    data class Page(
        override val id: Long,
        val title: String,
        val icon: String?,
        val indent: Int = 0,
        val enabledActions: Set<Action> = setOf()
    ) : PageItem(id, PAGE)

    data class Divider(override val id: Long, val title: String) : PageItem(id, DIVIDER)

    data class Empty(val textResource: Int? = null): PageItem(type = EMPTY)

    enum class Type(val viewType: Int) {
        PAGE(1), DIVIDER(2), EMPTY(3)
    }

    enum class Action(val itemId: Int) {
        VIEW_PAGE(R.id.view_page),
        SET_PARENT(R.id.set_parent),
        PUBLISH_NOW(R.id.publish_now),
        MOVE_TO_DRAFT(R.id.move_to_draft),
        MOVE_TO_TRASH(R.id.move_to_trash);

        companion object {
            fun fromItemId(itemId: Int): Action {
                return values().firstOrNull { it.itemId == itemId }
                        ?: throw IllegalArgumentException("Unexpected item ID in context menu")
            }
        }
    }
}
