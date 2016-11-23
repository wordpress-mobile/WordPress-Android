package org.wordpress.android.models;

import android.content.ContentValues;
import android.database.Cursor;
import android.text.TextUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Holds blog settings and provides methods to (de)serialize .com and self-hosted network calls.
 */

public class SiteSettingsModel {
    public static final int RELATED_POSTS_ENABLED_FLAG = 0x1;
    public static final int RELATED_POST_HEADER_FLAG = 0x2;
    public static final int RELATED_POST_IMAGE_FLAG = 0x4;

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
    public static final String RELATED_POSTS_COLUMN_NAME = "relatedPosts";
    public static final String ALLOW_COMMENTS_COLUMN_NAME = "allowComments";
    public static final String SEND_PINGBACKS_COLUMN_NAME = "sendPingbacks";
    public static final String RECEIVE_PINGBACKS_COLUMN_NAME = "receivePingbacks";
    public static final String SHOULD_CLOSE_AFTER_COLUMN_NAME = "shouldCloseAfter";
    public static final String CLOSE_AFTER_COLUMN_NAME = "closeAfter";
    public static final String SORT_BY_COLUMN_NAME = "sortBy";
    public static final String SHOULD_THREAD_COLUMN_NAME = "shouldThread";
    public static final String THREADING_COLUMN_NAME = "threading";
    public static final String SHOULD_PAGE_COLUMN_NAME = "shouldPage";
    public static final String PAGING_COLUMN_NAME = "paging";
    public static final String MANUAL_APPROVAL_COLUMN_NAME = "manualApproval";
    public static final String IDENTITY_REQUIRED_COLUMN_NAME = "identityRequired";
    public static final String USER_ACCOUNT_REQUIRED_COLUMN_NAME = "userAccountRequired";
    public static final String WHITELIST_COLUMN_NAME = "whitelist";
    public static final String MODERATION_KEYS_COLUMN_NAME = "moderationKeys";
    public static final String BLACKLIST_KEYS_COLUMN_NAME = "blacklistKeys";

