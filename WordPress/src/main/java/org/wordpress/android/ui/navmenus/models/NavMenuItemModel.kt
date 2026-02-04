package org.wordpress.android.ui.navmenus.models

/**
 * Represents an item within a WordPress navigation menu.
 * Maps to the WP REST API /wp/v2/menu-items endpoint.
 */
data class NavMenuItemModel(
    val localSiteId: Int = 0,
    val remoteItemId: Long = 0,
    val menuId: Long = 0,
    val title: String = "",
    val url: String = "",
    // Type of menu item: "custom", "post_type", "taxonomy", "post_type_archive"
    val type: String = "",
    // Specific object type: "post", "page", "category", "tag", or custom taxonomy/post type
    val objectType: String = "",
    // ID of the linked object (post, page, category, etc.)
    val objectId: Long = 0,
    // Parent menu item ID for hierarchical menus
    val parentId: Long = 0,
    // Order position within the menu
    val menuOrder: Int = 0,
    // Link target: "_blank", "_self", etc.
    val target: String = "",
    // CSS classes (JSON array stored as string)
    val classes: String = "",
    // Item description
    val description: String = "",
    // Attribute title (tooltip)
    val attrTitle: String = ""
) {
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
