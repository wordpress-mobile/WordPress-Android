package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;

import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.util.DatabaseUtils.*;

/**
 * Describes a Menu and provides convenience methods for local database (de)serialization.
 *
 * Menus contain a list of {@link MenuItemModel} ID's and a list of {@link MenuLocationModel} ID's,
 * the former being ordered.
 */

public class MenuModel {
    //
    // Menu database table column names
    //
    /** SQL type - INTEGER (PRIMARY KEY) */
    public static final String ID_COLUMN_NAME = "menuId";
    /** SQL type - TEXT */
    public static final String NAME_COLUMN_NAME = "menuName";
    /** SQL type - TEXT */
    public static final String DETAILS_COLUMN_NAME = "menuDetails";
    /** SQL type - TEXT */
    public static final String LOCATIONS_COLUMN_NAME = "menuLocations";
    /** SQL type - TEXT */
    public static final String ITEMS_COLUMN_NAME = "menuItems";

    //
    // Convenience Strings for SQL database table creation.
    //
    public static final String MENUS_TABLE_NAME = "menus";
    public static final String CREATE_MENUS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    MENUS_TABLE_NAME + " (" +
                    ID_COLUMN_NAME + " INTEGER PRIMARY KEY, " +
                    NAME_COLUMN_NAME + " TEXT, " +
                    DETAILS_COLUMN_NAME + " TEXT, " +
                    LOCATIONS_COLUMN_NAME + " TEXT, " +
                    ITEMS_COLUMN_NAME + " TEXT" +
                    ");";

    public long menuId;
    public String name;
    public String details;
    public List<Long> locations;
    public List<Long> menuItems;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MenuModel)) return false;

        MenuModel otherModel = (MenuModel) other;
        return menuId == otherModel.menuId &&
                StringUtils.equals(name, otherModel.name) &&
                StringUtils.equals(details, otherModel.details) &&
                CollectionUtils.areListsEqual(locations, otherModel.locations) &&
                CollectionUtils.areListsEqual(menuItems, otherModel.menuItems);
    }

    /**
     * Sets values from a local database {@link Cursor}.
     */
    public MenuModel deserializeFromDatabase(Cursor cursor) {
        if (cursor != null && !cursor.isBeforeFirst() && !cursor.isAfterLast()) {
            menuId = getLongFromCursor(cursor, ID_COLUMN_NAME);
            name = getStringFromCursor(cursor, NAME_COLUMN_NAME);
            details = getStringFromCursor(cursor, DETAILS_COLUMN_NAME);
            locations = deserializeLocations(cursor);
            menuItems = deserializeItems(cursor);
        }

        return this;
    }

    public List<Long> deserializeLocations(Cursor cursor) {
        String locationNames = getStringFromCursor(cursor, LOCATIONS_COLUMN_NAME);
        List<Long> locations = new ArrayList<>();
        for (String name : locationNames.split(",")) {
            locations.add(Long.valueOf(name));
        }
        return locations;
    }

    public List<Long> deserializeItems(Cursor cursor) {
        String itemIds = getStringFromCursor(cursor, ITEMS_COLUMN_NAME);
        List<Long> items = new ArrayList<>();
        for (String id : itemIds.split(",")) {
            items.add(Long.valueOf(id));
        }
        return items;
    }

    /**
     * Creates a {@link ContentValues} object to store this object in a local database.
     */
    public ContentValues serializeToDatabase() {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN_NAME, menuId);
        values.put(NAME_COLUMN_NAME, name);
        values.put(DETAILS_COLUMN_NAME, details);
        values.put(LOCATIONS_COLUMN_NAME, separatedStringList(locations, ","));
        values.put(ITEMS_COLUMN_NAME, separatedStringList(menuItems, ","));
        return values;
    }

    /**
     * Removes any existing children before adding children from given string list.
     *
     * @param locationList
     *  comma (',') separated {@link MenuItemModel#itemId}'s to be added as children to this item
     */
    public void setLocationsFromStringList(String locationList) {
        if (locations != null) locations.clear();
        addLocationFromStringList(locationList);
    }

    /**
     * Adds children from given string list, maintaining existing children.
     *
     * @param locationList
     *  comma (',') separated {@link MenuItemModel#itemId}'s to be added as children to this item
     */
    public void addLocationFromStringList(String locationList) {
        if (locations == null) locations = new ArrayList<>();
        CollectionUtils.addLongsFromStringListToArrayList(locations, locationList);
    }

    /**
     * Removes any existing children before adding children from given string list.
     *
     * @param itemList
     *  comma (',') separated {@link MenuItemModel#itemId}'s to be added as children to this item
     */
    public void setItemsFromStringList(String itemList) {
        if (menuItems != null) menuItems.clear();
        addItemsFromStringList(itemList);
    }

    /**
     * Adds children from given string list, maintaining existing children.
     *
     * @param itemList
     *  comma (',') separated {@link MenuItemModel#itemId}'s to be added as children to this item
     */
    public void addItemsFromStringList(String itemList) {
        if (menuItems == null) menuItems = new ArrayList<>();
        CollectionUtils.addLongsFromStringListToArrayList(menuItems, itemList);
    }
}