    public static final String SETTINGS_TABLE_NAME = "site_settings";
    public static final String CREATE_SETTINGS_TABLE_SQL =
            "CREATE TABLE IF NOT EXISTS " +
                    SETTINGS_TABLE_NAME +
                    " (" +
                    ID_COLUMN_NAME + " INTEGER PRIMARY KEY, " +
                    ADDRESS_COLUMN_NAME + " TEXT, " +
                    USERNAME_COLUMN_NAME + " TEXT, " +
                    PASSWORD_COLUMN_NAME + " TEXT, " +
                    TITLE_COLUMN_NAME + " TEXT, " +
                    TAGLINE_COLUMN_NAME + " TEXT, " +
                    LANGUAGE_COLUMN_NAME + " INTEGER, " +
                    PRIVACY_COLUMN_NAME + " INTEGER, " +
                    LOCATION_COLUMN_NAME + " BOOLEAN, " +
                    DEF_CATEGORY_COLUMN_NAME + " TEXT, " +
                    DEF_POST_FORMAT_COLUMN_NAME + " TEXT, " +
                    CATEGORIES_COLUMN_NAME + " TEXT, " +
                    POST_FORMATS_COLUMN_NAME + " TEXT, " +
                    CREDS_VERIFIED_COLUMN_NAME + " BOOLEAN, " +
                    RELATED_POSTS_COLUMN_NAME + " INTEGER, " +
                    ALLOW_COMMENTS_COLUMN_NAME + " BOOLEAN, " +
                    SEND_PINGBACKS_COLUMN_NAME + " BOOLEAN, " +
                    RECEIVE_PINGBACKS_COLUMN_NAME + " BOOLEAN, " +
                    SHOULD_CLOSE_AFTER_COLUMN_NAME + " BOOLEAN, " +
                    CLOSE_AFTER_COLUMN_NAME + " INTEGER, " +
                    SORT_BY_COLUMN_NAME + " INTEGER, " +
                    SHOULD_THREAD_COLUMN_NAME + " BOOLEAN, " +
                    THREADING_COLUMN_NAME + " INTEGER, " +
                    SHOULD_PAGE_COLUMN_NAME + " BOOLEAN, " +
                    PAGING_COLUMN_NAME + " INTEGER, " +
                    MANUAL_APPROVAL_COLUMN_NAME + " BOOLEAN, " +
                    IDENTITY_REQUIRED_COLUMN_NAME + " BOOLEAN, " +
                    USER_ACCOUNT_REQUIRED_COLUMN_NAME + " BOOLEAN, " +
                    WHITELIST_COLUMN_NAME + " BOOLEAN, " +
                    MODERATION_KEYS_COLUMN_NAME + " TEXT, " +
                    BLACKLIST_KEYS_COLUMN_NAME + " TEXT" +
                    ");";

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
    public boolean showRelatedPosts;
    public boolean showRelatedPostHeader;
    public boolean showRelatedPostImages;
    public boolean allowComments;
    public boolean sendPingbacks;
    public boolean receivePingbacks;
    public boolean shouldCloseAfter;
    public int closeCommentAfter;
    public int sortCommentsBy;
    public boolean shouldThreadComments;
    public int threadingLevels;
    public boolean shouldPageComments;
    public int commentsPerPage;
    public boolean commentApprovalRequired;
    public boolean commentsRequireIdentity;
    public boolean commentsRequireUserAccount;
    public boolean commentAutoApprovalKnownUsers;
    public int maxLinks;
    public List<String> holdForModeration;
    public List<String> blacklist;

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
                defaultCategory == otherModel.defaultCategory &&
                showRelatedPosts == otherModel.showRelatedPosts &&
                showRelatedPostHeader == otherModel.showRelatedPostHeader &&
                showRelatedPostImages == otherModel.showRelatedPostImages &&
                allowComments == otherModel.allowComments &&
                sendPingbacks == otherModel.sendPingbacks &&
                receivePingbacks == otherModel.receivePingbacks &&
                closeCommentAfter == otherModel.closeCommentAfter &&
                sortCommentsBy == otherModel.sortCommentsBy &&
                threadingLevels == otherModel.threadingLevels &&
                commentsPerPage == otherModel.commentsPerPage &&
                commentApprovalRequired == otherModel.commentApprovalRequired &&
                commentsRequireIdentity == otherModel.commentsRequireIdentity &&
                commentsRequireUserAccount == otherModel.commentsRequireUserAccount &&
                commentAutoApprovalKnownUsers == otherModel.commentAutoApprovalKnownUsers &&
                maxLinks == otherModel.maxLinks &&
                holdForModeration != null && holdForModeration.equals(otherModel.holdForModeration) &&
                blacklist != null && blacklist.equals(otherModel.blacklist);
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
        showRelatedPosts = other.showRelatedPosts;
        showRelatedPostHeader = other.showRelatedPostHeader;
        showRelatedPostImages = other.showRelatedPostImages;
        allowComments = other.allowComments;
        sendPingbacks = other.sendPingbacks;
        receivePingbacks = other.receivePingbacks;
        shouldCloseAfter = other.shouldCloseAfter;
        closeCommentAfter = other.closeCommentAfter;
        sortCommentsBy = other.sortCommentsBy;
        shouldThreadComments = other.shouldThreadComments;
        threadingLevels = other.threadingLevels;
        shouldPageComments = other.shouldPageComments;
        commentsPerPage = other.commentsPerPage;
        commentApprovalRequired = other.commentApprovalRequired;
        commentsRequireIdentity = other.commentsRequireIdentity;
        commentsRequireUserAccount = other.commentsRequireUserAccount;
        commentAutoApprovalKnownUsers = other.commentAutoApprovalKnownUsers;
        maxLinks = other.maxLinks;
        if (other.holdForModeration != null) {
            holdForModeration = new ArrayList<>(other.holdForModeration);
        }
        if (other.blacklist != null) {
            blacklist = new ArrayList<>(other.blacklist);
        }
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
        allowComments = getBooleanFromCursor(cursor, ALLOW_COMMENTS_COLUMN_NAME);
        sendPingbacks = getBooleanFromCursor(cursor, SEND_PINGBACKS_COLUMN_NAME);
        receivePingbacks = getBooleanFromCursor(cursor, RECEIVE_PINGBACKS_COLUMN_NAME);
        shouldCloseAfter = getBooleanFromCursor(cursor, SHOULD_CLOSE_AFTER_COLUMN_NAME);
        closeCommentAfter = getIntFromCursor(cursor, CLOSE_AFTER_COLUMN_NAME);
        sortCommentsBy = getIntFromCursor(cursor, SORT_BY_COLUMN_NAME);
        shouldThreadComments = getBooleanFromCursor(cursor, SHOULD_THREAD_COLUMN_NAME);
        threadingLevels = getIntFromCursor(cursor, THREADING_COLUMN_NAME);
        shouldPageComments = getBooleanFromCursor(cursor, SHOULD_PAGE_COLUMN_NAME);
        commentsPerPage = getIntFromCursor(cursor, PAGING_COLUMN_NAME);
        commentApprovalRequired = getBooleanFromCursor(cursor, MANUAL_APPROVAL_COLUMN_NAME);
        commentsRequireIdentity = getBooleanFromCursor(cursor, IDENTITY_REQUIRED_COLUMN_NAME);
        commentsRequireUserAccount = getBooleanFromCursor(cursor, USER_ACCOUNT_REQUIRED_COLUMN_NAME);
        commentAutoApprovalKnownUsers = getBooleanFromCursor(cursor, WHITELIST_COLUMN_NAME);

