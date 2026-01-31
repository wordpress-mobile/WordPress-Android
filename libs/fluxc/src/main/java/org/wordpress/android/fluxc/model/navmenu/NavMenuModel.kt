package org.wordpress.android.fluxc.model.navmenu

/**
 * Represents a WordPress navigation menu.
 * Maps to the WP REST API /wp/v2/menus endpoint.
 */
class NavMenuModel {
    var localSiteId: Int = 0
    var remoteMenuId: Long = 0
    var name: String = ""
    var slug: String = ""
    var description: String = ""
    // JSON array of location slugs where this menu is assigned
    var locations: String = ""
    // Whether to automatically add new top-level pages to this menu
    var autoAdd: Boolean = false
}
