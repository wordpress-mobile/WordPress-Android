package org.wordpress.android.fluxc.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import org.wordpress.android.fluxc.Dispatcher
import org.wordpress.android.fluxc.Payload
import org.wordpress.android.fluxc.action.NavMenuAction
import org.wordpress.android.fluxc.annotations.action.Action
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuItemModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuLocationModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuModel
import org.wordpress.android.fluxc.module.FLUXC_SCOPE
import org.wordpress.android.fluxc.network.BaseRequest.BaseNetworkError
import org.wordpress.android.fluxc.network.rest.wpapi.navmenu.NavMenuRsRestClient
import org.wordpress.android.fluxc.persistence.NavMenuSqlUtils
import org.wordpress.android.util.AppLog
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class NavMenuStore @Inject constructor(
    dispatcher: Dispatcher,
    @Named(FLUXC_SCOPE) private val scope: CoroutineScope,
    private val navMenuRestClient: NavMenuRsRestClient
) : Store(dispatcher) {

    // ========== Payload Classes ==========

    class FetchMenusPayload(val site: SiteModel) : Payload<BaseNetworkError>()

    class FetchMenusResponsePayload(
        val site: SiteModel,
        val menus: List<NavMenuModel>
    ) : Payload<NavMenuError>() {
        constructor(error: NavMenuError, site: SiteModel) : this(site, emptyList()) {
            this.error = error
        }
    }

    class RemoteMenuPayload(
        val site: SiteModel,
        val menu: NavMenuModel
    ) : Payload<NavMenuError>()

    class FetchMenuItemsPayload(
        val site: SiteModel,
        val menuId: Long
    ) : Payload<BaseNetworkError>()

    class FetchMenuItemsResponsePayload(
        val site: SiteModel,
        val menuId: Long,
        val items: List<NavMenuItemModel>
    ) : Payload<NavMenuError>() {
        constructor(error: NavMenuError, site: SiteModel, menuId: Long) : this(site, menuId, emptyList()) {
            this.error = error
        }
    }

    class RemoteMenuItemPayload(
        val site: SiteModel,
        val item: NavMenuItemModel
    ) : Payload<NavMenuError>()

    class FetchMenuLocationsPayload(val site: SiteModel) : Payload<BaseNetworkError>()

    class FetchMenuLocationsResponsePayload(
        val site: SiteModel,
        val locations: List<NavMenuLocationModel>
    ) : Payload<NavMenuError>() {
        constructor(error: NavMenuError, site: SiteModel) : this(site, emptyList()) {
            this.error = error
        }
    }

    // ========== OnChanged Events ==========

    class OnNavMenuChanged(
        var rowsAffected: Int = 0,
        var causeOfChange: NavMenuAction? = null
    ) : OnChanged<NavMenuError>()

    class OnNavMenuItemChanged(
        var rowsAffected: Int = 0,
        var causeOfChange: NavMenuAction? = null
    ) : OnChanged<NavMenuError>()

    class OnNavMenuLocationChanged(
        var rowsAffected: Int = 0
    ) : OnChanged<NavMenuError>()

    // ========== Error Types ==========

    class NavMenuError(
        var type: NavMenuErrorType = NavMenuErrorType.GENERIC_ERROR,
        var message: String = ""
    ) : OnChangedError

    enum class NavMenuErrorType {
        GENERIC_ERROR,
        INVALID_RESPONSE,
        NOT_FOUND,
        UNAUTHORIZED
    }

    // ========== Store Registration ==========

    override fun onRegister() {
        AppLog.d(AppLog.T.API, "NavMenuStore onRegister")
    }

    // ========== Action Handling ==========

    @Subscribe(threadMode = ThreadMode.ASYNC)
    override fun onAction(action: Action<*>) {
        val actionType = action.type as? NavMenuAction ?: return

        when (actionType) {
            NavMenuAction.FETCH_MENUS -> handleFetchMenus(action.payload as FetchMenusPayload)
            NavMenuAction.FETCHED_MENUS -> handleFetchedMenus(action.payload as FetchMenusResponsePayload)
            NavMenuAction.PUSH_MENU -> handlePushMenu(action.payload as RemoteMenuPayload)
            NavMenuAction.PUSHED_MENU -> handlePushedMenu(action.payload as RemoteMenuPayload)
            NavMenuAction.DELETE_MENU -> handleDeleteMenu(action.payload as RemoteMenuPayload)
            NavMenuAction.DELETED_MENU -> handleDeletedMenu(action.payload as RemoteMenuPayload)
            NavMenuAction.FETCH_MENU_ITEMS -> handleFetchMenuItems(action.payload as FetchMenuItemsPayload)
            NavMenuAction.FETCHED_MENU_ITEMS ->
                handleFetchedMenuItems(action.payload as FetchMenuItemsResponsePayload)
            NavMenuAction.PUSH_MENU_ITEM -> handlePushMenuItem(action.payload as RemoteMenuItemPayload)
            NavMenuAction.PUSHED_MENU_ITEM -> handlePushedMenuItem(action.payload as RemoteMenuItemPayload)
            NavMenuAction.DELETE_MENU_ITEM -> handleDeleteMenuItem(action.payload as RemoteMenuItemPayload)
            NavMenuAction.DELETED_MENU_ITEM -> handleDeletedMenuItem(action.payload as RemoteMenuItemPayload)
            NavMenuAction.FETCH_MENU_LOCATIONS ->
                handleFetchMenuLocations(action.payload as FetchMenuLocationsPayload)
            NavMenuAction.FETCHED_MENU_LOCATIONS ->
                handleFetchedMenuLocations(action.payload as FetchMenuLocationsResponsePayload)
            NavMenuAction.REMOVE_ALL_MENUS -> handleRemoveAllMenus()
        }
    }

    // ========== Menu Handlers ==========

    private fun handleFetchMenus(payload: FetchMenusPayload) {
        scope.launch {
            val result = navMenuRestClient.fetchMenus(payload.site)
            val responsePayload = when (result) {
                is NavMenuRsRestClient.NavMenusResult.Success -> {
                    FetchMenusResponsePayload(payload.site, result.menus)
                }
                is NavMenuRsRestClient.NavMenusResult.Error -> {
                    FetchMenusResponsePayload(
                        NavMenuError(NavMenuErrorType.GENERIC_ERROR, result.message),
                        payload.site
                    )
                }
            }
            mDispatcher.dispatch(NavMenuActionBuilder.newFetchedMenusAction(responsePayload))
        }
    }

    private fun handleFetchedMenus(payload: FetchMenusResponsePayload) {
        val onChanged = OnNavMenuChanged(causeOfChange = NavMenuAction.FETCH_MENUS)
        if (payload.isError) {
            onChanged.error = payload.error
        } else {
            // Clear existing menus for this site and insert new ones
            NavMenuSqlUtils.deleteAllMenusForSite(payload.site)
            var rowsAffected = 0
            payload.menus.forEach { menu ->
                rowsAffected += NavMenuSqlUtils.insertOrUpdateMenu(menu)
            }
            onChanged.rowsAffected = rowsAffected
        }
        emitChange(onChanged)
    }

    private fun handlePushMenu(payload: RemoteMenuPayload) {
        scope.launch {
            val result = if (payload.menu.remoteMenuId > 0) {
                navMenuRestClient.updateMenu(payload.site, payload.menu)
            } else {
                navMenuRestClient.createMenu(payload.site, payload.menu)
            }
            val responsePayload = when (result) {
                is NavMenuRsRestClient.NavMenuResult.Success -> {
                    RemoteMenuPayload(payload.site, result.menu)
                }
                is NavMenuRsRestClient.NavMenuResult.Error -> {
                    RemoteMenuPayload(payload.site, payload.menu).apply {
                        error = NavMenuError(NavMenuErrorType.GENERIC_ERROR, result.message)
                    }
                }
            }
            mDispatcher.dispatch(NavMenuActionBuilder.newPushedMenuAction(responsePayload))
        }
    }

    private fun handlePushedMenu(payload: RemoteMenuPayload) {
        val onChanged = OnNavMenuChanged(causeOfChange = NavMenuAction.PUSH_MENU)
        if (payload.isError) {
            onChanged.error = payload.error
        } else {
            onChanged.rowsAffected = NavMenuSqlUtils.insertOrUpdateMenu(payload.menu)
        }
        emitChange(onChanged)
    }

    private fun handleDeleteMenu(payload: RemoteMenuPayload) {
        scope.launch {
            val result = navMenuRestClient.deleteMenu(payload.site, payload.menu.remoteMenuId)
            val responsePayload = when (result) {
                is NavMenuRsRestClient.NavMenuDeleteResult.Success -> {
                    RemoteMenuPayload(payload.site, payload.menu)
                }
                is NavMenuRsRestClient.NavMenuDeleteResult.Error -> {
                    RemoteMenuPayload(payload.site, payload.menu).apply {
                        error = NavMenuError(NavMenuErrorType.GENERIC_ERROR, result.message)
                    }
                }
            }
            mDispatcher.dispatch(NavMenuActionBuilder.newDeletedMenuAction(responsePayload))
        }
    }

    private fun handleDeletedMenu(payload: RemoteMenuPayload) {
        val onChanged = OnNavMenuChanged(causeOfChange = NavMenuAction.DELETE_MENU)
        if (payload.isError) {
            onChanged.error = payload.error
        } else {
            onChanged.rowsAffected = NavMenuSqlUtils.deleteMenu(payload.menu)
        }
        emitChange(onChanged)
    }

    // ========== Menu Item Handlers ==========

    private fun handleFetchMenuItems(payload: FetchMenuItemsPayload) {
        scope.launch {
            val result = navMenuRestClient.fetchMenuItems(payload.site, payload.menuId)
            val responsePayload = when (result) {
                is NavMenuRsRestClient.NavMenuItemsResult.Success -> {
                    FetchMenuItemsResponsePayload(payload.site, payload.menuId, result.items)
                }
                is NavMenuRsRestClient.NavMenuItemsResult.Error -> {
                    FetchMenuItemsResponsePayload(
                        NavMenuError(NavMenuErrorType.GENERIC_ERROR, result.message),
                        payload.site,
                        payload.menuId
                    )
                }
            }
            mDispatcher.dispatch(NavMenuActionBuilder.newFetchedMenuItemsAction(responsePayload))
        }
    }

    private fun handleFetchedMenuItems(payload: FetchMenuItemsResponsePayload) {
        val onChanged = OnNavMenuItemChanged(causeOfChange = NavMenuAction.FETCH_MENU_ITEMS)
        if (payload.isError) {
            onChanged.error = payload.error
        } else {
            var rowsAffected = 0
            payload.items.forEach { item ->
                rowsAffected += NavMenuSqlUtils.insertOrUpdateMenuItem(item)
            }
            onChanged.rowsAffected = rowsAffected
        }
        emitChange(onChanged)
    }

    private fun handlePushMenuItem(payload: RemoteMenuItemPayload) {
        scope.launch {
            val result = if (payload.item.remoteItemId > 0) {
                navMenuRestClient.updateMenuItem(payload.site, payload.item)
            } else {
                navMenuRestClient.createMenuItem(payload.site, payload.item)
            }
            val responsePayload = when (result) {
                is NavMenuRsRestClient.NavMenuItemResult.Success -> {
                    RemoteMenuItemPayload(payload.site, result.item)
                }
                is NavMenuRsRestClient.NavMenuItemResult.Error -> {
                    RemoteMenuItemPayload(payload.site, payload.item).apply {
                        error = NavMenuError(NavMenuErrorType.GENERIC_ERROR, result.message)
                    }
                }
            }
            mDispatcher.dispatch(NavMenuActionBuilder.newPushedMenuItemAction(responsePayload))
        }
    }

    private fun handlePushedMenuItem(payload: RemoteMenuItemPayload) {
        val onChanged = OnNavMenuItemChanged(causeOfChange = NavMenuAction.PUSH_MENU_ITEM)
        if (payload.isError) {
            onChanged.error = payload.error
        } else {
            onChanged.rowsAffected = NavMenuSqlUtils.insertOrUpdateMenuItem(payload.item)
        }
        emitChange(onChanged)
    }

    private fun handleDeleteMenuItem(payload: RemoteMenuItemPayload) {
        scope.launch {
            val result = navMenuRestClient.deleteMenuItem(payload.site, payload.item.remoteItemId)
            val responsePayload = when (result) {
                is NavMenuRsRestClient.NavMenuItemDeleteResult.Success -> {
                    RemoteMenuItemPayload(payload.site, payload.item)
                }
                is NavMenuRsRestClient.NavMenuItemDeleteResult.Error -> {
                    RemoteMenuItemPayload(payload.site, payload.item).apply {
                        error = NavMenuError(NavMenuErrorType.GENERIC_ERROR, result.message)
                    }
                }
            }
            mDispatcher.dispatch(NavMenuActionBuilder.newDeletedMenuItemAction(responsePayload))
        }
    }

    private fun handleDeletedMenuItem(payload: RemoteMenuItemPayload) {
        val onChanged = OnNavMenuItemChanged(causeOfChange = NavMenuAction.DELETE_MENU_ITEM)
        if (payload.isError) {
            onChanged.error = payload.error
        } else {
            onChanged.rowsAffected = NavMenuSqlUtils.deleteMenuItem(payload.item)
        }
        emitChange(onChanged)
    }

    // ========== Menu Location Handlers ==========

    private fun handleFetchMenuLocations(payload: FetchMenuLocationsPayload) {
        scope.launch {
            val result = navMenuRestClient.fetchMenuLocations(payload.site)
            val responsePayload = when (result) {
                is NavMenuRsRestClient.NavMenuLocationsResult.Success -> {
                    FetchMenuLocationsResponsePayload(payload.site, result.locations)
                }
                is NavMenuRsRestClient.NavMenuLocationsResult.Error -> {
                    FetchMenuLocationsResponsePayload(
                        NavMenuError(NavMenuErrorType.GENERIC_ERROR, result.message),
                        payload.site
                    )
                }
            }
            mDispatcher.dispatch(NavMenuActionBuilder.newFetchedMenuLocationsAction(responsePayload))
        }
    }

    private fun handleFetchedMenuLocations(payload: FetchMenuLocationsResponsePayload) {
        val onChanged = OnNavMenuLocationChanged()
        if (payload.isError) {
            onChanged.error = payload.error
        } else {
            // Clear existing locations for this site and insert new ones
            NavMenuSqlUtils.deleteAllMenuLocationsForSite(payload.site)
            var rowsAffected = 0
            payload.locations.forEach { location ->
                rowsAffected += NavMenuSqlUtils.insertOrUpdateMenuLocation(location)
            }
            onChanged.rowsAffected = rowsAffected
        }
        emitChange(onChanged)
    }

    private fun handleRemoveAllMenus() {
        val rowsAffected = NavMenuSqlUtils.deleteAllMenus()
        NavMenuSqlUtils.deleteAllMenuLocations()
        val onChanged = OnNavMenuChanged(
            rowsAffected = rowsAffected,
            causeOfChange = NavMenuAction.REMOVE_ALL_MENUS
        )
        emitChange(onChanged)
    }

    // ========== Public API ==========

    fun getMenusForSite(site: SiteModel): List<NavMenuModel> {
        return NavMenuSqlUtils.getMenusForSite(site)
    }

    fun getMenuByRemoteId(site: SiteModel, remoteMenuId: Long): NavMenuModel? {
        return NavMenuSqlUtils.getMenuByRemoteId(site, remoteMenuId)
    }

    fun getMenuItemsForMenu(site: SiteModel, menuId: Long): List<NavMenuItemModel> {
        return NavMenuSqlUtils.getMenuItemsForMenu(site, menuId)
    }

    fun getMenuItemByRemoteId(site: SiteModel, remoteItemId: Long): NavMenuItemModel? {
        return NavMenuSqlUtils.getMenuItemByRemoteId(site, remoteItemId)
    }

    fun getMenuLocationsForSite(site: SiteModel): List<NavMenuLocationModel> {
        return NavMenuSqlUtils.getMenuLocationsForSite(site)
    }
}

