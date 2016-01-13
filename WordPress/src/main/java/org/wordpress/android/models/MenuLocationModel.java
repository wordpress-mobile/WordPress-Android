package org.wordpress.android.models;


import android.content.ContentValues;
import android.database.Cursor;

public class MenuLocationModel {
    // MenuLocation table column names
    public static final String ID_COLUMN_NAME = "id";

    public static final String MENU_LOCATIONS_TABLE_NAME = "menu_items";
    public static final String CREATE_MENU_LOCATIONS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    MENU_LOCATIONS_TABLE_NAME +
                    " (" +
                    ID_COLUMN_NAME + " TEXT PRIMARY KEY, " +
                    ");";

    public String defaultState;
    public String details;
    public String name;
    public MenuModel menu;

    public static MenuLocationModel fromName(String name) {
        MenuLocationModel model = new MenuLocationModel();
        model.name = name;
        return model;
    }

    public void deserializeFromDatabaseCursor(Cursor cursor) {
    }

    /**
     * Creates a {@link ContentValues} object to store this object in a local database.
     */
    public ContentValues serializeToDatabase() {
        ContentValues values = new ContentValues();
        return values;
    }
}
