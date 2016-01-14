package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;

public class MenusTable {
    public static void createTable(SQLiteDatabase db) {
        if (db != null) {
            db.execSQL(MenuModel.CREATE_MENUS_TABLE_SQL);
            db.execSQL(MenuLocationModel.CREATE_MENU_LOCATIONS_TABLE_SQL);
            db.execSQL(MenuItemModel.CREATE_MENU_ITEMS_TABLE_SQL);
        }
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

        for (MenuLocationModel location : menu.locations) {
            saveMenuLocation(location);
        }

        return true;
    }

    public static boolean saveMenuItems(MenuModel menu) {
        if (menu == null) return false;

        for (MenuItemModel item : menu.menuItems) {
            saveMenuItem(item);
        }

        return true;
    }
}
