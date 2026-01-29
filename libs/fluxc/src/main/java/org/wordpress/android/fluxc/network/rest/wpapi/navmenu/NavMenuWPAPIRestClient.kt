package org.wordpress.android.fluxc.network.rest.wpapi.navmenu

import com.android.volley.RequestQueue
import com.google.gson.reflect.TypeToken
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuItemModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuLocationModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuModel
import org.wordpress.android.fluxc.module.OkHttpClientQualifiers
import org.wordpress.android.fluxc.network.UserAgent
import org.wordpress.android.fluxc.network.rest.wpapi.BaseWPAPIRestClient
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIGsonRequestBuilder
import org.wordpress.android.fluxc.network.rest.wpapi.WPAPIResponse
import org.wordpress.android.fluxc.utils.extensions.getPasswordProcessed
import org.wordpress.android.fluxc.utils.extensions.getUserNameProcessed
import java.util.Base64
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class NavMenuWPAPIRestClient @Inject constructor(
    private val wpapiGsonRequestBuilder: WPAPIGsonRequestBuilder,
    dispatcher: Dispatcher,
    @Named(OkHttpClientQualifiers.CUSTOM_SSL) requestQueue: RequestQueue,
    userAgent: UserAgent
) : BaseWPAPIRestClient(dispatcher, requestQueue, userAgent) {

    companion object {
        private const val MENUS_ENDPOINT = "wp/v2/menus"
        private const val MENU_ITEMS_ENDPOINT = "wp/v2/menu-items"
        private const val MENU_LOCATIONS_ENDPOINT = "wp/v2/menu-locations"
    }

    // ========== Menu Operations ==========

    suspend fun fetchMenus(site: SiteModel): NavMenusResult {
        val url = buildApiUrl(site, MENUS_ENDPOINT)
        val listType = object : TypeToken<List<NavMenuApiResponse>>() {}.type

        val response = wpapiGsonRequestBuilder.syncGetRequest<List<NavMenuApiResponse>>(
            restClient = this,
            url = url,
            type = listType,
            params = mapOf("per_page" to "100")
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                val menus = response.data?.map { apiMenu ->
                    apiMenu.toNavMenuModel(site.id)
                } ?: emptyList()
                NavMenusResult.Success(menus)
            }
            is WPAPIResponse.Error -> {
                NavMenusResult.Error(response.error.message ?: "Unknown error")
            }
        }
    }

    suspend fun createMenu(site: SiteModel, menu: NavMenuModel): NavMenuResult {
        val url = buildApiUrl(site, MENUS_ENDPOINT)
        val body = buildMenuBody(menu)

        val response = wpapiGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            body = body,
            clazz = NavMenuApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    NavMenuResult.Success(it.toNavMenuModel(site.id))
                } ?: NavMenuResult.Error("Empty response")
            }
            is WPAPIResponse.Error -> {
                NavMenuResult.Error(response.error.message ?: "Unknown error")
            }
        }
    }

    suspend fun updateMenu(site: SiteModel, menu: NavMenuModel): NavMenuResult {
        val url = buildApiUrl(site, "$MENUS_ENDPOINT/${menu.remoteMenuId}")
        val body = buildMenuBody(menu)

        val response = wpapiGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            body = body,
            clazz = NavMenuApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    NavMenuResult.Success(it.toNavMenuModel(site.id))
                } ?: NavMenuResult.Error("Empty response")
            }
            is WPAPIResponse.Error -> {
                NavMenuResult.Error(response.error.message ?: "Unknown error")
            }
        }
    }

    suspend fun deleteMenu(site: SiteModel, menuId: Long): NavMenuDeleteResult {
        val url = buildApiUrl(site, "$MENUS_ENDPOINT/$menuId")
        val body = mapOf("force" to true)

        val response = wpapiGsonRequestBuilder.syncDeleteRequest(
            restClient = this,
            url = url,
            body = body,
            clazz = NavMenuDeleteResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                if (response.data?.deleted == true) {
                    NavMenuDeleteResult.Success
                } else {
                    NavMenuDeleteResult.Error("Menu was not deleted")
                }
            }
            is WPAPIResponse.Error -> {
                NavMenuDeleteResult.Error(response.error.message ?: "Unknown error")
            }
        }
    }

    // ========== Menu Item Operations ==========

    suspend fun fetchMenuItems(site: SiteModel, menuId: Long): NavMenuItemsResult {
        val url = buildApiUrl(site, MENU_ITEMS_ENDPOINT)
        val listType = object : TypeToken<List<NavMenuItemApiResponse>>() {}.type

        val response = wpapiGsonRequestBuilder.syncGetRequest<List<NavMenuItemApiResponse>>(
            restClient = this,
            url = url,
            type = listType,
            params = mapOf(
                "menus" to menuId.toString(),
                "per_page" to "100"
            )
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                val items = response.data?.map { apiItem ->
                    apiItem.toNavMenuItemModel(site.id)
                } ?: emptyList()
                NavMenuItemsResult.Success(items)
            }
            is WPAPIResponse.Error -> {
                NavMenuItemsResult.Error(response.error.message ?: "Unknown error")
            }
        }
    }

    suspend fun createMenuItem(site: SiteModel, item: NavMenuItemModel): NavMenuItemResult {
        val url = buildApiUrl(site, MENU_ITEMS_ENDPOINT)
        val body = buildMenuItemBody(item)

        val response = wpapiGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            body = body,
            clazz = NavMenuItemApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    NavMenuItemResult.Success(it.toNavMenuItemModel(site.id))
                } ?: NavMenuItemResult.Error("Empty response")
            }
            is WPAPIResponse.Error -> {
                NavMenuItemResult.Error(response.error.message ?: "Unknown error")
            }
        }
    }

    suspend fun updateMenuItem(site: SiteModel, item: NavMenuItemModel): NavMenuItemResult {
        val url = buildApiUrl(site, "$MENU_ITEMS_ENDPOINT/${item.remoteItemId}")
        val body = buildMenuItemBody(item)

        val response = wpapiGsonRequestBuilder.syncPostRequest(
            restClient = this,
            url = url,
            body = body,
            clazz = NavMenuItemApiResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                response.data?.let {
                    NavMenuItemResult.Success(it.toNavMenuItemModel(site.id))
                } ?: NavMenuItemResult.Error("Empty response")
            }
            is WPAPIResponse.Error -> {
                NavMenuItemResult.Error(response.error.message ?: "Unknown error")
            }
        }
    }

    suspend fun deleteMenuItem(site: SiteModel, itemId: Long): NavMenuItemDeleteResult {
        val url = buildApiUrl(site, "$MENU_ITEMS_ENDPOINT/$itemId")
        val body = mapOf("force" to true)

        val response = wpapiGsonRequestBuilder.syncDeleteRequest(
            restClient = this,
            url = url,
            body = body,
            clazz = NavMenuItemDeleteResponse::class.java
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                if (response.data?.deleted == true) {
                    NavMenuItemDeleteResult.Success
                } else {
                    NavMenuItemDeleteResult.Error("Menu item was not deleted")
                }
            }
            is WPAPIResponse.Error -> {
                NavMenuItemDeleteResult.Error(response.error.message ?: "Unknown error")
            }
        }
    }

    // ========== Menu Location Operations ==========

    suspend fun fetchMenuLocations(site: SiteModel): NavMenuLocationsResult {
        val url = buildApiUrl(site, MENU_LOCATIONS_ENDPOINT)
        val mapType = object : TypeToken<Map<String, NavMenuLocationApiResponse>>() {}.type

        val response = wpapiGsonRequestBuilder.syncGetRequest<Map<String, NavMenuLocationApiResponse>>(
            restClient = this,
            url = url,
            type = mapType
        )

        return when (response) {
            is WPAPIResponse.Success -> {
                val locations = response.data?.map { (slug, location) ->
                    location.toNavMenuLocationModel(site.id, slug)
                } ?: emptyList()
                NavMenuLocationsResult.Success(locations)
            }
            is WPAPIResponse.Error -> {
                NavMenuLocationsResult.Error(response.error.message ?: "Unknown error")
            }
        }
    }

    // ========== Helper Functions ==========

    private fun buildApiUrl(site: SiteModel, endpoint: String): String {
        val baseUrl = site.wpApiRestUrl?.takeIf { it.isNotEmpty() } ?: "${site.url}/wp-json"
        return "$baseUrl/$endpoint"
    }

    private fun buildMenuBody(menu: NavMenuModel): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            put("name", menu.name)
            if (menu.description.isNotEmpty()) {
                put("description", menu.description)
            }
            if (menu.locations.isNotEmpty()) {
                // Parse JSON array string to list
                put("locations", parseJsonArray(menu.locations))
            }
            put("auto_add", menu.autoAdd)
        }
    }

    private fun buildMenuItemBody(item: NavMenuItemModel): Map<String, Any> {
        return mutableMapOf<String, Any>().apply {
            put("menus", item.menuId)
            put("title", item.title)
            put("url", item.url)
            put("type", item.type)
            if (item.objectType.isNotEmpty()) {
                put("object", item.objectType)
            }
            if (item.objectId > 0) {
                put("object_id", item.objectId)
            }
            put("parent", item.parentId)
            put("menu_order", item.menuOrder)
            if (item.target.isNotEmpty()) {
                put("target", item.target)
            }
            if (item.classes.isNotEmpty()) {
                put("classes", parseJsonArray(item.classes))
            }
            if (item.description.isNotEmpty()) {
                put("description", item.description)
            }
            if (item.attrTitle.isNotEmpty()) {
                put("attr_title", item.attrTitle)
            }
        }
    }

    private fun parseJsonArray(jsonArray: String): List<String> {
        return try {
            if (jsonArray.isEmpty() || jsonArray == "[]") {
                emptyList()
            } else {
                jsonArray.trim('[', ']')
                    .split(",")
                    .map { it.trim().trim('"') }
                    .filter { it.isNotEmpty() }
            }
        } catch (e: Exception) {
            emptyList()
        }
    }

    // ========== Extension Functions ==========

    private fun NavMenuApiResponse.toNavMenuModel(localSiteId: Int): NavMenuModel {
        return NavMenuModel().apply {
            this.localSiteId = localSiteId
            this.remoteMenuId = this@toNavMenuModel.id
            this.name = this@toNavMenuModel.name
            this.slug = this@toNavMenuModel.slug
            this.description = this@toNavMenuModel.description ?: ""
            this.locations = this@toNavMenuModel.locations?.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]"
            ) { "\"$it\"" } ?: "[]"
            this.autoAdd = this@toNavMenuModel.autoAdd ?: false
        }
    }

    private fun NavMenuItemApiResponse.toNavMenuItemModel(localSiteId: Int): NavMenuItemModel {
        return NavMenuItemModel().apply {
            this.localSiteId = localSiteId
            this.remoteItemId = this@toNavMenuItemModel.id
            this.menuId = this@toNavMenuItemModel.menuId
            this.title = this@toNavMenuItemModel.title?.rendered ?: ""
            this.url = this@toNavMenuItemModel.url ?: ""
            this.type = this@toNavMenuItemModel.type ?: ""
            this.objectType = this@toNavMenuItemModel.objectType ?: ""
            this.objectId = this@toNavMenuItemModel.objectId ?: 0
            this.parentId = this@toNavMenuItemModel.parentId ?: 0
            this.menuOrder = this@toNavMenuItemModel.menuOrder ?: 0
            this.target = this@toNavMenuItemModel.target ?: ""
            this.classes = this@toNavMenuItemModel.classes?.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]"
            ) { "\"$it\"" } ?: "[]"
            this.description = this@toNavMenuItemModel.description ?: ""
            this.attrTitle = this@toNavMenuItemModel.attrTitle ?: ""
        }
    }

    private fun NavMenuLocationApiResponse.toNavMenuLocationModel(
        localSiteId: Int,
        slug: String
    ): NavMenuLocationModel {
        return NavMenuLocationModel().apply {
            this.localSiteId = localSiteId
            this.name = slug
            this.description = this@toNavMenuLocationModel.description ?: this@toNavMenuLocationModel.name
            this.menuId = this@toNavMenuLocationModel.menuId ?: 0
        }
    }

    // ========== Result Types ==========

    sealed class NavMenusResult {
        data class Success(val menus: List<NavMenuModel>) : NavMenusResult()
        data class Error(val message: String) : NavMenusResult()
    }

    sealed class NavMenuResult {
        data class Success(val menu: NavMenuModel) : NavMenuResult()
        data class Error(val message: String) : NavMenuResult()
    }

    sealed class NavMenuDeleteResult {
        data object Success : NavMenuDeleteResult()
        data class Error(val message: String) : NavMenuDeleteResult()
    }

    sealed class NavMenuItemsResult {
        data class Success(val items: List<NavMenuItemModel>) : NavMenuItemsResult()
        data class Error(val message: String) : NavMenuItemsResult()
    }

    sealed class NavMenuItemResult {
        data class Success(val item: NavMenuItemModel) : NavMenuItemResult()
        data class Error(val message: String) : NavMenuItemResult()
    }

    sealed class NavMenuItemDeleteResult {
        data object Success : NavMenuItemDeleteResult()
        data class Error(val message: String) : NavMenuItemDeleteResult()
    }

    sealed class NavMenuLocationsResult {
        data class Success(val locations: List<NavMenuLocationModel>) : NavMenuLocationsResult()
        data class Error(val message: String) : NavMenuLocationsResult()
    }
}
