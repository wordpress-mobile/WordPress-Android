package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * Holds blog settings and provides methods to (de)serialize .com and self-hosted network calls.
 */
public class SiteSettingsModel {
    // Settings table column names
    public static final String ID_COLUMN_NAME = "id";
    public static final String ADDRESS_COLUMN_NAME = "address";
    public static final String USERNAME_COLUMN_NAME = "username";
    public static final String PASSWORD_COLUMN_NAME = "password";
    public static final String TITLE_COLUMN_NAME = "title";
    public static final String TAGLINE_COLUMN_NAME = "tagline";
    public static final String LANGUAGE_COLUMN_NAME = "language";
    public static final String PRIVACY_COLUMN_NAME = "privacy";
    public static final String LOCATION_COLUMN_NAME = "location";
    public static final String DEF_CATEGORY_COLUMN_NAME = "defaultCategory";
    public static final String DEF_POST_FORMAT_COLUMN_NAME = "defaultPostFormat";
    public static final String CATEGORIES_COLUMN_NAME = "categories";
    public static final String POST_FORMATS_COLUMN_NAME = "postFormats";

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
     * Sets values from a local database {@link Cursor}.
     */
    public void deserializeOptionsDatabaseCursor(Cursor cursor, Map<Integer, CategoryModel> models) {
        if (cursor == null || !cursor.moveToFirst() || cursor.getCount() == 0) return;

        localTableId = cursor.getInt(cursor.getColumnIndex(ID_COLUMN_NAME));
        address = cursor.getString(cursor.getColumnIndex(ADDRESS_COLUMN_NAME));
        username = cursor.getString(cursor.getColumnIndex(USERNAME_COLUMN_NAME));
        password = cursor.getString(cursor.getColumnIndex(PASSWORD_COLUMN_NAME));
        title = cursor.getString(cursor.getColumnIndex(TITLE_COLUMN_NAME));
        tagline = cursor.getString(cursor.getColumnIndex(TAGLINE_COLUMN_NAME));
        languageId = cursor.getInt(cursor.getColumnIndex(LANGUAGE_COLUMN_NAME));
        privacy = cursor.getInt(cursor.getColumnIndex(PRIVACY_COLUMN_NAME));
        defaultCategory = cursor.getInt(cursor.getColumnIndex(DEF_CATEGORY_COLUMN_NAME));
        defaultPostFormat = cursor.getString(cursor.getColumnIndex(DEF_POST_FORMAT_COLUMN_NAME));

        String cachedLocation = cursor.getString(cursor.getColumnIndex(LOCATION_COLUMN_NAME));
        location = cachedLocation != null && !cachedLocation.equals(String.valueOf(0));

        String cachedCategories = cursor.getString(cursor.getColumnIndex(CATEGORIES_COLUMN_NAME));
        String cachedFormats = cursor.getString(cursor.getColumnIndex(POST_FORMATS_COLUMN_NAME));
        if (models != null && !TextUtils.isEmpty(cachedCategories)) {
            String[] split = cachedCategories.split(",");
            categories = new CategoryModel[split.length];
            for (int i = 0; i < split.length; ++i) {
                int catId = Integer.parseInt(split[i]);
                categories[i] = models.get(catId);
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
    
    /**
     * Creates the {@link ContentValues} object to store this category data in a local database.
     */
    public ContentValues serializeToDatabase() {
        ContentValues values = new ContentValues();
        values.put(ID_COLUMN_NAME, localTableId);
        values.put(ADDRESS_COLUMN_NAME, address);
        values.put(USERNAME_COLUMN_NAME, username);
        values.put(PASSWORD_COLUMN_NAME, password);
        values.put(TITLE_COLUMN_NAME, title);
        values.put(TAGLINE_COLUMN_NAME, tagline);
        values.put(PRIVACY_COLUMN_NAME, privacy);
        values.put(LANGUAGE_COLUMN_NAME, languageId);
        values.put(LOCATION_COLUMN_NAME, location);
        values.put(DEF_CATEGORY_COLUMN_NAME, defaultCategory);
        values.put(CATEGORIES_COLUMN_NAME, commaSeparatedElements(categories));
        values.put(DEF_POST_FORMAT_COLUMN_NAME, defaultPostFormat);
        values.put(POST_FORMATS_COLUMN_NAME, serializePostFormats(postFormats));
        
        return values;
    }

    private static String serializePostFormats(Map<String, String> formats) {
        if (formats == null || formats.size() == 0) return "";

        StringBuilder builder = new StringBuilder();
        for (String key : formats.keySet()) {
            builder.append(key).append(",").append(formats.get(key)).append(";");
        }
        builder.setLength(builder.length() - 1);

        return builder.toString();
    }

    private static String commaSeparatedElements(CategoryModel[] elements) {
        if (elements == null) return "";

        StringBuilder builder = new StringBuilder();
        for (CategoryModel element : elements) {
            builder.append(Integer.toString(element.id)).append(",");
        }
        builder.setLength(builder.length() - 1);

        return builder.toString();
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
