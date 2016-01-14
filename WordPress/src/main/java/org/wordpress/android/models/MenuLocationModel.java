package org.wordpress.android.models;


import android.content.ContentValues;
import android.database.Cursor;

import org.wordpress.android.util.StringUtils;

import static org.wordpress.android.util.DatabaseUtils.*;

public class MenuLocationModel {
    // MenuLocation table column names
    public static final String NAME_COLUMN_NAME = "locationName";
    public static final String DETAILS_COLUMN_NAME = "locationDetails";
    public static final String DEFAULT_STATE_COLUMN_NAME = "locationDefaultState";
    public static final String MENU_COLUMN_NAME = "locationMenu";

    public static final String MENU_LOCATIONS_TABLE_NAME = "menu_locations";
    public static final String CREATE_MENU_LOCATIONS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    MENU_LOCATIONS_TABLE_NAME +
                    " (" +
                    NAME_COLUMN_NAME + " TEXT PRIMARY KEY, " +
                    DETAILS_COLUMN_NAME + " TEXT, " +
                    DEFAULT_STATE_COLUMN_NAME + " TEXT, " +
                    MENU_COLUMN_NAME + " INTEGER" +
                    ");";

    public String name;
    public String details;
    public String defaultState;
    public MenuModel menu;

    public static MenuLocationModel fromName(String name) {
        MenuLocationModel location = new MenuLocationModel();
        location.name = name;
        return location;
    }

    public static MenuLocationModel fromDatabase(Cursor cursor) {
        MenuLocationModel location = new MenuLocationModel();
        location.deserializeFromDatabase(cursor);
        return location;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MenuLocationModel)) return false;

        MenuLocationModel otherModel = (MenuLocationModel) other;
        return StringUtils.equals(name, otherModel.name) &&
                StringUtils.equals(details, otherModel.details) &&
                StringUtils.equals(defaultState, otherModel.defaultState) &&
                hasSameMenu(otherModel.menu);
    }

    public void deserializeFromDatabase(Cursor cursor) {
        if (cursor != null && cursor.getCount() != 0 && cursor.moveToFirst()) {
            name = getStringFromCursor(cursor, NAME_COLUMN_NAME);
            details = getStringFromCursor(cursor, DETAILS_COLUMN_NAME);
            defaultState = getStringFromCursor(cursor, DEFAULT_STATE_COLUMN_NAME);
            (menu = new MenuModel()).menuId = getLongFromCursor(cursor, MENU_COLUMN_NAME);
        }
    }

    /**
     * Creates a {@link ContentValues} object to store this object in a local database.
     */
    public ContentValues serializeToDatabase() {
        ContentValues values = new ContentValues();
        values.put(NAME_COLUMN_NAME, name);
        values.put(DETAILS_COLUMN_NAME, details);
        values.put(DEFAULT_STATE_COLUMN_NAME, defaultState);
        values.put(MENU_COLUMN_NAME, menu.menuId);
        return values;
    }

    private boolean hasSameMenu(MenuModel otherMenu) {
        if (menu == null) return otherMenu == null;
        return otherMenu != null && menu.menuId == otherMenu.menuId;
    }
}