/**
 * Builder for NavMenu actions
 */
object NavMenuActionBuilder {
    @JvmStatic
    fun newFetchMenusAction(payload: NavMenuStore.FetchMenusPayload): Action<NavMenuStore.FetchMenusPayload> {
        return Action(NavMenuAction.FETCH_MENUS, payload)
    }

    @JvmStatic
    fun newFetchedMenusAction(
        payload: NavMenuStore.FetchMenusResponsePayload
    ): Action<NavMenuStore.FetchMenusResponsePayload> {
        return Action(NavMenuAction.FETCHED_MENUS, payload)
    }

    @JvmStatic
    fun newPushMenuAction(payload: NavMenuStore.RemoteMenuPayload): Action<NavMenuStore.RemoteMenuPayload> {
        return Action(NavMenuAction.PUSH_MENU, payload)
    }

    @JvmStatic
    fun newPushedMenuAction(payload: NavMenuStore.RemoteMenuPayload): Action<NavMenuStore.RemoteMenuPayload> {
        return Action(NavMenuAction.PUSHED_MENU, payload)
    }

    @JvmStatic
    fun newDeleteMenuAction(payload: NavMenuStore.RemoteMenuPayload): Action<NavMenuStore.RemoteMenuPayload> {
        return Action(NavMenuAction.DELETE_MENU, payload)
    }

