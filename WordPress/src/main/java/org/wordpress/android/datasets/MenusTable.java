package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

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
    public static final String SELECT_ALL_MENU_SQL =
            "SELECT * FROM " + MenuModel.MENUS_TABLE_NAME + " ";
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

    public static void deleteTable(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL("DELETE * FROM " + MenuModel.MENUS_TABLE_NAME);
            db.execSQL("DELETE * FROM " + MenuLocationModel.MENU_LOCATIONS_TABLE_NAME);
            db.execSQL("DELETE * FROM " + MenuItemModel.MENU_ITEMS_TABLE_NAME);
        }
    }

    /**
     * Pass-through to {@link MenusTable#getMenuLocationFromId(SQLiteDatabase, long)} using
     * {@link WordPress#wpDB} as the database argument.
     */
    public static MenuLocationModel getMenuLocationFromId(long id) {
        return getMenuLocationFromId(WordPress.wpDB.getDatabase(), id);
    }

    /**
     * Attempts to load a {@link MenuLocationModel} from a database.
     *
     * @param id
     *  id of the Menu Location to load
     * @return
     *  a {@link MenuLocationModel} deserialized from the database, null if name is not recognized
     */
    public static MenuLocationModel getMenuLocationFromId(SQLiteDatabase db, long id) {
        String sqlQuery = SELECT_ALL_LOCATION_SQL + where(MenuLocationModel.ID_COLUMN_NAME) + ";";
        Cursor cursor = db.rawQuery(sqlQuery, new String[]{ String.valueOf(id) });
        MenuLocationModel location = cursor.getCount() > 0 ? new MenuLocationModel() : null;
        if (location != null) {
            cursor.moveToFirst();
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
            cursor.moveToFirst();
            item.deserializeFromDatabase(cursor);
        }
        cursor.close();
        return item;
    }

    public static MenuModel getMenuFromId(long id) {
        return getMenuFromId(WordPress.wpDB.getDatabase(), id);
    }

    public static MenuModel getMenuFromId(SQLiteDatabase db, long id) {
        String sqlQuery = SELECT_ALL_MENU_SQL + where(MenuModel.ID_COLUMN_NAME) + ";";
        Cursor cursor = db.rawQuery(sqlQuery, new String[]{ String.valueOf(id) });
        MenuModel menu = cursor.getCount() > 0 ? new MenuModel() : null;
        if (menu != null) {
            cursor.moveToFirst();
            menu.deserializeFromDatabase(cursor);
        }
        cursor.close();
        return menu;
    }

    public static MenuModel getMenuForMenuId(long id) {
        return getMenuFromId(WordPress.wpDB.getDatabase(), id);
    }

    public static MenuItemModel getMenuItemForId(SQLiteDatabase db, long id) {
        if (id < 0) return null;
        String sqlCommand = "SELECT * FROM " + MenuItemModel.MENU_ITEMS_TABLE_NAME + " WHERE " + MenuItemModel.ID_COLUMN_NAME + "=?";
        Cursor cursor = db.rawQuery(sqlCommand, new String[]{String.valueOf(id)});
        MenuItemModel item = cursor.getCount() > 0 ? new MenuItemModel() : null;
        if (item != null) {
            cursor.moveToFirst();
            item.deserializeFromDatabase(cursor);
        }
        cursor.close();
        return item;
    }

    public static MenuItemModel getMenuItemForId(long id) {
        return getMenuItemFromId(WordPress.wpDB.getDatabase(), id);
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
                MenuLocationModel.MENU_LOCATIONS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
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

        return true;
    }
}
