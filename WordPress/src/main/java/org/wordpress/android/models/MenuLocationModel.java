package org.wordpress.android.models;


import android.content.ContentValues;
import android.database.Cursor;

import org.wordpress.android.util.StringUtils;

import static org.wordpress.android.util.DatabaseUtils.*;

/**
 * Describes a Menu Location and provides convenience methods for local database (de)serialization.
 *
 * Menu Locations are designated on a per-Theme basis. Each Menu Location has an id, name,
 * description, default state, and associated Menu.
 */

public class MenuLocationModel {
    //
    // Menu Location database table column names
    //
    /** SQL type - INTEGER (PRIMARY KEY) */
    public static final String ID_COLUMN_NAME = "itemId";
    /** SQL type - TEXT */
    public static final String NAME_COLUMN_NAME = "locationName";
    /** SQL type - TEXT */
    public static final String DETAILS_COLUMN_NAME = "locationDetails";
    /** SQL type - TEXT */
    public static final String DEFAULT_STATE_COLUMN_NAME = "locationDefaultState";
    /** SQL type - INTEGER */
    public static final String MENU_COLUMN_NAME = "locationMenu";

    //
    // Convenience Strings for SQL database table creation.
    //
    public static final String MENU_LOCATIONS_TABLE_NAME = "menu_locations";
    public static final String CREATE_MENU_LOCATIONS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    MENU_LOCATIONS_TABLE_NAME + " (" +
                    ID_COLUMN_NAME + " INTEGER PRIMARY KEY, " +
                    NAME_COLUMN_NAME + " TEXT, " +
                    DETAILS_COLUMN_NAME + " TEXT, " +
                    DEFAULT_STATE_COLUMN_NAME + " TEXT, " +
                    MENU_COLUMN_NAME + " INTEGER" +
                    ");";

    public long locationId;
    public String name;
    public String details;
    public String defaultState;
    public long menuId;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MenuLocationModel)) return false;
        MenuLocationModel otherLocation = (MenuLocationModel) other;

        return locationId == otherLocation.locationId &&
                StringUtils.equals(name, otherLocation.name) &&
                StringUtils.equals(details, otherLocation.details) &&
                StringUtils.equals(defaultState, otherLocation.defaultState) &&
                menuId == otherLocation.menuId;
    }

    /**
     * Sets the state of this instance to match the state described by the row at the current
     * position of the given {@link Cursor}.
     */
    public void deserializeFromDatabase(Cursor cursor) {
        if (cursor != null && !cursor.isBeforeFirst() && !cursor.isAfterLast()) {
            locationId = getLongFromCursor(cursor, ID_COLUMN_NAME);
            name = getStringFromCursor(cursor, NAME_COLUMN_NAME);
            details = getStringFromCursor(cursor, DETAILS_COLUMN_NAME);
            defaultState = getStringFromCursor(cursor, DEFAULT_STATE_COLUMN_NAME);
            menuId = getLongFromCursor(cursor, MENU_COLUMN_NAME);
        }
    }

    /**
     * Creates a {@link ContentValues} object to store in a local database. Passing a {@link Cursor}
     * with these values to {@link MenuLocationModel#deserializeFromDatabase(Cursor)} will recreate
     * this instance state.
     */
    public ContentValues serializeToDatabase() {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN_NAME, locationId);
        values.put(NAME_COLUMN_NAME, name);
        values.put(DETAILS_COLUMN_NAME, details);
        values.put(DEFAULT_STATE_COLUMN_NAME, defaultState);
        values.put(MENU_COLUMN_NAME, menuId);
        return values;
    }
}