    @JvmStatic
    fun newDeletedMenuAction(payload: NavMenuStore.RemoteMenuPayload): Action<NavMenuStore.RemoteMenuPayload> {
        return Action(NavMenuAction.DELETED_MENU, payload)
    }

    @JvmStatic
    fun newFetchMenuItemsAction(
        payload: NavMenuStore.FetchMenuItemsPayload
    ): Action<NavMenuStore.FetchMenuItemsPayload> {
        return Action(NavMenuAction.FETCH_MENU_ITEMS, payload)
    }

    @JvmStatic
    fun newFetchedMenuItemsAction(
        payload: NavMenuStore.FetchMenuItemsResponsePayload
    ): Action<NavMenuStore.FetchMenuItemsResponsePayload> {
        return Action(NavMenuAction.FETCHED_MENU_ITEMS, payload)
    }

    @JvmStatic
    fun newPushMenuItemAction(
        payload: NavMenuStore.RemoteMenuItemPayload
    ): Action<NavMenuStore.RemoteMenuItemPayload> {
        return Action(NavMenuAction.PUSH_MENU_ITEM, payload)
    }

    @JvmStatic
    fun newPushedMenuItemAction(
        payload: NavMenuStore.RemoteMenuItemPayload
    ): Action<NavMenuStore.RemoteMenuItemPayload> {
        return Action(NavMenuAction.PUSHED_MENU_ITEM, payload)
    }

