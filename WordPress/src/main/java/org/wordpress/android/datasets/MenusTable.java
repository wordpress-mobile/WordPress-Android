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
    public static final String DROP_TABLE_SQL = "DROP TABLE ";
    public static final String SELECT_ALL_LOCATION_SQL =
            "SELECT * FROM " + MenuLocationModel.MENU_LOCATIONS_TABLE_NAME + " ";
    public static final String SELECT_ALL_MENU_SQL =
            "SELECT * FROM " + MenuModel.MENUS_TABLE_NAME + " ";
    public static final String SELECT_ALL_ITEM_SQL =
            "SELECT * FROM " + MenuItemModel.MENU_ITEMS_TABLE_NAME + " ";

    /**
     * Creates tables for {@link MenuModel}, {@link MenuLocationModel}, and {@link MenuItemModel}.
     */
    public static void createMenusTables(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL(MenuModel.CREATE_MENUS_TABLE_SQL);
            db.execSQL(MenuLocationModel.CREATE_MENU_LOCATIONS_TABLE_SQL);
            db.execSQL(MenuItemModel.CREATE_MENU_ITEMS_TABLE_SQL);
        }
    }

    public static void deleteMenusTables() {
        deleteMenusTables(WordPress.wpDB.getDatabase());
    }

    /**
     * Deletes tables for {@link MenuModel}, {@link MenuLocationModel}, and {@link MenuItemModel}.
     */
    public static void deleteMenusTables(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL(DROP_TABLE_SQL + MenuModel.MENUS_TABLE_NAME + ";");
            db.execSQL(DROP_TABLE_SQL + MenuLocationModel.MENU_LOCATIONS_TABLE_NAME + ";");
            db.execSQL(DROP_TABLE_SQL + MenuItemModel.MENU_ITEMS_TABLE_NAME + ";");
        }
    }

    /**
     * Passthrough for {@link MenusTable#getMenuFromId(SQLiteDatabase, long)} using
     * {@link WordPress#wpDB}'s database.
     */
    public static MenuModel getMenuFromId(long id) {
        return getMenuFromId(WordPress.wpDB.getDatabase(), id);
    }

    /**
     * Attempts to load a {@link MenuModel} from a database.
     *
     * @param id
     *  id of the Menu to load
     * @return
     *  a {@link MenuModel} deserialized from the database, null if id is not recognized
     */
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

    public static boolean saveMenu(MenuModel menu) {
        return saveMenu(WordPress.wpDB.getDatabase(), menu);
    }

    /**
     * Attempts to save a {@link MenuModel} to a database. Child {@link MenuItemModel}'s and
     * {@link MenuLocationModel}'s are stored as a comma-separated string list of IDs.
     */
    public static boolean saveMenu(SQLiteDatabase db, MenuModel menu) {
        if (menu == null) return false;

        ContentValues values = menu.serializeToDatabase();
        return db.insertWithOnConflict(
                MenuModel.MENUS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public static boolean saveMenuItem(MenuItemModel item) {
        return saveMenuItem(WordPress.wpDB.getDatabase(), item);
    }

    /**
     * Attempts to save a {@link MenuItemModel} to a database.
     */
    public static boolean saveMenuItem(SQLiteDatabase db, MenuItemModel item) {
        if (item == null) return false;

        ContentValues values = item.serializeToDatabase();
        return db.insertWithOnConflict(
                MenuItemModel.MENU_ITEMS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public static boolean saveMenuLocation(MenuLocationModel location) {
        return saveMenuLocation(WordPress.wpDB.getDatabase(), location);
    }

    /**
     * Attempts to save a {@link MenuLocationModel} to a database.
     */
    public static boolean saveMenuLocation(SQLiteDatabase db, MenuLocationModel location) {
        if (location == null) return false;

        ContentValues values = location.serializeToDatabase();
        return db.insertWithOnConflict(
                MenuLocationModel.MENU_LOCATIONS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }
}
