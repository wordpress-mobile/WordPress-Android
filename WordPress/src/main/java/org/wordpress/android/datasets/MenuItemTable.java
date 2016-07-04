package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.MenuItemModel;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.util.SqlUtils.*;

public class MenuItemTable {
    //
    // Menu Item database table column names
    //
    public static final String ID_COLUMN = "itemId";
    public static final String MENU_ID_COLUMN = "itemMenu";
    public static final String PARENT_ID_COLUMN = "itemParent";
    public static final String CONTENT_ID_COLUMN = "itemContentId";
    public static final String URL_COLUMN = "itemUrl";
    public static final String NAME_COLUMN = "itemName";
    public static final String DETAILS_COLUMN = "itemDetails";
    public static final String LINK_TARGET_COLUMN = "itemLinkTarget";
    public static final String LINK_TITLE_COLUMN = "itemLinkTitle";
    public static final String TYPE_COLUMN = "itemType";
    public static final String TYPE_FAMILY_COLUMN = "itemTypeFamily";
    public static final String TYPE_LABEL_COLUMN = "itemTypeLabel";
    public static final String CHILDREN_COLUMN = "itemChildren";

    //
    // Convenience SQL Strings
    //
    public static final String MENU_ITEMS_TABLE_NAME = "menu_items";

    public static final String CREATE_MENU_ITEM_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    MENU_ITEMS_TABLE_NAME + " (" +
                    ID_COLUMN + " INTEGER PRIMARY KEY, " +
                    MENU_ID_COLUMN + " INTEGER, " +
                    PARENT_ID_COLUMN + " INTEGER, " +
                    CONTENT_ID_COLUMN + " INTEGER, " +
                    URL_COLUMN + " TEXT, " +
                    NAME_COLUMN + " TEXT, " +
                    DETAILS_COLUMN + " TEXT, " +
                    LINK_TARGET_COLUMN + " TEXT, " +
                    LINK_TITLE_COLUMN + " TEXT, " +
                    TYPE_COLUMN + " TEXT, " +
                    TYPE_FAMILY_COLUMN + " TEXT, " +
                    TYPE_LABEL_COLUMN + " TEXT, " +
                    CHILDREN_COLUMN + " TEXT" +
                    ");";

    /** Well-formed WHERE clause for identifying a row using PRIMARY KEY constraints */
    public static final String UNIQUE_WHERE_SQL = ID_COLUMN + "=?";

    public static final String UNIQUE_WHERE_SQL_MENU_ID = MENU_ID_COLUMN + "=?";

    public static void saveMenuItem(MenuItemModel item) {
        if (item == null || item.itemId < 0) return;

        ContentValues row = serializeToDatabase(item);
        WordPress.wpDB.getDatabase().insertWithOnConflict(
                MENU_ITEMS_TABLE_NAME, null, row, SQLiteDatabase.CONFLICT_REPLACE);

        if (item.hasChildren()) {
            for (MenuItemModel child : item.children) {
                saveMenuItem(child);
            }
        }
    }

    public static int deleteMenuItemForMenuId(long menuId) {
        if (menuId < 0) return 0;
        String[] args = {String.valueOf(menuId)};
        return WordPress.wpDB.getDatabase().delete(MENU_ITEMS_TABLE_NAME, UNIQUE_WHERE_SQL_MENU_ID, args);
    }

    public static void deleteMenuItem(long itemId) {
        if (itemId < 0) return;
        String[] args = {String.valueOf(itemId)};
        WordPress.wpDB.getDatabase().delete(MENU_ITEMS_TABLE_NAME, UNIQUE_WHERE_SQL, args);
    }

    public static void deleteAllMenuItems() {
        WordPress.wpDB.getDatabase().delete(MENU_ITEMS_TABLE_NAME, null, null);
    }

    public static MenuItemModel getMenuItem(long itemId) {
        if (itemId < 0) return null;

        String[] args = {String.valueOf(itemId)};
        Cursor cursor = WordPress.wpDB.getDatabase().rawQuery("SELECT * FROM " + MENU_ITEMS_TABLE_NAME + " WHERE " + UNIQUE_WHERE_SQL + ";", args);
        cursor.moveToFirst();
        MenuItemModel item = deserializeFromDatabase(cursor);
        cursor.close();
        return item;
    }

    public static List<MenuItemModel> getMenuItemsForMenu(long menuId) {
        List<MenuItemModel> items = new ArrayList<>();
        String[] args = {String.valueOf(menuId)};
        Cursor cursor = WordPress.wpDB.getDatabase().rawQuery("SELECT * FROM " + MENU_ITEMS_TABLE_NAME + " WHERE " + UNIQUE_WHERE_SQL_MENU_ID + ";", args);
        if (cursor.moveToFirst()) {
            do {
                MenuItemModel item = deserializeFromDatabase(cursor);
                if (item != null) items.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();

        return items;
    }

    public static MenuItemModel deserializeFromDatabase(Cursor cursor) {
        if (cursor == null || cursor.isBeforeFirst() || cursor.isAfterLast()) return null;

        MenuItemModel item = new MenuItemModel();
        item.itemId = getLongFromCursor(cursor, ID_COLUMN);
        item.menuId = getLongFromCursor(cursor, MENU_ID_COLUMN);
        item.parentId = getLongFromCursor(cursor, PARENT_ID_COLUMN);
        item.contentId = getLongFromCursor(cursor, CONTENT_ID_COLUMN);
        item.url = getStringFromCursor(cursor, URL_COLUMN);
        item.name = getStringFromCursor(cursor, NAME_COLUMN);
        item.details = getStringFromCursor(cursor, DETAILS_COLUMN);
        item.linkTarget = getStringFromCursor(cursor, LINK_TARGET_COLUMN);
        item.linkTitle = getStringFromCursor(cursor, LINK_TITLE_COLUMN);
        item.type = getStringFromCursor(cursor, TYPE_COLUMN);
        item.typeFamily = getStringFromCursor(cursor, TYPE_FAMILY_COLUMN);
        item.typeLabel = getStringFromCursor(cursor, TYPE_LABEL_COLUMN);
        String children = getStringFromCursor(cursor, CHILDREN_COLUMN);
        if (!TextUtils.isEmpty(children)) {
            item.children = new ArrayList<>();
            for (String childId : children.split(",")) {
                MenuItemModel child = MenuItemTable.getMenuItem(Long.valueOf(childId));
                if (child == null) continue;
                item.children.add(child);
            }
        }
        return item;
    }

    public static ContentValues serializeToDatabase(MenuItemModel item) {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN, item.itemId);
        values.put(MENU_ID_COLUMN, item.menuId);
        values.put(PARENT_ID_COLUMN, item.parentId);
        values.put(CONTENT_ID_COLUMN, item.contentId);
        values.put(URL_COLUMN, item.url);
        values.put(NAME_COLUMN, item.name);
        values.put(DETAILS_COLUMN, item.details);
        values.put(LINK_TARGET_COLUMN, item.linkTarget);
        values.put(LINK_TITLE_COLUMN, item.linkTitle);
        values.put(TYPE_COLUMN, item.type);
        values.put(TYPE_FAMILY_COLUMN, item.typeFamily);
        values.put(TYPE_LABEL_COLUMN, item.typeLabel);
        if (item.hasChildren()) {
            String childIds = "";
            for (MenuItemModel child : item.children) {
                childIds += child.itemId + ",";
            }
            values.put(CHILDREN_COLUMN, childIds.substring(0, childIds.length() - 1));
        }
        return values;
    }
}