    @JvmStatic
    fun newDeleteMenuItemAction(
        payload: NavMenuStore.RemoteMenuItemPayload
    ): Action<NavMenuStore.RemoteMenuItemPayload> {
        return Action(NavMenuAction.DELETE_MENU_ITEM, payload)
    }

    @JvmStatic
    fun newDeletedMenuItemAction(
        payload: NavMenuStore.RemoteMenuItemPayload
    ): Action<NavMenuStore.RemoteMenuItemPayload> {
        return Action(NavMenuAction.DELETED_MENU_ITEM, payload)
    }

    @JvmStatic
    fun newFetchMenuLocationsAction(
        payload: NavMenuStore.FetchMenuLocationsPayload
    ): Action<NavMenuStore.FetchMenuLocationsPayload> {
        return Action(NavMenuAction.FETCH_MENU_LOCATIONS, payload)
    }

    @JvmStatic
    fun newFetchedMenuLocationsAction(
        payload: NavMenuStore.FetchMenuLocationsResponsePayload
    ): Action<NavMenuStore.FetchMenuLocationsResponsePayload> {
        return Action(NavMenuAction.FETCHED_MENU_LOCATIONS, payload)
    }

    @JvmStatic
    fun newRemoveAllMenusAction(): Action<Unit> {
        return Action(NavMenuAction.REMOVE_ALL_MENUS, Unit)
    }
}
