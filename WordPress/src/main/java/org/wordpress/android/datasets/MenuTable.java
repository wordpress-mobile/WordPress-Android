package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuItemModel;
import org.wordpress.android.models.MenuLocationModel;
import org.wordpress.android.models.MenuModel;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.util.SqlUtils.*;

public class MenuTable {
    //
    // Menu database table column names
    //
    public static final String ID_COLUMN = "menuId";
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
                    ID_COLUMN + " INTEGER PRIMARY KEY, " +
                    NAME_COLUMN + " TEXT, " +
                    DETAILS_COLUMN + " TEXT, " +
                    LOCATIONS_COLUMN + " TEXT, " +
                    ITEMS_COLUMN + " TEXT" +
                    ");";

    public static final String UNIQUE_WHERE_SQL = "WHERE " + ID_COLUMN + "=?";

    public static MenuModel deserializeFromDatabase(Cursor cursor) {
        if (cursor == null || cursor.isBeforeFirst() || cursor.isAfterLast()) return null;

        MenuModel menu = new MenuModel();
        menu.menuId = getLongFromCursor(cursor, ID_COLUMN);
        menu.name = getStringFromCursor(cursor, NAME_COLUMN);
        menu.details = getStringFromCursor(cursor, DETAILS_COLUMN);
        menu.locations = deserializeLocations(cursor);
        menu.menuItems = deserializeItems(cursor);
        return menu;
    }

    public static ContentValues serializeToDatabase(MenuModel menu) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, menu.menuId);
        values.put(NAME_COLUMN, menu.name);
        values.put(DETAILS_COLUMN, menu.details);
        values.put(LOCATIONS_COLUMN, separatedStringList(menu.locations, ","));
        values.put(ITEMS_COLUMN, separatedStringList(menu.menuItems, ","));
        return values;
    }

    public static List<Long> deserializeLocations(Cursor cursor) {
        String locationNames = getStringFromCursor(cursor, LOCATIONS_COLUMN);
        List<Long> locations = new ArrayList<>();
        for (String name : locationNames.split(",")) {
            locations.add(Long.valueOf(name));
        }
        return locations;
    }

    public static List<Long> deserializeItems(Cursor cursor) {
        String itemIds = getStringFromCursor(cursor, ITEMS_COLUMN);
        List<Long> items = new ArrayList<>();
        for (String id : itemIds.split(",")) {
            items.add(Long.valueOf(id));
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
}
