package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;

import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.util.DatabaseUtils.*;

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
    public List<MenuLocationModel> locations;
    public List<MenuItemModel> menuItems;

    public static MenuModel deserializeFromDatabase(Cursor cursor) {
        MenuModel model = new MenuModel();
        return model.deserializeDatabaseCursor(cursor);
    }

    public MenuModel() {
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MenuModel)) return false;

        MenuModel otherModel = (MenuModel) other;
        return StringUtils.equals(details, otherModel.details) &&
                StringUtils.equals(menuId, otherModel.menuId) &&
                StringUtils.equals(name, otherModel.name) &&
                CollectionUtils.areListsEqual(locations, otherModel.locations) &&
                CollectionUtils.areListsEqual(menuItems, otherModel.menuItems);
    }

    /**
     * Sets values from a local database {@link Cursor}.
     */
    public MenuModel deserializeDatabaseCursor(Cursor cursor) {
        if (cursor != null && cursor.getCount() != 0 && cursor.moveToFirst()) {
            menuId = getStringFromCursor(cursor, ID_COLUMN_NAME);
            name = getStringFromCursor(cursor, NAME_COLUMN_NAME);
            details = getStringFromCursor(cursor, DETAILS_COLUMN_NAME);
            locations = deserializeLocations(cursor);
            menuItems = deserializeItems(cursor);
        }

        return this;
    }

    public List<MenuLocationModel> deserializeLocations(Cursor cursor) {
        String locationNames = getStringFromCursor(cursor, LOCATIONS_COLUMN_NAME);
        List<MenuLocationModel> locations = new ArrayList<>();
        for (String name : locationNames.split(",")) {
            locations.add(MenuLocationModel.fromName(name));
        }
        return locations;
    }

    public List<MenuItemModel> deserializeItems(Cursor cursor) {
        String itemIds = getStringFromCursor(cursor, ITEMS_COLUMN_NAME);
        List<MenuItemModel> items = new ArrayList<>();
        for (String id : itemIds.split(",")) {
            items.add(MenuItemModel.fromItemId(id));
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
