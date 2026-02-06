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
import org.wordpress.android.ui.navmenus.LinkableItemOption
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
import uniffi.wp_api.PostEndpointType
import uniffi.wp_api.PostListParams
import uniffi.wp_api.TermEndpointType
import uniffi.wp_api.TermListParams
import uniffi.wp_api.WpApiParamOrder
import uniffi.wp_api.WpApiParamPostsOrderBy
import uniffi.wp_api.WpApiParamTermsOrderBy
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

    suspend fun fetchMenus(site: SiteModel, offset: Int = 0): NavMenuListResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenus().listWithEditContext(
                NavMenuListParams(
                    perPage = PAGE_SIZE,
                    offset = offset.toUInt()
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Fetched ${response.response.data.size} nav menus")
                val menus = response.response.data.map { it.toNavMenuModel(site.id) }
                val canLoadMore = response.response.data.size.toUInt() == PAGE_SIZE
                NavMenuListResult.Success(menus, canLoadMore)
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to fetch nav menus: $errorMessage")
                NavMenuListResult.Error(errorMessage)
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

    suspend fun fetchMenuItems(site: SiteModel, menuId: Long, offset: Int = 0): NavMenuItemListResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.navMenuItems().listWithEditContext(
                NavMenuItemListParams(
                    perPage = PAGE_SIZE,
                    offset = offset.toUInt(),
                    menus = listOf(menuId)
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Fetched ${response.response.data.size} menu items")
                val items = response.response.data.map { it.toNavMenuItemModel(site.id, menuId) }
                val canLoadMore = response.response.data.size.toUInt() == PAGE_SIZE
                NavMenuItemListResult.Success(items, canLoadMore)
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to fetch menu items: $errorMessage")
                NavMenuItemListResult.Error(errorMessage)
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

    // ========== Linkable Items Operations ==========

    /**
     * Fetches posts for menu item linking. Sorted by date (newest first).
     */
    suspend fun fetchPosts(site: SiteModel, offset: Int = 0): LinkableItemsResult =
        fetchPostType(site, PostEndpointType.Posts, "posts", offset)

    /**
     * Fetches pages for menu item linking. Sorted by date (newest first).
     */
    suspend fun fetchPages(site: SiteModel, offset: Int = 0): LinkableItemsResult =
        fetchPostType(site, PostEndpointType.Pages, "pages", offset)

    /**
     * Fetches categories for menu item linking. Sorted alphabetically.
     */
    suspend fun fetchCategories(site: SiteModel, offset: Int = 0): LinkableItemsResult =
        fetchTermType(site, TermEndpointType.Categories, "categories", offset)

    /**
     * Fetches tags for menu item linking. Sorted alphabetically.
     */
    suspend fun fetchTags(site: SiteModel, offset: Int = 0): LinkableItemsResult =
        fetchTermType(site, TermEndpointType.Tags, "tags", offset)

    private suspend fun fetchPostType(
        site: SiteModel,
        endpointType: PostEndpointType,
        typeName: String,
        offset: Int
    ): LinkableItemsResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.posts().listWithEditContext(
                postEndpointType = endpointType,
                params = PostListParams(
                    perPage = PAGE_SIZE,
                    offset = offset.toUInt(),
                    order = WpApiParamOrder.DESC,
                    orderby = WpApiParamPostsOrderBy.DATE
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Fetched ${response.response.data.size} $typeName")
                val items = response.response.data.map {
                    val title = it.title?.raw?.takeIf { raw -> raw.isNotBlank() }
                        ?: it.title?.rendered
                        ?: ""
                    LinkableItemOption(it.id, title)
                }
                val canLoadMore = response.response.data.size.toUInt() == PAGE_SIZE
                LinkableItemsResult.Success(items, canLoadMore)
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to fetch $typeName: $errorMessage")
                LinkableItemsResult.Error(errorMessage)
            }
        }
    }

    private suspend fun fetchTermType(
        site: SiteModel,
        endpointType: TermEndpointType,
        typeName: String,
        offset: Int
    ): LinkableItemsResult {
        val client = wpApiClientProvider.getWpApiClient(site)

        val response = client.request { requestBuilder ->
            requestBuilder.terms().listWithEditContext(
                termEndpointType = endpointType,
                params = TermListParams(
                    perPage = PAGE_SIZE,
                    offset = offset.toUInt(),
                    order = WpApiParamOrder.ASC,
                    orderby = WpApiParamTermsOrderBy.NAME
                )
            )
        }

        return when (response) {
            is WpRequestResult.Success -> {
                appLogWrapper.d(AppLog.T.API, "Fetched ${response.response.data.size} $typeName")
                val items = response.response.data.map { LinkableItemOption(it.id, it.name) }
                val canLoadMore = response.response.data.size.toUInt() == PAGE_SIZE
                LinkableItemsResult.Success(items, canLoadMore)
            }
            else -> {
                val errorMessage = parseErrorMessage(response)
                appLogWrapper.e(AppLog.T.API, "Failed to fetch $typeName: $errorMessage")
                LinkableItemsResult.Error(errorMessage)
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
        appLogWrapper.e(AppLog.T.API, "API error: $response")

        if (!networkUtilsWrapper.isNetworkAvailable()) {
            return context.getString(R.string.no_network_message)
        }

        return when (response) {
            is WpRequestResult.WpError<*> ->
                response.errorMessage.takeIf { it.isNotBlank() }
                    ?: context.getString(R.string.request_failed_message)
            else -> context.getString(R.string.request_failed_message)
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

    sealed class NavMenuListResult {
        data class Success(val menus: List<NavMenuModel>, val canLoadMore: Boolean) : NavMenuListResult()
        data class Error(val message: String) : NavMenuListResult()
    }

    sealed class NavMenuResult {
        data class Success(val menu: NavMenuModel) : NavMenuResult()
        data class Error(val message: String) : NavMenuResult()
    }

    sealed class NavMenuDeleteResult {
        data object Success : NavMenuDeleteResult()
        data class Error(val message: String) : NavMenuDeleteResult()
    }

    sealed class NavMenuItemListResult {
        data class Success(
            val items: List<NavMenuItemModel>,
            val canLoadMore: Boolean
        ) : NavMenuItemListResult()
        data class Error(val message: String) : NavMenuItemListResult()
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

    sealed class LinkableItemsResult {
        data class Success(
            val items: List<LinkableItemOption>,
            val canLoadMore: Boolean
        ) : LinkableItemsResult()
        data class Error(val message: String) : LinkableItemsResult()
    }

    companion object {
        private const val PAGE_SIZE = 20u
    }
}
