package org.wordpress.android.models;

import android.database.Cursor;
import android.text.TextUtils;

import org.json.JSONObject;
import org.wordpress.android.datasets.SiteSettingsTable;
import org.wordpress.android.networking.RestClientUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds blog settings and provides methods to (de)serialize .com and self-hosted network calls.
 */
public class SiteSettingsModel {
    public boolean isInLocalTable;
    public long localTableId;
    public String address;
    public String username;
    public String password;
    public String title;
    public String tagline;
    public String language;
    public int languageId;
    public int privacy;
    public boolean location;
    public int defaultCategory;
    public CategoryModel[] categories;
    public String defaultPostFormat;
    public Map<String, String> postFormats;

    public boolean isTheSame(SiteSettingsModel other) {
        return address.equals(other.address) &&
               username.equals(other.username) &&
               password.equals(other.password) &&
               title.equals(other.title) &&
               tagline.equals(other.tagline) &&
               languageId == other.languageId &&
               privacy == other.privacy &&
               location == other.location &&
               defaultPostFormat.equals(other.defaultPostFormat) &&
               defaultCategory == other.defaultCategory;
    }

    public boolean isSamePostFormats(String[] keys) {
        if (keys == null) return postFormats == null;

        for (String key : keys) {
            if (!postFormats.containsKey(key)) return false;
        }

        return true;
    }

    /**
     * Copies data from another {@link SiteSettingsModel}.
     */
    public void copyFrom(SiteSettingsModel other) {
        if (other == null) return;

        isInLocalTable = other.isInLocalTable;
        localTableId = other.localTableId;
        address = other.address;
        username = other.username;
        password = other.password;
        title = other.title;
        tagline = other.tagline;
        language = other.language;
        languageId = other.languageId;
        privacy = other.privacy;
        location = other.location;
        defaultCategory = other.defaultCategory;
        categories = other.categories;
        defaultPostFormat = other.defaultPostFormat;
        postFormats = other.postFormats;
    }

    public void copyFormatsFrom(SiteSettingsModel other) {
        if (other.postFormats == null) return;
        postFormats = new HashMap<>(other.postFormats);
    }

    /**
     * Sets values from a .com REST response object.
     */
    public void deserializeDotComRestResponse(Blog blog, JSONObject response) {
        if (blog == null || response == null) return;
        JSONObject settingsObject = response.optJSONObject("settings");

        username = blog.getUsername();
        password = blog.getPassword();
        address = response.optString(RestClientUtils.SITE_URL_KEY, "");
        title = response.optString(RestClientUtils.SITE_TITLE_KEY, "");
        tagline = response.optString(RestClientUtils.SITE_DESC_KEY, "");
        languageId = settingsObject.optInt(RestClientUtils.SITE_LANGUAGE_ID_KEY, -1);
        privacy = settingsObject.optInt(RestClientUtils.SITE_PRIVACY_KEY, -2);
        defaultCategory = settingsObject.optInt(RestClientUtils.SITE_DEF_CATEGORY_KEY, 0);
        defaultPostFormat = settingsObject.optString(RestClientUtils.SITE_DEF_POST_FORMAT_KEY, "0");
    }

    /**
     * Sets values from a local database {@link Cursor}.
     */
    public void deserializeDatabaseCursor(Cursor cursor) {
        if (cursor == null) return;

        localTableId = cursor.getInt(cursor.getColumnIndex(SiteSettingsTable.ID_COLUMN_NAME));
        address = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.ADDRESS_COLUMN_NAME));
        username = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.USERNAME_COLUMN_NAME));
        password = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.PASSWORD_COLUMN_NAME));
        title = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.TITLE_COLUMN_NAME));
        tagline = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.TAGLINE_COLUMN_NAME));
        languageId = cursor.getInt(cursor.getColumnIndex(SiteSettingsTable.LANGUAGE_COLUMN_NAME));
        privacy = cursor.getInt(cursor.getColumnIndex(SiteSettingsTable.PRIVACY_COLUMN_NAME));
        defaultCategory = cursor.getInt(cursor.getColumnIndex(SiteSettingsTable.DEF_CATEGORY_COLUMN_NAME));
        defaultPostFormat = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.DEF_POST_FORMAT_COLUMN_NAME));

        String cachedCategories = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.CATEGORIES_COLUMN_NAME));
        String cachedFormats = cursor.getString(cursor.getColumnIndex(SiteSettingsTable.POST_FORMATS_COLUMN_NAME));
        if (!TextUtils.isEmpty(cachedCategories)) {
            String[] split = cachedCategories.split(",");
            categories = new CategoryModel[split.length];
            for (int i = 0; i < split.length; ++i) {
                int catId = Integer.parseInt(split[i]);
                categories[i] = new CategoryModel();
                categories[i].deserializeFromDatabase(SiteSettingsTable.getCategory(catId));
            }
        }
        if (!TextUtils.isEmpty(cachedFormats)) {
            String[] split = cachedFormats.split(";");
            postFormats = new HashMap<>();
            for (String format : split) {
                String[] kvp = format.split(",");
                postFormats.put(kvp[0], kvp[1]);
            }
        }

        isInLocalTable = true;
    }

    public String[] getCategoriesForDisplay() {
        if (categories == null) return null;

        String[] categoryStrings = new String[categories.length];
        for (int i = 0; i < categories.length; ++i) {
            categoryStrings[i] = categories[i].name;
        }

        return categoryStrings;
    }

    public String getDefaultCategoryForDisplay() {
        for (CategoryModel model : categories) {
            if (model.id == defaultCategory) {
                return model.name;
            }
        }

        return "";
    }

    public String getDefaultFormatForDisplay() {
        if (postFormats.containsKey(defaultPostFormat)) {
            return postFormats.get(defaultPostFormat);
        }

        return "";
    }
}
