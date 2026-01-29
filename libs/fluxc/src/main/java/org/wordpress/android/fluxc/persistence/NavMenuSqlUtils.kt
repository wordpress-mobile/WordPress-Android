package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.NavMenuItemModelTable
import com.wellsql.generated.NavMenuLocationModelTable
import com.wellsql.generated.NavMenuModelTable
import com.yarolegovich.wellsql.WellSql
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuItemModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuLocationModel
import org.wordpress.android.fluxc.model.navmenu.NavMenuModel

object NavMenuSqlUtils {
    // ========== Menu Operations ==========

    fun insertOrUpdateMenu(menu: NavMenuModel): Int {
        val existingMenus = WellSql.select(NavMenuModel::class.java)
            .where()
            .beginGroup()
            .equals(NavMenuModelTable.ID, menu.id)
            .or()
            .beginGroup()
            .equals(NavMenuModelTable.REMOTE_MENU_ID, menu.remoteMenuId)
            .equals(NavMenuModelTable.LOCAL_SITE_ID, menu.localSiteId)
            .endGroup()
            .endGroup()
            .endWhere()
            .asModel

        return if (existingMenus.isEmpty()) {
            WellSql.insert(menu).asSingleTransaction(true).execute()
            1
        } else {
            WellSql.update(NavMenuModel::class.java)
                .whereId(existingMenus[0].id)
                .put(menu, UpdateAllExceptId(NavMenuModel::class.java))
                .execute()
        }
    }

