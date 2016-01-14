package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;

import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.util.DatabaseUtils.*;

public class MenuItemModel {
    // MenuItem table column names
    public static final String ID_COLUMN_NAME = "itemId";
    public static final String CONTENT_ID_COLUMN_NAME = "itemContentId";
    public static final String URL_COLUMN_NAME = "itemUrl";
    public static final String NAME_COLUMN_NAME = "itemName";
    public static final String DETAILS_COLUMN_NAME = "itemDetails";
    public static final String LINK_TARGET_COLUMN_NAME = "itemLinkTarget";
    public static final String LINK_TITLE_COLUMN_NAME = "itemLinkTitle";
    public static final String TYPE_COLUMN_NAME = "itemType";
    public static final String TYPE_FAMILY_COLUMN_NAME = "itemTypeFamily";
    public static final String TYPE_LABEL_COLUMN_NAME = "itemTypeLabel";
    public static final String MENU_COLUMN_NAME = "itemMenu";
    public static final String PARENT_COLUMN_NAME = "itemParent";
    public static final String CHILDREN_COLUMN_NAME = "itemChildren";

    public static final String MENU_ITEMS_TABLE_NAME = "menu_items";
    public static final String CREATE_MENU_ITEMS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    MENU_ITEMS_TABLE_NAME +
                    " (" +
                    ID_COLUMN_NAME + " INTEGER PRIMARY KEY, " +
                    CONTENT_ID_COLUMN_NAME + " TEXT, " +
                    URL_COLUMN_NAME + " TEXT, " +
                    NAME_COLUMN_NAME + " TEXT, " +
                    DETAILS_COLUMN_NAME + " TEXT, " +
                    LINK_TARGET_COLUMN_NAME + " TEXT, " +
                    LINK_TITLE_COLUMN_NAME + " TEXT, " +
                    TYPE_COLUMN_NAME + " TEXT, " +
                    TYPE_FAMILY_COLUMN_NAME + " TEXT, " +
                    TYPE_LABEL_COLUMN_NAME + " TEXT, " +
                    MENU_COLUMN_NAME + " INTEGER, " +
                    PARENT_COLUMN_NAME + " INTEGER, " +
                    CHILDREN_COLUMN_NAME + " TEXT" +
                    ");";

    public long itemId;
    public String contentId;
    public String url;
    public String name;
    public String details;
    public String linkTarget;
    public String linkTitle;
    public String type;
    public String typeFamily;
    public String typeLabel;
    public MenuModel menu;
    public MenuItemModel parent;
    public List<MenuItemModel> children;

    public static MenuItemModel fromItemId(long itemId) {
        MenuItemModel model = new MenuItemModel();
        model.itemId = itemId;
        return model;
    }

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MenuItemModel)) return false;

        MenuItemModel otherModel = (MenuItemModel) other;
        return itemId == otherModel.itemId &&
                StringUtils.equals(contentId, otherModel.contentId) &&
                StringUtils.equals(url, otherModel.url) &&
                StringUtils.equals(name, otherModel.name) &&
                StringUtils.equals(details, otherModel.details) &&
                StringUtils.equals(linkTarget, otherModel.linkTarget) &&
                StringUtils.equals(linkTitle, otherModel.linkTitle) &&
                StringUtils.equals(type, otherModel.type) &&
                StringUtils.equals(typeFamily, otherModel.typeFamily) &&
                StringUtils.equals(typeLabel, otherModel.typeLabel) &&
                hasSameMenu(otherModel.menu) &&
                hasSameParent(otherModel.parent) &&
                CollectionUtils.areListsEqual(children, otherModel.children);
    }

    public void deserializeFromDatabase(Cursor cursor) {
        if (cursor != null && cursor.getCount() != 0 && cursor.moveToFirst()) {
            itemId = getLongFromCursor(cursor, ID_COLUMN_NAME);
            contentId = getStringFromCursor(cursor, CONTENT_ID_COLUMN_NAME);
            url = getStringFromCursor(cursor, URL_COLUMN_NAME);
            name = getStringFromCursor(cursor, NAME_COLUMN_NAME);
            details = getStringFromCursor(cursor, DETAILS_COLUMN_NAME);
            linkTarget = getStringFromCursor(cursor, LINK_TARGET_COLUMN_NAME);
            linkTitle = getStringFromCursor(cursor, LINK_TITLE_COLUMN_NAME);
            type = getStringFromCursor(cursor, TYPE_COLUMN_NAME);
            typeFamily = getStringFromCursor(cursor, TYPE_FAMILY_COLUMN_NAME);
            typeLabel = getStringFromCursor(cursor, TYPE_LABEL_COLUMN_NAME);
            (menu = new MenuModel()).menuId = getLongFromCursor(cursor, MENU_COLUMN_NAME);
            (parent = new MenuItemModel()).itemId = getLongFromCursor(cursor, PARENT_COLUMN_NAME);
            children = deserializeChildren(getStringFromCursor(cursor, CHILDREN_COLUMN_NAME));
        }
    }

    /**
     * Creates a {@link ContentValues} object to store this object in a local database.
     */
    public ContentValues serializeToDatabase() {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN_NAME, itemId);
        values.put(CONTENT_ID_COLUMN_NAME, contentId);
        values.put(URL_COLUMN_NAME, url);
        values.put(NAME_COLUMN_NAME, name);
        values.put(DETAILS_COLUMN_NAME, details);
        values.put(LINK_TARGET_COLUMN_NAME, linkTarget);
        values.put(LINK_TITLE_COLUMN_NAME, linkTitle);
        values.put(TYPE_COLUMN_NAME, type);
        values.put(TYPE_FAMILY_COLUMN_NAME, typeFamily);
        values.put(TYPE_LABEL_COLUMN_NAME, typeLabel);
        values.put(MENU_COLUMN_NAME, menu != null ? menu.menuId : -1);
        values.put(PARENT_COLUMN_NAME, parent != null ? parent.itemId : -1);
        values.put(CHILDREN_COLUMN_NAME, serializeChildren());
        return values;
    }

    public List<MenuItemModel> deserializeChildren(String childList) {
        List<MenuItemModel> childrenModels = new ArrayList<>();
        for (String child : childList.split(",")) {
            MenuItemModel childModel = new MenuItemModel();
            childModel.itemId = Long.valueOf(child);
            childrenModels.add(childModel);
        }
        return childrenModels;
    }

    public String serializeChildren() {
        StringBuilder builder = new StringBuilder();
        for (MenuItemModel item : children) {
            builder.append(item.itemId);
            builder.append(",");
        }
        return builder.substring(0, builder.length() - 1);
    }

    private boolean hasSameParent(MenuItemModel otherParent) {
        if (menu == null) return otherParent == null;
        return otherParent != null && parent.itemId == otherParent.itemId;
    }

    private boolean hasSameMenu(MenuModel otherMenu) {
        if (menu == null) return otherMenu == null;
        return otherMenu != null && menu.menuId == otherMenu.menuId;
    }
}
