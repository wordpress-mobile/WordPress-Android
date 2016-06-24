package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.util.AppLog;

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

    // WP.com REST keys used in response to a categories GET request
    private static final String CAT_ID_KEY = "ID";
    private static final String CAT_NAME_KEY = "name";
    private static final String CAT_SLUG_KEY = "slug";
    private static final String CAT_DESC_KEY = "description";
    private static final String CAT_PARENT_ID_KEY = "parent";
    private static final String CAT_POST_COUNT_KEY = "post_count";
    private static final String CAT_NUM_POSTS_KEY = "found";
    private static final String CATEGORIES_KEY = "categories";

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

    public static CategoryModel deserializeCategoryFromJson(JSONObject category) throws JSONException {
        if (category == null) return null;

        CategoryModel model = new CategoryModel();
        model.id = category.getInt(CAT_ID_KEY);
        model.name = category.getString(CAT_NAME_KEY);
        model.slug = category.getString(CAT_SLUG_KEY);
        model.description = category.getString(CAT_DESC_KEY);
        model.parentId = category.getInt(CAT_PARENT_ID_KEY);
        model.postCount = category.getInt(CAT_POST_COUNT_KEY);

        return model;
    }

    public static CategoryModel[] deserializeJsonRestResponse(JSONObject response) {
        try {
            int num = response.getInt(CAT_NUM_POSTS_KEY);
            JSONArray categories = response.getJSONArray(CATEGORIES_KEY);
            CategoryModel[] models = new CategoryModel[num];

            for (int i = 0; i < num; ++i) {
                JSONObject category = categories.getJSONObject(i);
                models[i] = deserializeCategoryFromJson(category);
            }

            AppLog.d(AppLog.T.API, "Successfully fetched WP.com categories");

            return models;
        } catch (JSONException exception) {
            AppLog.d(AppLog.T.API, "Error parsing WP.com categories response:" + response);
            return null;
        }
    }

}
