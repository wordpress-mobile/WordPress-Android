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
    public static final String CREDS_VERIFIED_COLUMN_NAME = "credsVerified";

    public boolean isInLocalTable;
    public boolean hasVerifiedCredentials;
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

    @Override
    public boolean equals(Object other) {
        if (!(other instanceof SiteSettingsModel)) return false;
        SiteSettingsModel otherModel = (SiteSettingsModel) other;

        return localTableId == otherModel.localTableId &&
                address.equals(otherModel.address) &&
                username.equals(otherModel.username) &&
                password.equals(otherModel.password) &&
                title.equals(otherModel.title) &&
                tagline.equals(otherModel.tagline) &&
                languageId == otherModel.languageId &&
                privacy == otherModel.privacy &&
                location == otherModel.location &&
                defaultPostFormat.equals(otherModel.defaultPostFormat) &&
                defaultCategory == otherModel.defaultCategory;
    }

    /**
     * Copies data from another {@link SiteSettingsModel}.
     */
    public void copyFrom(SiteSettingsModel other) {
        if (other == null) return;

        isInLocalTable = other.isInLocalTable;
        hasVerifiedCredentials = other.hasVerifiedCredentials;
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

    /**
     * Sets values from a local database {@link Cursor}.
     */
    public void deserializeOptionsDatabaseCursor(Cursor cursor, Map<Integer, CategoryModel> models) {
        if (cursor == null || !cursor.moveToFirst() || cursor.getCount() == 0) return;

        localTableId = getIntFromCursor(cursor, ID_COLUMN_NAME);
        address = getStringFromCursor(cursor, ADDRESS_COLUMN_NAME);
        username = getStringFromCursor(cursor, USERNAME_COLUMN_NAME);
        password = getStringFromCursor(cursor, PASSWORD_COLUMN_NAME);
        title = getStringFromCursor(cursor, TITLE_COLUMN_NAME);
        tagline = getStringFromCursor(cursor, TAGLINE_COLUMN_NAME);
        languageId = getIntFromCursor(cursor, LANGUAGE_COLUMN_NAME);
        privacy = getIntFromCursor(cursor, PRIVACY_COLUMN_NAME);
        defaultCategory = getIntFromCursor(cursor, DEF_CATEGORY_COLUMN_NAME);
        defaultPostFormat = getStringFromCursor(cursor, DEF_POST_FORMAT_COLUMN_NAME);
        location = getBooleanFromCursor(cursor, LOCATION_COLUMN_NAME);
        hasVerifiedCredentials = getBooleanFromCursor(cursor, CREDS_VERIFIED_COLUMN_NAME);

        String cachedCategories = getStringFromCursor(cursor, CATEGORIES_COLUMN_NAME);
        String cachedFormats = getStringFromCursor(cursor, POST_FORMATS_COLUMN_NAME);
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
        values.put(CATEGORIES_COLUMN_NAME, categoryIdList(categories));
        values.put(DEF_POST_FORMAT_COLUMN_NAME, defaultPostFormat);
        values.put(POST_FORMATS_COLUMN_NAME, postFormatList(postFormats));
        values.put(CREDS_VERIFIED_COLUMN_NAME, hasVerifiedCredentials);

        return values;
    }

    /**
     * Used to serialize post formats to store in a local database.
     *
     * @param formats
     * map of post formats where the key is the format ID and the value is the format name
     * @return
     * a String of semi-colon separated KVP's of Post Formats; Post Format ID -> Post Format Name
     */
    private static String postFormatList(Map<String, String> formats) {
        if (formats == null || formats.size() == 0) return "";

        StringBuilder builder = new StringBuilder();
        for (String key : formats.keySet()) {
            builder.append(key).append(",").append(formats.get(key)).append(";");
        }
        builder.setLength(builder.length() - 1);

        return builder.toString();
    }

    /**
     * Used to serialize categories to store in a local database.
     *
     * @param elements
     * {@link CategoryModel} array to create String ID list from
     * @return
     * a String of comma-separated integer Category ID's
     */
    private static String categoryIdList(CategoryModel[] elements) {
        if (elements == null || elements.length == 0) return "";

        StringBuilder builder = new StringBuilder();
        for (CategoryModel element : elements) {
            builder.append(String.valueOf(element.id)).append(",");
        }
        builder.setLength(builder.length() - 1);

        return builder.toString();
    }

    /**
     * Helper method to get an integer value from a given column in a Cursor.
     */
    private int getIntFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getInt(columnIndex) : -1;
    }

    /**
     * Helper method to get a String value from a given column in a Cursor.
     */
    private String getStringFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 ? cursor.getString(columnIndex) : "";
    }

    /**
     * Helper method to get a boolean value (stored as an int) from a given column in a Cursor.
     */
    private boolean getBooleanFromCursor(Cursor cursor, String columnName) {
        int columnIndex = cursor.getColumnIndex(columnName);
        return columnIndex != -1 && cursor.getInt(columnIndex) != 0;
    }
}
