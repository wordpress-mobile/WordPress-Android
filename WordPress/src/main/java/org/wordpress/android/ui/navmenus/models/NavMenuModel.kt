package org.wordpress.android.ui.navmenus.models

/**
 * Represents a WordPress navigation menu.
 * Maps to the WP REST API /wp/v2/menus endpoint.
 */
data class NavMenuModel(
    val localSiteId: Int = 0,
    val remoteMenuId: Long = 0,
    val name: String = "",
    val slug: String = "",
    val description: String = "",
    // JSON array of location slugs where this menu is assigned
    val locations: String = "",
    // Whether to automatically add new top-level pages to this menu
    val autoAdd: Boolean = false
)
