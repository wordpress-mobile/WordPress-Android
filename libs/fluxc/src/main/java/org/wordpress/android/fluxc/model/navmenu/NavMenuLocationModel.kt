package org.wordpress.android.fluxc.model.navmenu

/**
 * Represents a theme menu location where menus can be assigned.
 * Maps to the WP REST API /wp/v2/menu-locations endpoint.
 */
class NavMenuLocationModel {
    var localSiteId: Int = 0
    // The location slug (e.g., "primary", "footer", "social")
    var name: String = ""
    // Human-readable description of the location
    var description: String = ""
    // The ID of the menu assigned to this location (0 if no menu assigned)
    var menuId: Long = 0
}
