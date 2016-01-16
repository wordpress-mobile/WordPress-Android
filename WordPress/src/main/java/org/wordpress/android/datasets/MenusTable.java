package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;

import static org.wordpress.android.util.DatabaseUtils.*;

/**
 * Provides methods to interface with a local SQL database for Menus related queries.
 */

public class MenusTable {
    public static final String SELECT_ALL_LOCATION_SQL =
            "SELECT * FROM " + MenuLocationModel.MENU_LOCATIONS_TABLE_NAME + " ";
    public static final String SELECT_ALL_ITEM_SQL =
            "SELECT * FROM " + MenuItemModel.MENU_ITEMS_TABLE_NAME + " ";

    /**
     * Creates tables for {@link MenuModel}, {@link MenuLocationModel}, and {@link MenuItemModel}.
     */
    public static void createTable(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL(MenuModel.CREATE_MENUS_TABLE_SQL);
            db.execSQL(MenuLocationModel.CREATE_MENU_LOCATIONS_TABLE_SQL);
            db.execSQL(MenuItemModel.CREATE_MENU_ITEMS_TABLE_SQL);
        }
    }

    /**
     * Pass-through to {@link MenusTable#getMenuLocationFromName(SQLiteDatabase, String)} using
     * {@link WordPress#wpDB} as the database argument.
     */
    public static MenuLocationModel getMenuLocationFromName(String name) {
        return getMenuLocationFromName(WordPress.wpDB.getDatabase(), name);
    }

    /**
     * Attempts to load a {@link MenuLocationModel} from a database.
     *
     * @param name
     *  name of the Menu Location to load
     * @return
     *  a {@link MenuLocationModel} deserialized from the database, null if name is not recognized
     */
    public static MenuLocationModel getMenuLocationFromName(SQLiteDatabase db, String name) {
        String sqlQuery = SELECT_ALL_LOCATION_SQL + where(MenuLocationModel.NAME_COLUMN_NAME) + ";";
        Cursor cursor = db.rawQuery(sqlQuery, new String[]{ name });
        MenuLocationModel location = cursor.getCount() > 0 ? new MenuLocationModel() : null;
        if (location != null) {
            location.deserializeFromDatabase(cursor);
        }
        cursor.close();
        return location;
    }

    /**
     * Pass-through to {@link MenusTable#getMenuItemFromId(SQLiteDatabase, long)} using
     * {@link WordPress#wpDB} as the database argument.
     */
    public static MenuItemModel getMenuItemFromId(long id) {
        return getMenuItemFromId(WordPress.wpDB.getDatabase(), id);
    }

    /**
     * Attempts to load a {@link MenuItemModel} from a database.
     *
     * @param id
     *  id of the Menu Item to load
     * @return
     *  a {@link MenuItemModel} deserialized from the database, null if id is not recognized
     */
    public static MenuItemModel getMenuItemFromId(SQLiteDatabase db, long id) {
        String sqlQuery = SELECT_ALL_ITEM_SQL + where(MenuItemModel.ID_COLUMN_NAME) + ";";
        Cursor cursor = db.rawQuery(sqlQuery, new String[]{ String.valueOf(id) });
        MenuItemModel item = cursor.getCount() > 0 ? new MenuItemModel() : null;
        if (item != null) {
            item.deserializeFromDatabase(cursor);
        }
        return item;
    }

    public static MenuModel getMenuForMenuId(String id) {
        if (TextUtils.isEmpty(id)) return null;
        String sqlCommand = "";
        MenuModel menu = new MenuModel();
        menu.deserializeFromDatabase(WordPress.wpDB.getDatabase().rawQuery(sqlCommand, null));
        return menu;
    }

    public static MenuItemModel getMenuItemForId(long id) {
        if (id < 0) return null;
        String sqlCommand = "SELECT * FROM " + MenuItemModel.MENU_ITEMS_TABLE_NAME + " WHERE " + MenuItemModel.ID_COLUMN_NAME + "=?";
        String[] args = new String[] { String.valueOf(id) };
        MenuItemModel item = new MenuItemModel();
        item.deserializeFromDatabase(WordPress.wpDB.getDatabase().rawQuery(sqlCommand, args));
        return item;
    }

    public static boolean saveMenu(MenuModel menu) {
        if (menu == null) return false;

        ContentValues values = menu.serializeToDatabase();
        boolean saved = WordPress.wpDB.getDatabase().insertWithOnConflict(
                MenuModel.MENUS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;

        saveMenuItems(menu);
        saveMenuLocations(menu);

        return saved;
    }

    public static boolean saveMenuLocation(MenuLocationModel location) {
        if (location == null) return false;

        ContentValues values = location.serializeToDatabase();
        boolean saved = WordPress.wpDB.getDatabase().insertWithOnConflict(
                MenuItemModel.MENU_ITEMS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
        return saved;
    }

    public static boolean saveMenuItem(MenuItemModel item) {
        if (item == null) return false;

        ContentValues values = item.serializeToDatabase();
        boolean saved = WordPress.wpDB.getDatabase().insertWithOnConflict(
                MenuItemModel.MENU_ITEMS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
        return saved;
    }

    public static boolean saveMenuLocations(MenuModel menu) {
        if (menu == null) return false;

//        for (MenuLocationModel location : menu.locations) {
//            saveMenuLocation(location);
//        }

        return true;
    }

    public static boolean saveMenuItems(MenuModel menu) {
        if (menu == null) return false;

//        for (MenuItemModel item : menu.menuItems) {
//            saveMenuItem(item);
//        }

        return true;
    }
}
