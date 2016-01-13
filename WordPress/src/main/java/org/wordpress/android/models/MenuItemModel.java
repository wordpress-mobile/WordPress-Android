package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;

import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.StringUtils;

import java.util.List;

public class MenuItemModel {
    // MenuItem table column names
    public static final String ID_COLUMN_NAME = "id";

    public static final String MENU_ITEMS_TABLE_NAME = "menu_items";
    public static final String CREATE_MENU_ITEMS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    MENU_ITEMS_TABLE_NAME +
                    " (" +
                    ID_COLUMN_NAME + " TEXT PRIMARY KEY, " +
                    ");";

    public String contentId;
    public String details;
    public String itemId;
    public String linkTarget;
    public String linkTitle;
    public String name;
    public String type;
    public String typeFamily;
    public String typeLabel;
    public String url;
    public MenuModel menu;
    public MenuItemModel parent;
    public List<MenuItemModel> children;

    public static MenuItemModel fromItemId(String itemId) {
        MenuItemModel model = new MenuItemModel();
        model.itemId = itemId;
        return model;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MenuItemModel)) return false;

        MenuItemModel otherModel = (MenuItemModel) other;
        return StringUtils.equals(name, otherModel.name) &&
                StringUtils.equals(contentId, otherModel.contentId) &&
                StringUtils.equals(itemId, otherModel.itemId) &&
                StringUtils.equals(linkTarget, otherModel.linkTarget) &&
                StringUtils.equals(linkTitle, otherModel.linkTitle) &&
                StringUtils.equals(type, otherModel.type) &&
                StringUtils.equals(typeFamily, otherModel.typeFamily) &&
                StringUtils.equals(typeLabel, otherModel.typeLabel) &&
                StringUtils.equals(url, otherModel.url) &&
                StringUtils.equals(details, otherModel.details) &&
                CollectionUtils.areListsEqual(children, otherModel.children);
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
