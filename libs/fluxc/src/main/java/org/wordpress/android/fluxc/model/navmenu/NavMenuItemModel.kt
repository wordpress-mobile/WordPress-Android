package org.wordpress.android.fluxc.model.navmenu

import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table

/**
 * Represents an item within a WordPress navigation menu.
 * Maps to the WP REST API /wp/v2/menu-items endpoint.
 */
@Table
class NavMenuItemModel(@PrimaryKey @Column private var id: Int = 0) : Identifiable {
    @Column var localSiteId: Int = 0
    @Column var remoteItemId: Long = 0
    @Column var menuId: Long = 0
    @Column var title: String = ""
    @Column var url: String = ""
    // Type of menu item: "custom", "post_type", "taxonomy", "post_type_archive"
    @Column var type: String = ""
    // Specific object type: "post", "page", "category", "tag", or custom taxonomy/post type
    @Column var objectType: String = ""
    // ID of the linked object (post, page, category, etc.)
    @Column var objectId: Long = 0
    // Parent menu item ID for hierarchical menus
    @Column var parentId: Long = 0
    // Order position within the menu
    @Column var menuOrder: Int = 0
    // Link target: "_blank", "_self", etc.
    @Column var target: String = ""
    // CSS classes (JSON array stored as string)
    @Column var classes: String = ""
    // Item description
    @Column var description: String = ""
    // Attribute title (tooltip)
    @Column var attrTitle: String = ""

    override fun getId(): Int = id

    override fun setId(id: Int) {
        this.id = id
    }

    companion object {
        const val TYPE_CUSTOM = "custom"
        const val TYPE_POST_TYPE = "post_type"
        const val TYPE_TAXONOMY = "taxonomy"
        const val TYPE_POST_TYPE_ARCHIVE = "post_type_archive"

        const val OBJECT_TYPE_POST = "post"
        const val OBJECT_TYPE_PAGE = "page"
        const val OBJECT_TYPE_CATEGORY = "category"
        const val OBJECT_TYPE_TAG = "post_tag"
    }
}
