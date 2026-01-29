package org.wordpress.android.fluxc.network.rest.wpapi.navmenu

import com.google.gson.annotations.SerializedName

/**
 * Response model for WP REST API /wp/v2/menus endpoint
 */
data class NavMenuApiResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("name") val name: String,
    @SerializedName("slug") val slug: String,
    @SerializedName("description") val description: String?,
    @SerializedName("locations") val locations: List<String>?,
    @SerializedName("auto_add") val autoAdd: Boolean?
)

/**
 * Response model for WP REST API /wp/v2/menu-items endpoint
 */
data class NavMenuItemApiResponse(
    @SerializedName("id") val id: Long,
    @SerializedName("menus") val menuId: Long,
    @SerializedName("title") val title: RenderedContent?,
    @SerializedName("url") val url: String?,
    @SerializedName("type") val type: String?,
    @SerializedName("object") val objectType: String?,
    @SerializedName("object_id") val objectId: Long?,
    @SerializedName("parent") val parentId: Long?,
    @SerializedName("menu_order") val menuOrder: Int?,
    @SerializedName("target") val target: String?,
    @SerializedName("classes") val classes: List<String>?,
    @SerializedName("description") val description: String?,
    @SerializedName("attr_title") val attrTitle: String?
)

/**
 * Wrapper for rendered content fields (title, content, etc.)
 */
data class RenderedContent(
    @SerializedName("rendered") val rendered: String?
)

/**
 * Response model for WP REST API /wp/v2/menu-locations endpoint
 */
data class NavMenuLocationApiResponse(
    @SerializedName("name") val name: String,
    @SerializedName("description") val description: String?,
    @SerializedName("menu") val menuId: Long?
)

/**
 * Response model for delete operations
 */
data class NavMenuDeleteResponse(
    @SerializedName("deleted") val deleted: Boolean,
    @SerializedName("previous") val previous: NavMenuApiResponse?
)

/**
 * Response model for delete menu item operations
 */
data class NavMenuItemDeleteResponse(
    @SerializedName("deleted") val deleted: Boolean,
    @SerializedName("previous") val previous: NavMenuItemApiResponse?
)
