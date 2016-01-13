package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;

import java.util.List;

public class MenuModel {
    // Menu table column names
    public static final String ID_COLUMN_NAME = "id";
    public static final String NAME_COLUMN_NAME = "name";
    public static final String DETAILS_COLUMN_NAME = "details";
    public static final String LOCATIONS_COLUMN_NAME = "locations";
    public static final String ITEMS_COLUMN_NAME = "items";

    public static final String MENUS_TABLE_NAME = "menus";
    public static final String CREATE_MENUS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    MENUS_TABLE_NAME +
                    " (" +
                    ID_COLUMN_NAME + " TEXT PRIMARY KEY, " +
                    NAME_COLUMN_NAME + " TEXT, " +
                    DETAILS_COLUMN_NAME + " TEXT, " +
                    LOCATIONS_COLUMN_NAME + " TEXT, " +
                    ITEMS_COLUMN_NAME + " TEXT" +
                    ");";

    public String details;
    public String menuId;
    public String name;
    public List<MenuItemModel> menuItems;
    public List<MenuLocationModel> locations;

    /**
     * Sets values from a local database {@link Cursor}.
     */
    public void deserializeDatabaseCursor(Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst() || cursor.getCount() == 0) return;

        menuId = SiteSettingsModel.getStringFromCursor(cursor, ID_COLUMN_NAME);
        name = SiteSettingsModel.getStringFromCursor(cursor, NAME_COLUMN_NAME);
        details = SiteSettingsModel.getStringFromCursor(cursor, DETAILS_COLUMN_NAME);
//        locations = SiteSettingsModel.getStringFromCursor(cursor, LOCATIONS_COLUMN_NAME);
//        menuItems = SiteSettingsModel.getStringFromCursor(cursor, ITEMS_COLUMN_NAME);
    }

    /**
     * Creates a {@link ContentValues} object to store this object in a local database.
     */
    public ContentValues serializeToDatabase() {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN_NAME, menuId);
        values.put(NAME_COLUMN_NAME, name);
        values.put(DETAILS_COLUMN_NAME, details);
        values.put(LOCATIONS_COLUMN_NAME, serializeMenuLocations());
        values.put(ITEMS_COLUMN_NAME, serializeMenuItems());
        return values;
    }

    public String serializeMenuItems() {
        StringBuilder builder = new StringBuilder();
        for (MenuItemModel item : menuItems) {
            builder.append(item.itemId);
            builder.append(",");
        }
        return builder.substring(0, builder.length() - 1);
    }

    public String serializeMenuLocations() {
        StringBuilder builder = new StringBuilder();
        for (MenuLocationModel location : locations) {
            builder.append(location.name);
            builder.append(",");
        }
        return builder.substring(0, builder.length() - 1);
    }
}