        String moderationKeys = getStringFromCursor(cursor, MODERATION_KEYS_COLUMN_NAME);
        String blacklistKeys = getStringFromCursor(cursor, BLACKLIST_KEYS_COLUMN_NAME);
        holdForModeration = new ArrayList<>();
        blacklist = new ArrayList<>();
        if (!TextUtils.isEmpty(moderationKeys)) {
            Collections.addAll(holdForModeration, moderationKeys.split("\n"));
        }
        if (!TextUtils.isEmpty(blacklistKeys)) {
            Collections.addAll(blacklist, blacklistKeys.split("\n"));
        }

        setRelatedPostsFlags(Math.max(0, getIntFromCursor(cursor, RELATED_POSTS_COLUMN_NAME)));

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

        int cachedRelatedPosts = getIntFromCursor(cursor, RELATED_POSTS_COLUMN_NAME);
        if (cachedRelatedPosts != -1) {
            setRelatedPostsFlags(cachedRelatedPosts);
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
        values.put(RELATED_POSTS_COLUMN_NAME, getRelatedPostsFlags());
        values.put(ALLOW_COMMENTS_COLUMN_NAME, allowComments);
        values.put(SEND_PINGBACKS_COLUMN_NAME, sendPingbacks);
        values.put(RECEIVE_PINGBACKS_COLUMN_NAME, receivePingbacks);
        values.put(SHOULD_CLOSE_AFTER_COLUMN_NAME, shouldCloseAfter);
        values.put(CLOSE_AFTER_COLUMN_NAME, closeCommentAfter);
        values.put(SORT_BY_COLUMN_NAME, sortCommentsBy);
        values.put(SHOULD_THREAD_COLUMN_NAME, shouldThreadComments);
        values.put(THREADING_COLUMN_NAME, threadingLevels);
        values.put(SHOULD_PAGE_COLUMN_NAME, shouldPageComments);
        values.put(PAGING_COLUMN_NAME, commentsPerPage);
        values.put(MANUAL_APPROVAL_COLUMN_NAME, commentApprovalRequired);
        values.put(IDENTITY_REQUIRED_COLUMN_NAME, commentsRequireIdentity);
        values.put(USER_ACCOUNT_REQUIRED_COLUMN_NAME, commentsRequireUserAccount);
        values.put(WHITELIST_COLUMN_NAME, commentAutoApprovalKnownUsers);

        String moderationKeys = "";
        if (holdForModeration != null) {
            for (String key : holdForModeration) {
                moderationKeys += key + "\n";
            }
        }
        String blacklistKeys = "";
        if (blacklist != null) {
            for (String key : blacklist) {
                blacklistKeys += key + "\n";
            }
        }
        values.put(MODERATION_KEYS_COLUMN_NAME, moderationKeys);
        values.put(BLACKLIST_KEYS_COLUMN_NAME, blacklistKeys);

        return values;
    }

    public int getRelatedPostsFlags() {
        int flags = 0;

        if (showRelatedPosts) flags |= RELATED_POSTS_ENABLED_FLAG;
        if (showRelatedPostHeader) flags |= RELATED_POST_HEADER_FLAG;
        if (showRelatedPostImages) flags |= RELATED_POST_IMAGE_FLAG;

        return flags;
    }

    public void setRelatedPostsFlags(int flags) {
        showRelatedPosts = (flags & RELATED_POSTS_ENABLED_FLAG) > 0;
        showRelatedPostHeader = (flags & RELATED_POST_HEADER_FLAG) > 0;
        showRelatedPostImages = (flags & RELATED_POST_IMAGE_FLAG) > 0;
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
