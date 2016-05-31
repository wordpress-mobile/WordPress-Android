package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import com.helpshift.support.util.ListUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;
import org.wordpress.android.util.SqlUtils;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.util.SqlUtils.*;

public class MenuTable {
    //
    // Menu database table column names
    //
    public static final String ID_COLUMN = "menuId";
    public static final String SITE_ID_COLUMN = "siteId";
    public static final String NAME_COLUMN = "menuName";
    public static final String DETAILS_COLUMN = "menuDetails";
    public static final String LOCATIONS_COLUMN = "menuLocations";
    public static final String ITEMS_COLUMN = "menuItems";

    //
    // Convenience Strings for SQL database table creation.
    //
    public static final String MENU_TABLE_NAME = "menus";
    public static final String CREATE_MENU_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    MENU_TABLE_NAME + " (" +
                    SITE_ID_COLUMN + " INTEGER NOT NULL," +
                    ID_COLUMN + " INTEGER PRIMARY KEY, " +
                    NAME_COLUMN + " TEXT, " +
                    DETAILS_COLUMN + " TEXT, " +
                    LOCATIONS_COLUMN + " TEXT, " +
                    ITEMS_COLUMN + " TEXT" +
                    ");";

    public static final String UNIQUE_WHERE_SQL = " WHERE " + ID_COLUMN + "=?";

    /** Well-formed SELECT query for selecting rows for a given site ID */
    public static final String SELECT_SITE_MENUS_SQL =
            "SELECT * FROM " + MENU_TABLE_NAME + " WHERE " + SITE_ID_COLUMN + "=?;";

    public static MenuModel deserializeFromDatabase(Cursor cursor) {
        if (cursor == null || cursor.isBeforeFirst() || cursor.isAfterLast()) return null;

        MenuModel menu = new MenuModel();
        menu.siteId = getLongFromCursor(cursor, SITE_ID_COLUMN);
        menu.menuId = getLongFromCursor(cursor, ID_COLUMN);
        menu.name = getStringFromCursor(cursor, NAME_COLUMN);
        menu.details = getStringFromCursor(cursor, DETAILS_COLUMN);
        menu.locations = deserializeLocations(cursor);
        menu.menuItems = deserializeItems(cursor);
        return menu;
    }

    public static ContentValues serializeToDatabase(MenuModel menu) {
        ContentValues values = new ContentValues();
        values.put(SITE_ID_COLUMN, menu.siteId);
        values.put(ID_COLUMN, menu.menuId);
        values.put(NAME_COLUMN, menu.name);
        values.put(DETAILS_COLUMN, menu.details);
        if (!ListUtils.isEmpty(menu.locations)) {
            String locationIds = "";
            for (MenuLocationModel location : menu.locations) {
                MenuLocationTable.saveMenuLocation(location);
                locationIds += location.name + ",";
            }
            values.put(LOCATIONS_COLUMN, locationIds.substring(0, locationIds.length() - 1));
        }
        if (!ListUtils.isEmpty(menu.menuItems)) {
            String itemIds = "";
            for (MenuItemModel item : menu.menuItems) {
                MenuItemTable.saveMenuItem(item);
                itemIds += item.itemId + ",";
            }
            values.put(ITEMS_COLUMN, itemIds.substring(0, itemIds.length() - 1));
        }
        return values;
    }

    public static List<MenuLocationModel> deserializeLocations(Cursor cursor) {
        String locationNames = getStringFromCursor(cursor, LOCATIONS_COLUMN);
        List<MenuLocationModel> locations = null;
        if (locationNames != null) {
            locations = new ArrayList<>();
            for (String name : locationNames.split(",")) {
                locations.add(MenuLocationTable.getMenuLocationForCurrentSite(name));
            }
        }
        return locations;
    }

    public static List<MenuItemModel> deserializeItems(Cursor cursor) {
        String itemIds = getStringFromCursor(cursor, ITEMS_COLUMN);
        List<MenuItemModel> items = null;
        if (itemIds != null) {
            items = new ArrayList<>();
            for (String id : itemIds.split(",")) {
                items.add(MenuItemTable.getMenuItem(Long.valueOf(id)));
            }
        }
        return items;
    }

    public static boolean saveMenu(MenuModel menu) {
        return saveMenu(WordPress.wpDB.getDatabase(), menu);
    }

    /**
     * Attempts to save a {@link MenuModel} to a database. Child {@link MenuItemModel}'s and
     * {@link MenuLocationModel}'s are stored as a comma-separated string list of IDs.
     */
    public static boolean saveMenu(SQLiteDatabase db, MenuModel menu) {
        if (menu == null) return false;
        ContentValues values = serializeToDatabase(menu);
        return db.insertWithOnConflict(
                MENU_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public static void saveMenus(List<MenuModel> menus) {
        saveMenus(WordPress.wpDB.getDatabase(), menus);
    }

    /**
     * Attempts to save a list of {@link MenuModel} to a database. Child {@link MenuItemModel}'s and
     * {@link MenuLocationModel}'s are stored as a comma-separated string list of IDs.
     */
    public static void saveMenus(SQLiteDatabase db, List<MenuModel> menus) {
        if (menus == null || menus.size() == 0) return;

        db.beginTransaction();
        try {
            for (MenuModel menu: menus) {
                ContentValues values = serializeToDatabase(menu);
                db.insertWithOnConflict(
                        MENU_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /**
     * Attempts to load a {@link MenuModel} from a database.
     *
     * @param id
     *  id of the Menu to load
     * @return
     *  a {@link MenuModel} deserialized from the database, null if id is not recognized
     */
    public static MenuModel getMenuFromId(long id) {
        String[] args = {String.valueOf(id)};
        Cursor cursor = WordPress.wpDB.getDatabase().rawQuery(UNIQUE_WHERE_SQL, args);
        cursor.moveToFirst();
        MenuModel menu = deserializeFromDatabase(cursor);
        cursor.close();
        return menu;
    }

    public static List<MenuModel> getAllMenusForCurrentSite() {
        return getAllMenusForSite(WordPress.currentBlog.getRemoteBlogId());
    }

    public static List<MenuModel> getAllMenusForSite(long siteId) {
        if (siteId < 0) return null;

        List<MenuModel> menus = new ArrayList<>();
        String[] args = {String.valueOf(siteId)};
        Cursor cursor = WordPress.wpDB.getDatabase().rawQuery(SELECT_SITE_MENUS_SQL, args);
        if (cursor.moveToFirst()) {
            do {
                MenuModel menu = deserializeFromDatabase(cursor);
                if (menu != null) menus.add(menu);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return menus;
    }

    public static void deleteMenu(long menuId) {
        if (menuId < 0) return;
        String[] args = {String.valueOf(menuId)};

        SQLiteDatabase db = WordPress.wpDB.getDatabase();

        db.beginTransaction();
        try {
            //delete menu items for this menu first
            MenuItemTable.deleteMenuItemForMenuId(menuId);
            //now delete menu
            db.delete(MENU_TABLE_NAME, UNIQUE_WHERE_SQL, args);
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }

    }

    public static void deleteAllMenus() {
        MenuItemTable.deleteAllMenuItems();
        WordPress.wpDB.getDatabase().delete(MENU_TABLE_NAME, null, null);
    }


}
