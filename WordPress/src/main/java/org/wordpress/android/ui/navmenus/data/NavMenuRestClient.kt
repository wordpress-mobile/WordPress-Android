package org.wordpress.android.ui.navmenus.data

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import org.wordpress.android.R
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.navmenus.models.NavMenuItemModel
import org.wordpress.android.ui.navmenus.models.NavMenuLocationModel
import org.wordpress.android.ui.navmenus.models.NavMenuModel
import org.wordpress.android.fluxc.network.rest.wpapi.rs.WpApiClientProvider
import org.wordpress.android.fluxc.utils.AppLogWrapper
import org.wordpress.android.ui.navmenus.parseJsonStringArray
import org.wordpress.android.ui.navmenus.toJsonStringArray
import org.wordpress.android.util.AppLog
import org.wordpress.android.util.NetworkUtilsWrapper
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
    @ApplicationContext private val context: Context,
    private val wpApiClientProvider: WpApiClientProvider,
    private val appLogWrapper: AppLogWrapper,
    private val networkUtilsWrapper: NetworkUtilsWrapper
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
                    locations = menu.locations.parseJsonStringArray().takeIf { it.isNotEmpty() },
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
                    locations = menu.locations.parseJsonStringArray().takeIf { it.isNotEmpty() },
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

    suspend fun fetchAllMenuItems(site: SiteModel): NavMenuItemsResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenuItems().listWithEditContext(
                NavMenuItemListParams(
                    perPage = 100u
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Fetched ${response.response.data.size} total menu items")
                val items = response.response.data.map { it.toNavMenuItemModel(site.id) }
                NavMenuItemsResult.Success(items)
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to fetch all menu items: $errorMessage")
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
                appLogWrapper.d(AppLog.T.API, "Fetched menu locations")
                val locationsMap = response.response.data.locations
                val locations = locationsMap.map { (slug, location) ->
                    location.toNavMenuLocationModel(site.id, slug)
                }
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

    private fun parseErrorMessage(response: WpRequestResult<*>): String {
        return when (response) {
            is WpRequestResult.Success -> "Unexpected error"
            else -> {
                appLogWrapper.e(AppLog.T.API, "API error: $response")
                if (!networkUtilsWrapper.isNetworkAvailable()) {
                    context.getString(R.string.no_network_message)
                } else {
                    context.getString(R.string.request_failed_message)
                }
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
        return NavMenuModel(
            localSiteId = localSiteId,
            remoteMenuId = id,
            name = name,
            slug = slug,
            description = description,
            locations = locations.toJsonStringArray(),
            autoAdd = autoAdd
        )
    }

    private fun NavMenuItemWithEditContext.toNavMenuItemModel(
        localSiteId: Int,
        menuId: Long = this.menus ?: 0L
    ): NavMenuItemModel {
        return NavMenuItemModel(
            localSiteId = localSiteId,
            remoteItemId = id,
            menuId = menuId,
            title = title.raw ?: "",
            url = url,
            type = mapTypeLabelToType(typeLabel),
            objectType = `object`,
            objectId = objectId,
            parentId = parent,
            menuOrder = menuOrder.toInt(),
            target = target,
            classes = classes.toJsonStringArray(),
            description = description,
            attrTitle = attrTitle
        )
    }

    private fun MenuLocationWithViewContext.toNavMenuLocationModel(
        localSiteId: Int,
        slug: String
    ): NavMenuLocationModel {
        return NavMenuLocationModel(
            localSiteId = localSiteId,
            name = slug,
            description = description,
            menuId = menu
        )
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
