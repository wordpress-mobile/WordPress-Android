package org.wordpress.android.ui.navmenus.models

/**
 * Represents a theme menu location where menus can be assigned.
 * Maps to the WP REST API /wp/v2/menu-locations endpoint.
 */
data class NavMenuLocationModel(
    val localSiteId: Int = 0,
    // The location slug (e.g., "primary", "footer", "social")
    val name: String = "",
    // Human-readable description of the location
    val description: String = "",
    // The ID of the menu assigned to this location (0 if no menu assigned)
    val menuId: Long = 0
)
