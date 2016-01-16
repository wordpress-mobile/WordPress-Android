package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;

import org.wordpress.android.util.CollectionUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.util.DatabaseUtils.*;

/**
 * Describes a Menu Item and provides convenience methods for local database (de)serialization.
 *
 * Menu Items can be of type Page, Link, Post, Category, or Tag and can have any number of child
 * {@link MenuItemModel}'s associated with it.
 */

public class MenuItemModel {
    //
    // Menu Location database table column names
    //
    /** SQL type - INTEGER (PRIMARY KEY) */
    public static final String ID_COLUMN_NAME = "itemId";
    /** SQL type - INTEGER */
    public static final String MENU_ID_COLUMN_NAME = "itemMenu";
    /** SQL type - INTEGER */
    public static final String PARENT_ID_COLUMN_NAME = "itemParent";
    /** SQL type - INTEGER */
    public static final String CONTENT_ID_COLUMN_NAME = "itemContentId";
    /** SQL type - TEXT */
    public static final String URL_COLUMN_NAME = "itemUrl";
    /** SQL type - TEXT */
    public static final String NAME_COLUMN_NAME = "itemName";
    /** SQL type - TEXT */
    public static final String DETAILS_COLUMN_NAME = "itemDetails";
    /** SQL type - TEXT */
    public static final String LINK_TARGET_COLUMN_NAME = "itemLinkTarget";
    /** SQL type - TEXT */
    public static final String LINK_TITLE_COLUMN_NAME = "itemLinkTitle";
    /** SQL type - TEXT */
    public static final String TYPE_COLUMN_NAME = "itemType";
    /** SQL type - TEXT */
    public static final String TYPE_FAMILY_COLUMN_NAME = "itemTypeFamily";
    /** SQL type - TEXT */
    public static final String TYPE_LABEL_COLUMN_NAME = "itemTypeLabel";
    /** SQL type - TEXT */
    public static final String CHILDREN_COLUMN_NAME = "itemChildren";

    //
    // Convenience Strings for SQL database table creation.
    //
    public static final String MENU_ITEMS_TABLE_NAME = "menu_items";
    public static final String CREATE_MENU_ITEMS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    MENU_ITEMS_TABLE_NAME + " (" +
                    ID_COLUMN_NAME + " INTEGER PRIMARY KEY, " +
                    MENU_ID_COLUMN_NAME + " INTEGER, " +
                    PARENT_ID_COLUMN_NAME + " INTEGER, " +
                    CONTENT_ID_COLUMN_NAME + " INTEGER, " +
                    URL_COLUMN_NAME + " TEXT, " +
                    NAME_COLUMN_NAME + " TEXT, " +
                    DETAILS_COLUMN_NAME + " TEXT, " +
                    LINK_TARGET_COLUMN_NAME + " TEXT, " +
                    LINK_TITLE_COLUMN_NAME + " TEXT, " +
                    TYPE_COLUMN_NAME + " TEXT, " +
                    TYPE_FAMILY_COLUMN_NAME + " TEXT, " +
                    TYPE_LABEL_COLUMN_NAME + " TEXT, " +
                    CHILDREN_COLUMN_NAME + " TEXT" +
                    ");";

    public long itemId;
    public long menuId;
    public long parentId;
    public long contentId;
    public String url;
    public String name;
    public String details;
    public String linkTarget;
    public String linkTitle;
    public String type;
    public String typeFamily;
    public String typeLabel;
    public List<Long> children;

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof MenuItemModel)) return false;

        MenuItemModel otherItem = (MenuItemModel) other;
        return itemId == otherItem.itemId &&
                menuId == otherItem.menuId &&
                parentId == otherItem.parentId &&
                contentId == otherItem.contentId &&
                StringUtils.equals(url, otherItem.url) &&
                StringUtils.equals(name, otherItem.name) &&
                StringUtils.equals(details, otherItem.details) &&
                StringUtils.equals(linkTarget, otherItem.linkTarget) &&
                StringUtils.equals(linkTitle, otherItem.linkTitle) &&
                StringUtils.equals(type, otherItem.type) &&
                StringUtils.equals(typeFamily, otherItem.typeFamily) &&
                StringUtils.equals(typeLabel, otherItem.typeLabel) &&
                CollectionUtils.areListsEqual(children, otherItem.children);
    }

    /**
     * Sets the state of this instance to match the state described by the row at the current
     * position of the given {@link Cursor}.
     */
    public void deserializeFromDatabase(Cursor cursor) {
        if (cursor != null && !cursor.isBeforeFirst() && !cursor.isAfterLast()) {
            itemId = getLongFromCursor(cursor, ID_COLUMN_NAME);
            menuId = getLongFromCursor(cursor, MENU_ID_COLUMN_NAME);
            parentId = getLongFromCursor(cursor, PARENT_ID_COLUMN_NAME);
            contentId = getLongFromCursor(cursor, CONTENT_ID_COLUMN_NAME);
            url = getStringFromCursor(cursor, URL_COLUMN_NAME);
            name = getStringFromCursor(cursor, NAME_COLUMN_NAME);
            details = getStringFromCursor(cursor, DETAILS_COLUMN_NAME);
            linkTarget = getStringFromCursor(cursor, LINK_TARGET_COLUMN_NAME);
            linkTitle = getStringFromCursor(cursor, LINK_TITLE_COLUMN_NAME);
            type = getStringFromCursor(cursor, TYPE_COLUMN_NAME);
            typeFamily = getStringFromCursor(cursor, TYPE_FAMILY_COLUMN_NAME);
            typeLabel = getStringFromCursor(cursor, TYPE_LABEL_COLUMN_NAME);
            setChildrenFromStringList(getStringFromCursor(cursor, CHILDREN_COLUMN_NAME));
        }
    }

    /**
     * Creates a {@link ContentValues} object to store in a local database. Passing a {@link Cursor}
     * with these values to {@link MenuItemModel#deserializeFromDatabase(Cursor)} will recreate
     * this instance state.
     */
    public ContentValues serializeToDatabase() {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN_NAME, itemId);
        values.put(MENU_ID_COLUMN_NAME, menuId);
        values.put(PARENT_ID_COLUMN_NAME, parentId);
        values.put(CONTENT_ID_COLUMN_NAME, contentId);
        values.put(URL_COLUMN_NAME, url);
        values.put(NAME_COLUMN_NAME, name);
        values.put(DETAILS_COLUMN_NAME, details);
        values.put(LINK_TARGET_COLUMN_NAME, linkTarget);
        values.put(LINK_TITLE_COLUMN_NAME, linkTitle);
        values.put(TYPE_COLUMN_NAME, type);
        values.put(TYPE_FAMILY_COLUMN_NAME, typeFamily);
        values.put(TYPE_LABEL_COLUMN_NAME, typeLabel);
        values.put(CHILDREN_COLUMN_NAME, separatedStringList(children, ","));
        return values;
    }

    /**
     * Removes any existing children before adding children from given string list.
     *
     * @param childList
     *  comma (',') separated {@link MenuItemModel#itemId}'s to be added as children to this item
     */
    public void setChildrenFromStringList(String childList) {
        if (children != null) children.clear();
        addChildrenFromStringList(childList);
    }

    /**
     * Adds children from given string list, maintaining existing children.
     *
     * @param childList
     *  comma (',') separated {@link MenuItemModel#itemId}'s to be added as children to this item
     */
    public void addChildrenFromStringList(String childList) {
        if (children == null) children = new ArrayList<>();
        CollectionUtils.addLongsFromStringListToArrayList(children, childList);
    }
}
