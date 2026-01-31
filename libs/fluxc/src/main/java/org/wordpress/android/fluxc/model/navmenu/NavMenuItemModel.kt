package org.wordpress.android.fluxc.model.navmenu

/**
 * Represents an item within a WordPress navigation menu.
 * Maps to the WP REST API /wp/v2/menu-items endpoint.
 */
class NavMenuItemModel {
    var localSiteId: Int = 0
    var remoteItemId: Long = 0
    var menuId: Long = 0
    var title: String = ""
    var url: String = ""
    // Type of menu item: "custom", "post_type", "taxonomy", "post_type_archive"
    var type: String = ""
    // Specific object type: "post", "page", "category", "tag", or custom taxonomy/post type
    var objectType: String = ""
    // ID of the linked object (post, page, category, etc.)
    var objectId: Long = 0
    // Parent menu item ID for hierarchical menus
    var parentId: Long = 0
    // Order position within the menu
    var menuOrder: Int = 0
    // Link target: "_blank", "_self", etc.
    var target: String = ""
    // CSS classes (JSON array stored as string)
    var classes: String = ""
    // Item description
    var description: String = ""
    // Attribute title (tooltip)
    var attrTitle: String = ""

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
