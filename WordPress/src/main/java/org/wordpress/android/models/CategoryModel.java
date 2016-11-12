package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;

/**
 * Represents WordPress post Category data and handles local database (de)serialization.
 */
public class CategoryModel {
    // Categories table column names
    public static final String ID_COLUMN_NAME = "ID";
    public static final String NAME_COLUMN_NAME = "name";
    public static final String SLUG_COLUMN_NAME = "slug";
    public static final String DESC_COLUMN_NAME = "description";
    public static final String PARENT_ID_COLUMN_NAME = "parent";
    public static final String POST_COUNT_COLUMN_NAME = "post_count";

    public int id;
    public String name;
    public String slug;
    public String description;
    public int parentId;
    public int postCount;
    public boolean isInLocalTable;

    public CategoryModel() {
        id = -1;
        name = "";
        slug = "";
        description = "";
        parentId = -1;
        postCount = 0;
        isInLocalTable = false;
    }

    /**
     * Sets data from a local database {@link Cursor}.
     */
    public void deserializeFromDatabase(Cursor cursor) {
        if (cursor == null) return;

        id = cursor.getInt(cursor.getColumnIndex(ID_COLUMN_NAME));
        name = cursor.getString(cursor.getColumnIndex(NAME_COLUMN_NAME));
        slug = cursor.getString(cursor.getColumnIndex(SLUG_COLUMN_NAME));
        description = cursor.getString(cursor.getColumnIndex(DESC_COLUMN_NAME));
        parentId = cursor.getInt(cursor.getColumnIndex(PARENT_ID_COLUMN_NAME));
        postCount = cursor.getInt(cursor.getColumnIndex(POST_COUNT_COLUMN_NAME));
        isInLocalTable = true;
    }

    /**
     * Creates the {@link ContentValues} object to store this category data in a local database.
     */
    public ContentValues serializeToDatabase() {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN_NAME, id);
        values.put(NAME_COLUMN_NAME, name);
        values.put(SLUG_COLUMN_NAME, slug);
        values.put(DESC_COLUMN_NAME, description);
        values.put(PARENT_ID_COLUMN_NAME, parentId);
        values.put(POST_COUNT_COLUMN_NAME, postCount);

        return values;
    }
}
