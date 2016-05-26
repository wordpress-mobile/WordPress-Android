package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuLocationModel;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.util.SqlUtils.*;

/**
 * Provides methods to interface with a local SQL database for Menu Location related queries.
 */
public class MenuLocationTable {
    //
    // Menu Location database table column names
    //
    public static final String SITE_ID_COLUMN = "siteId";
    public static final String NAME_COLUMN = "locationName";
    public static final String DETAILS_COLUMN = "locationDetails";
    public static final String DEFAULT_STATE_COLUMN = "locationDefaultState";
    public static final String PRIMARY_KEY_COLUMN = "pkLocation";

    //
    // Convenience SQL strings
    //
    public static final String MENU_LOCATIONS_TABLE_NAME = "menu_locations";

    /** SQL PRIMARY KEY constraint */
    public static final String PRIMARY_KEY = "CONSTRAINT " + PRIMARY_KEY_COLUMN + " PRIMARY KEY";

    public static final String CREATE_MENU_LOCATION_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " + MENU_LOCATIONS_TABLE_NAME + " (" +
                    SITE_ID_COLUMN + " INTEGER NOT NULL," +
                    NAME_COLUMN + " TEXT NOT NULL, " +
                    DETAILS_COLUMN + " TEXT, " +
                    DEFAULT_STATE_COLUMN + " TEXT, " +
                    PRIMARY_KEY + " (" + SITE_ID_COLUMN + "," + NAME_COLUMN + ")" +
                    ");";

    /** Well-formed WHERE clause for identifying a row using PRIMARY KEY constraints */
    public static final String UNIQUE_WHERE_SQL =
            "WHERE (" + SITE_ID_COLUMN + "=?) AND (" + NAME_COLUMN + "=?)";

    /** Well-formed SELECT query for selecting a row using PRIMARY KEY constraints */
    public static final String SELECT_UNIQUE_LOCATION_SQL =
            "SELECT * FROM " + MENU_LOCATIONS_TABLE_NAME +
                    " " + UNIQUE_WHERE_SQL + ";";

    /** Well-formed SELECT query for selecting rows for a given site ID */
    public static final String SELECT_SITE_LOCATIONS_SQL =
            "SELECT * FROM " + MENU_LOCATIONS_TABLE_NAME + " WHERE " + SITE_ID_COLUMN + "=?;";

    public static boolean saveMenuLocation(MenuLocationModel location) {
        if (location == null || location.siteId < 0) return false;

        ContentValues values = serializeToDatabase(location);
        return WordPress.wpDB.getDatabase().insertWithOnConflict(
                MENU_LOCATIONS_TABLE_NAME, null, values, SQLiteDatabase.CONFLICT_REPLACE) != -1;
    }

    public static void deleteMenuLocationForCurrentSite(String locationName) {
        deleteMenuLocationForSite(WordPress.currentBlog.getRemoteBlogId(), locationName);
    }

    public static void deleteMenuLocationForSite(long siteId, String locationName) {
        if (siteId < 0 || TextUtils.isEmpty(locationName)) return;
        String[] args = {String.valueOf(siteId), locationName};
        WordPress.wpDB.getDatabase().delete(MENU_LOCATIONS_TABLE_NAME, UNIQUE_WHERE_SQL, args);
    }

    public static void deleteAllLocationsForCurrentSite() {
        deleteAllLocationsForSite(WordPress.currentBlog.getRemoteBlogId());
    }

    public static void deleteAllLocationsForSite(long siteId) {
        if (siteId < 0) return;
        String params = SITE_ID_COLUMN + "=?";
        String[] args = {String.valueOf(siteId)};
        WordPress.wpDB.getDatabase().delete(MENU_LOCATIONS_TABLE_NAME, params, args);
    }

    public static void deleteAllLocations() {
        WordPress.wpDB.getDatabase().delete(MENU_LOCATIONS_TABLE_NAME, null, null);
    }

    /**
     * Passthrough to {@link #getMenuLocation(long, String)} using the remote ID of the current
     * site at {@link WordPress#currentBlog}.
     */
    public static MenuLocationModel getMenuLocationForCurrentSite(String locationName) {
        return getMenuLocation(WordPress.currentBlog.getRemoteBlogId(), locationName);
    }

    public static MenuLocationModel getMenuLocation(long siteId, String locationName) {
        if (siteId < 0 || TextUtils.isEmpty(locationName)) return null;

        String[] args = {String.valueOf(siteId), locationName};
        Cursor cursor = WordPress.wpDB.getDatabase().rawQuery(SELECT_UNIQUE_LOCATION_SQL, args);
        cursor.moveToFirst();
        MenuLocationModel location = deserializeFromDatabase(cursor);
        cursor.close();

        if (location != null) {
            // TODO: find Menu ID for the location
        }

        return location;
    }

    public static List<MenuLocationModel> getAllMenuLocationsForCurrentSite() {
        return getAllMenuLocationsForSite(WordPress.currentBlog.getRemoteBlogId());
    }

    public static List<MenuLocationModel> getAllMenuLocationsForSite(long siteId) {
        if (siteId < 0) return null;

        List<MenuLocationModel> siteLocations = new ArrayList<>();
        String[] args = {String.valueOf(siteId)};
        Cursor cursor = WordPress.wpDB.getDatabase().rawQuery(SELECT_SITE_LOCATIONS_SQL, args);
        if (cursor.moveToFirst()) {
            do {
                MenuLocationModel location = deserializeFromDatabase(cursor);
                if (location != null) siteLocations.add(location);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return siteLocations;
    }

    public static List<MenuLocationModel> getAllMenuLocations() {
        List<MenuLocationModel> siteLocations = new ArrayList<>();
        String sqlQuery = "SELECT * FROM " + MENU_LOCATIONS_TABLE_NAME + ";";
        Cursor cursor = WordPress.wpDB.getDatabase().rawQuery(sqlQuery, null);
        if (cursor.moveToFirst()) {
            do {
                MenuLocationModel location = deserializeFromDatabase(cursor);
                if (location != null) siteLocations.add(location);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return siteLocations;
    }

    public static MenuLocationModel deserializeFromDatabase(Cursor cursor) {
        if (cursor == null || cursor.isBeforeFirst() || cursor.isAfterLast()) return null;

        MenuLocationModel location = new MenuLocationModel();
        location.name = getStringFromCursor(cursor, NAME_COLUMN);
        location.details = getStringFromCursor(cursor, DETAILS_COLUMN);
        location.defaultState = getStringFromCursor(cursor, DEFAULT_STATE_COLUMN);

        return location;
    }

    public static ContentValues serializeToDatabase(MenuLocationModel location) {
        ContentValues values = new ContentValues();
        values.put(SITE_ID_COLUMN, location.siteId);
        values.put(NAME_COLUMN, location.name);
        values.put(DETAILS_COLUMN, location.details);
        values.put(DEFAULT_STATE_COLUMN, location.defaultState);
        return values;
    }
}
