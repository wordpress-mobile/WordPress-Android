package org.wordpress.android.ui.navmenus.data

import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuItemModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuLocationModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.util.AppLog
import rs.wordpress.api.kotlin.WpRequestResult
import uniffi.wp_api.MenuLocationWithViewContext
import uniffi.wp_api.NavMenuCreateParams
import uniffi.wp_api.NavMenuItemCreateParams
import uniffi.wp_api.NavMenuItemListParams
import uniffi.wp_api.NavMenuItemStatus
import uniffi.wp_api.NavMenuItemUpdateParams
import uniffi.wp_api.NavMenuItemWithEditContext
import uniffi.wp_api.NavMenuListParams
import uniffi.wp_api.NavMenuUpdateParams
import uniffi.wp_api.NavMenuWithEditContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * REST client for WordPress navigation menus using wordpress-rs library.
 */
@Singleton
class NavMenuRestClient @Inject constructor(
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper
) {
    // ========== Menu Operations ==========

    suspend fun fetchMenus(site: SiteModel): NavMenusResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenus().listWithEditContext(
                NavMenuListParams(
                    perPage = 100u
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Fetched ${response.response.data.size} nav menus")
                val menus = response.response.data.map { it.toNavMenuModel(site.id) }
                NavMenusResult.Success(menus)
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to fetch nav menus: $errorMessage")
                NavMenusResult.Error(errorMessage)
            }
        }
    }

    suspend fun createMenu(site: SiteModel, menu: NavMenuModel): NavMenuResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenus().create(
                NavMenuCreateParams(
                    name = menu.name,
                    description = menu.description.takeIf { it.isNotEmpty() },
                    slug = null, // Let WordPress generate the slug
                    locations = parseLocationsArray(menu.locations).takeIf { it.isNotEmpty() },
                    autoAdd = menu.autoAdd
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Created nav menu: ${response.response.data.name}")
                NavMenuResult.Success(response.response.data.toNavMenuModel(site.id))
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to create nav menu: $errorMessage")
                NavMenuResult.Error(errorMessage)
            }
        }
    }

    suspend fun updateMenu(site: SiteModel, menu: NavMenuModel): NavMenuResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenus().update(
                navMenuId = menu.remoteMenuId,
                params = NavMenuUpdateParams(
                    name = menu.name,
                    description = menu.description.takeIf { it.isNotEmpty() },
                    locations = parseLocationsArray(menu.locations).takeIf { it.isNotEmpty() },
                    autoAdd = menu.autoAdd
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Updated nav menu: ${response.response.data.name}")
                NavMenuResult.Success(response.response.data.toNavMenuModel(site.id))
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to update nav menu: $errorMessage")
                NavMenuResult.Error(errorMessage)
            }
        }
    }

    suspend fun deleteMenu(site: SiteModel, menuId: Long): NavMenuDeleteResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenus().delete(menuId)
        }

        return when (response) {
            is WpRequestResult.Success -> {
                if (response.response.data.deleted) {
                    appLogWrapper.d(AppLog.T.API, "Deleted nav menu: $menuId")
                    NavMenuDeleteResult.Success
                } else {
                    NavMenuDeleteResult.Error("Menu was not deleted")
                }
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to delete nav menu: $errorMessage")
                NavMenuDeleteResult.Error(errorMessage)
            }
        }
    }

    // ========== Menu Item Operations ==========

    suspend fun fetchMenuItems(site: SiteModel, menuId: Long): NavMenuItemsResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenuItems().listWithEditContext(
                NavMenuItemListParams(
                    perPage = 100u,
                    menus = listOf(menuId)
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Fetched ${response.response.data.size} menu items")
                val items = response.response.data.map { it.toNavMenuItemModel(site.id, menuId) }
                NavMenuItemsResult.Success(items)
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to fetch menu items: $errorMessage")
                NavMenuItemsResult.Error(errorMessage)
            }
        }
    }

    suspend fun createMenuItem(site: SiteModel, item: NavMenuItemModel): NavMenuItemResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenuItems().create(
                buildMenuItemCreateParams(item)
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Created menu item: ${response.response.data.title.raw}")
                NavMenuItemResult.Success(
                    response.response.data.toNavMenuItemModel(site.id, item.menuId)
                )
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to create menu item: $errorMessage")
                NavMenuItemResult.Error(errorMessage)
            }
        }
    }

    suspend fun updateMenuItem(site: SiteModel, item: NavMenuItemModel): NavMenuItemResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenuItems().update(
                navMenuItemId = item.remoteItemId,
                params = buildMenuItemUpdateParams(item)
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Updated menu item: ${response.response.data.title.raw}")
                NavMenuItemResult.Success(
                    response.response.data.toNavMenuItemModel(site.id, item.menuId)
                )
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to update menu item: $errorMessage")
                NavMenuItemResult.Error(errorMessage)
            }
        }
    }

    suspend fun deleteMenuItem(site: SiteModel, itemId: Long): NavMenuItemDeleteResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenuItems().delete(itemId)
        }

        return when (response) {
            is WpRequestResult.Success -> {
                if (response.response.data.deleted) {
                    appLogWrapper.d(AppLog.T.API, "Deleted menu item: $itemId")
                    NavMenuItemDeleteResult.Success
                } else {
                    NavMenuItemDeleteResult.Error("Menu item was not deleted")
                }
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to delete menu item: $errorMessage")
                NavMenuItemDeleteResult.Error(errorMessage)
            }
        }
    }

    // ========== Menu Location Operations ==========

    suspend fun fetchMenuLocations(site: SiteModel): NavMenuLocationsResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.menuLocations().listWithViewContext()
        }

        return when (response) {
            is WpRequestResult.Success -> {
                val locationsData = response.response.data
                appLogWrapper.d(AppLog.T.API, "Fetched menu locations")
                // The response is a Map<String, MenuLocationWithViewContext>
                @Suppress("UNCHECKED_CAST")
                val locationsMap = locationsData as? Map<String, MenuLocationWithViewContext>
                val locations = locationsMap?.map { entry ->
                    entry.value.toNavMenuLocationModel(site.id, entry.key)
                } ?: emptyList()
                NavMenuLocationsResult.Success(locations)
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to fetch menu locations: $errorMessage")
                NavMenuLocationsResult.Error(errorMessage)
            }
        }
    }

    // ========== Helper Functions ==========

    private fun buildMenuItemCreateParams(item: NavMenuItemModel): NavMenuItemCreateParams {
        return NavMenuItemCreateParams(
            title = item.title,
            url = item.url.takeIf { it.isNotEmpty() },
            status = NavMenuItemStatus.PUBLISH,
            menus = item.menuId,
            parent = item.parentId.takeIf { it > 0 },
            menuOrder = item.menuOrder.coerceAtLeast(1).toLong(),
            `object` = item.objectType.takeIf { it.isNotEmpty() },
            objectId = item.objectId.takeIf { it > 0 },
            description = item.description.takeIf { it.isNotEmpty() }
        )
    }

    private fun buildMenuItemUpdateParams(item: NavMenuItemModel): NavMenuItemUpdateParams {
        return NavMenuItemUpdateParams(
            title = item.title,
            url = item.url.takeIf { it.isNotEmpty() },
            status = NavMenuItemStatus.PUBLISH,
            menus = item.menuId,
            parent = item.parentId.takeIf { it > 0 },
            menuOrder = item.menuOrder.coerceAtLeast(1).toLong(),
            `object` = item.objectType.takeIf { it.isNotEmpty() },
            objectId = item.objectId.takeIf { it > 0 },
            description = item.description.takeIf { it.isNotEmpty() }
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private fun parseLocationsArray(jsonArray: String): List<String> {
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
            appLogWrapper.e(AppLog.T.API, "Failed to parse locations array: $jsonArray")
            emptyList()
        }
    }

    private fun parseErrorMessage(response: WpRequestResult<*>): String {
        return when (response) {
            is WpRequestResult.Success -> "Unexpected error"
            else -> {
                appLogWrapper.e(AppLog.T.API, "API error: $response")
                "Request failed"
            }
        }
    }

    private fun mapTypeLabelToType(typeLabel: String?): String {
        return when (typeLabel?.lowercase()) {
            "custom link" -> NavMenuItemModel.TYPE_CUSTOM
            "page" -> NavMenuItemModel.TYPE_POST_TYPE
            "post" -> NavMenuItemModel.TYPE_POST_TYPE
            "category" -> NavMenuItemModel.TYPE_TAXONOMY
            "tag" -> NavMenuItemModel.TYPE_TAXONOMY
            else -> NavMenuItemModel.TYPE_CUSTOM
        }
    }

    // ========== Extension Functions ==========

    private fun NavMenuWithEditContext.toNavMenuModel(localSiteId: Int): NavMenuModel {
        return NavMenuModel().apply {
            this.localSiteId = localSiteId
            this.remoteMenuId = this@toNavMenuModel.id
            this.name = this@toNavMenuModel.name
            this.slug = this@toNavMenuModel.slug
            this.description = this@toNavMenuModel.description ?: ""
            this.locations = this@toNavMenuModel.locations.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]"
            ) { "\"$it\"" }
            this.autoAdd = this@toNavMenuModel.autoAdd
        }
    }

    private fun NavMenuItemWithEditContext.toNavMenuItemModel(localSiteId: Int, menuId: Long): NavMenuItemModel {
        return NavMenuItemModel().apply {
            this.localSiteId = localSiteId
            this.remoteItemId = this@toNavMenuItemModel.id
            this.menuId = menuId
            this.title = this@toNavMenuItemModel.title.raw ?: ""
            this.url = this@toNavMenuItemModel.url ?: ""
            this.type = mapTypeLabelToType(this@toNavMenuItemModel.typeLabel)
            this.objectType = this@toNavMenuItemModel.`object` ?: ""
            this.objectId = this@toNavMenuItemModel.objectId ?: 0
            this.parentId = this@toNavMenuItemModel.parent ?: 0
            this.menuOrder = this@toNavMenuItemModel.menuOrder.toInt()
            this.target = this@toNavMenuItemModel.target ?: ""
            this.classes = this@toNavMenuItemModel.classes.joinToString(
                separator = ",",
                prefix = "[",
                postfix = "]"
            ) { "\"$it\"" }
            this.description = this@toNavMenuItemModel.description ?: ""
            this.attrTitle = this@toNavMenuItemModel.attrTitle ?: ""
        }
    }

    private fun MenuLocationWithViewContext.toNavMenuLocationModel(
        localSiteId: Int,
        slug: String
    ): NavMenuLocationModel {
        return NavMenuLocationModel().apply {
            this.localSiteId = localSiteId
            this.name = slug
            this.description = this@toNavMenuLocationModel.description ?: ""
            this.menuId = this@toNavMenuLocationModel.menu
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
