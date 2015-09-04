package org.wordpress.android.models;

import android.database.Cursor;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.util.AppLog;

public class CategoryModel {
    public static final String NUM_POSTS_WPCOM_REST_KEY = "found";
    public static final String CATEGORIES_WPCOM_REST_KEY = "categories";
    public static final String ID_WPCOM_REST_KEY = "ID";
    public static final String NAME_WPCOM_REST_KEY = "name";
    public static final String SLUG_WPCOM_REST_KEY = "slug";
    public static final String DESC_WPCOM_REST_KEY = "description";
    public static final String PARENT_ID_WPCOM_REST_KEY = "parent";
    public static final String POST_COUNT_WPCOM_REST_KEY = "post_count";

    public int id;
    public String name;
    public String slug;
    public String description;
    public int parentId;
    public int postCount;
    public boolean isInLocalTable;

    public static CategoryModel[] deserializeFromDotComRestResponse(JSONObject response) {
        try {
            int num = response.getInt(NUM_POSTS_WPCOM_REST_KEY);
            JSONArray categories = response.getJSONArray(CATEGORIES_WPCOM_REST_KEY);
            CategoryModel[] models = new CategoryModel[num];

            for (int i = 0; i < num; ++i) {
                JSONObject category = categories.getJSONObject(i);
                models[i] = new CategoryModel();
                models[i].deserializeDotComRestResponse(category);
            }

            AppLog.d(AppLog.T.API, "Successfully fetched WP.com categories");

            return models;
        } catch (JSONException exception) {
            AppLog.d(AppLog.T.API, "Error parsing WP.com categories response:" + response);
            return null;
        }
    }

    public CategoryModel() {
        id = -1;
        name = "";
        slug = "";
        description = "";
        parentId = -1;
        postCount = 0;
        isInLocalTable = false;
    }

    public void deserializeLocalDatabaseCursor(Cursor cursor) {
        if (cursor == null || !cursor.moveToFirst()) return;

        id = cursor.getInt(cursor.getColumnIndex(SiteSettingsTable.CAT_ID_COLUMN_NAME));
        name = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.CAT_NAME_COLUMN_NAME));
        slug = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.CAT_SLUG_COLUMN_NAME));
        description = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.CAT_DESC_COLUMN_NAME));
        parentId = cursor.getInt(cursor.getColumnIndex(SiteSettingsTable.CAT_PARENT_ID_COLUMN_NAME));
        postCount = cursor.getInt(cursor.getColumnIndex(SiteSettingsTable.CAT_POST_COUNT_COLUMN_NAME));
        isInLocalTable = true;
    }

    public void deserializeDotComRestResponse(JSONObject category) throws JSONException {
        if (category == null) return;

        id = category.getInt(ID_WPCOM_REST_KEY);
        name = category.getString(NAME_WPCOM_REST_KEY);
        slug = category.getString(SLUG_WPCOM_REST_KEY);
        description = category.getString(DESC_WPCOM_REST_KEY);
        parentId = category.getInt(PARENT_ID_WPCOM_REST_KEY);
        postCount = category.getInt(POST_COUNT_WPCOM_REST_KEY);
    }
}