    fun getMenusForSite(site: SiteModel): List<NavMenuModel> {
        return WellSql.select(NavMenuModel::class.java)
            .where()
            .equals(NavMenuModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel
    }

    fun getMenuByRemoteId(site: SiteModel, remoteMenuId: Long): NavMenuModel? {
        val result = WellSql.select(NavMenuModel::class.java)
            .where()
            .beginGroup()
            .equals(NavMenuModelTable.LOCAL_SITE_ID, site.id)
            .equals(NavMenuModelTable.REMOTE_MENU_ID, remoteMenuId)
            .endGroup()
            .endWhere()
            .asModel

        return result.firstOrNull()
    }

    fun deleteMenu(menu: NavMenuModel): Int {
        // Also delete all menu items for this menu
        deleteMenuItemsForMenu(menu.localSiteId, menu.remoteMenuId)

        return WellSql.delete(NavMenuModel::class.java)
            .where()
            .beginGroup()
            .equals(NavMenuModelTable.REMOTE_MENU_ID, menu.remoteMenuId)
            .equals(NavMenuModelTable.LOCAL_SITE_ID, menu.localSiteId)
            .endGroup()
            .endWhere()
            .execute()
    }

    fun deleteAllMenusForSite(site: SiteModel): Int {
        // Also delete all menu items for this site
        deleteAllMenuItemsForSite(site)

        return WellSql.delete(NavMenuModel::class.java)
            .where()
            .equals(NavMenuModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .execute()
    }

    fun deleteAllMenus(): Int {
        deleteAllMenuItems()
        return WellSql.delete(NavMenuModel::class.java).execute()
    }

    // ========== Menu Item Operations ==========

    fun insertOrUpdateMenuItem(item: NavMenuItemModel): Int {
        val existingItems = WellSql.select(NavMenuItemModel::class.java)
            .where()
            .beginGroup()
            .equals(NavMenuItemModelTable.ID, item.id)
            .or()
            .beginGroup()
            .equals(NavMenuItemModelTable.REMOTE_ITEM_ID, item.remoteItemId)
            .equals(NavMenuItemModelTable.LOCAL_SITE_ID, item.localSiteId)
            .endGroup()
            .endGroup()
            .endWhere()
            .asModel

        return if (existingItems.isEmpty()) {
            WellSql.insert(item).asSingleTransaction(true).execute()
            1
        } else {
            WellSql.update(NavMenuItemModel::class.java)
                .whereId(existingItems[0].id)
                .put(item, UpdateAllExceptId(NavMenuItemModel::class.java))
                .execute()
        }
    }

    fun getMenuItemsForMenu(site: SiteModel, menuId: Long): List<NavMenuItemModel> {
        return WellSql.select(NavMenuItemModel::class.java)
            .where()
            .beginGroup()
            .equals(NavMenuItemModelTable.LOCAL_SITE_ID, site.id)
            .equals(NavMenuItemModelTable.MENU_ID, menuId)
            .endGroup()
            .endWhere()
            .orderBy(NavMenuItemModelTable.MENU_ORDER, com.yarolegovich.wellsql.SelectQuery.ORDER_ASCENDING)
            .asModel
    }

    fun getMenuItemByRemoteId(site: SiteModel, remoteItemId: Long): NavMenuItemModel? {
        val result = WellSql.select(NavMenuItemModel::class.java)
            .where()
            .beginGroup()
            .equals(NavMenuItemModelTable.LOCAL_SITE_ID, site.id)
            .equals(NavMenuItemModelTable.REMOTE_ITEM_ID, remoteItemId)
            .endGroup()
            .endWhere()
            .asModel

        return result.firstOrNull()
    }

    fun deleteMenuItem(item: NavMenuItemModel): Int {
        return WellSql.delete(NavMenuItemModel::class.java)
            .where()
            .beginGroup()
            .equals(NavMenuItemModelTable.REMOTE_ITEM_ID, item.remoteItemId)
            .equals(NavMenuItemModelTable.LOCAL_SITE_ID, item.localSiteId)
            .endGroup()
            .endWhere()
            .execute()
    }

    private fun deleteMenuItemsForMenu(localSiteId: Int, menuId: Long): Int {
        return WellSql.delete(NavMenuItemModel::class.java)
            .where()
            .beginGroup()
            .equals(NavMenuItemModelTable.LOCAL_SITE_ID, localSiteId)
            .equals(NavMenuItemModelTable.MENU_ID, menuId)
            .endGroup()
            .endWhere()
            .execute()
    }

    private fun deleteAllMenuItemsForSite(site: SiteModel): Int {
        return WellSql.delete(NavMenuItemModel::class.java)
            .where()
            .equals(NavMenuItemModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .execute()
    }

    private fun deleteAllMenuItems(): Int {
        return WellSql.delete(NavMenuItemModel::class.java).execute()
    }

    // ========== Menu Location Operations ==========

    fun insertOrUpdateMenuLocation(location: NavMenuLocationModel): Int {
        val existingLocations = WellSql.select(NavMenuLocationModel::class.java)
            .where()
            .beginGroup()
            .equals(NavMenuLocationModelTable.ID, location.id)
            .or()
            .beginGroup()
            .equals(NavMenuLocationModelTable.NAME, location.name)
            .equals(NavMenuLocationModelTable.LOCAL_SITE_ID, location.localSiteId)
            .endGroup()
            .endGroup()
            .endWhere()
            .asModel

        return if (existingLocations.isEmpty()) {
            WellSql.insert(location).asSingleTransaction(true).execute()
            1
        } else {
            WellSql.update(NavMenuLocationModel::class.java)
                .whereId(existingLocations[0].id)
                .put(location, UpdateAllExceptId(NavMenuLocationModel::class.java))
                .execute()
        }
    }

    fun getMenuLocationsForSite(site: SiteModel): List<NavMenuLocationModel> {
        return WellSql.select(NavMenuLocationModel::class.java)
            .where()
            .equals(NavMenuLocationModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .asModel
    }

    fun deleteAllMenuLocationsForSite(site: SiteModel): Int {
        return WellSql.delete(NavMenuLocationModel::class.java)
            .where()
            .equals(NavMenuLocationModelTable.LOCAL_SITE_ID, site.id)
            .endWhere()
            .execute()
    }

    fun deleteAllMenuLocations(): Int {
        return WellSql.delete(NavMenuLocationModel::class.java).execute()
    }
}
