package org.wordpress.android.models

import android.content.ContentValues
import android.database.Cursor
import android.support.v4.util.SparseArrayCompat
import android.text.TextUtils

/**
 * Holds blog settings and provides methods to (de) serialize .com and self-hosted network calls.
 */

data class SiteSettingsModel(
    var isInLocalTable: Boolean = false,
    var hasVerifiedCredentials: Boolean = false,
    var localTableId: Long = 0,
    var address: String? = null,
    var username: String? = null,
    var password: String? = null,
    var title: String? = null,
    var tagline: String? = null,
    var language: String? = null,
    var siteIconMediaId: Int = 0,
    var languageId: Int = 0,
    var privacy: Int = 0,
    var location: Boolean = false,
    var defaultCategory: Int = 0,
    var categories: Array<CategoryModel>? = null,
    var defaultPostFormat: String? = null,
    var postFormats: MutableMap<String, String>? = null,
    var showRelatedPosts: Boolean = false,
    var showRelatedPostHeader: Boolean = false,
    var showRelatedPostImages: Boolean = false,
    var allowComments: Boolean = false,
    var sendPingbacks: Boolean = false,
    var receivePingbacks: Boolean = false,
    var shouldCloseAfter: Boolean = false,
    var closeCommentAfter: Int = 0,
    var sortCommentsBy: Int = 0,
    var shouldThreadComments: Boolean = false,
    var threadingLevels: Int = 0,
    var shouldPageComments: Boolean = false,
    var commentsPerPage: Int = 0,
    var commentApprovalRequired: Boolean = false,
    var commentsRequireIdentity: Boolean = false,
    var commentsRequireUserAccount: Boolean = false,
    var commentAutoApprovalKnownUsers: Boolean = false,
    var maxLinks: Int = 0,
    var holdForModeration: List<String>? = null,
    var blacklist: List<String>? = null,
    var sharingLabel: String? = null,
    var sharingButtonStyle: String? = null,
    var allowReblogButton: Boolean = false,
    var allowLikeButton: Boolean = false,
    var allowCommentLikes: Boolean = false,
    var twitterUsername: String? = null,
    var startOfWeek: String? = null,
    var dateFormat: String? = null,
    var timeFormat: String? = null,
    var timezone: String? = null,
    var postsPerPage: Int = 0,
    var ampSupported: Boolean = false,
    var ampEnabled: Boolean = false,
    var quotaDiskSpace: String? = null,
    var isPortfolioEnabled: Boolean = false,
    var portfolioPostsPerPage: Int = 0
) {
    private var relatedPostsFlags: Int
        get() {
            var flags = 0

            if (showRelatedPosts) {
                flags = flags or RELATED_POSTS_ENABLED_FLAG
            }
            if (showRelatedPostHeader) {
                flags = flags or RELATED_POST_HEADER_FLAG
            }
            if (showRelatedPostImages) {
                flags = flags or RELATED_POST_IMAGE_FLAG
            }

            return flags
        }
        set(flags) {
            showRelatedPosts = flags and RELATED_POSTS_ENABLED_FLAG > 0
            showRelatedPostHeader = flags and RELATED_POST_HEADER_FLAG > 0
            showRelatedPostImages = flags and RELATED_POST_IMAGE_FLAG > 0
        }

    /**
     * Copies data from another [SiteSettingsModel].
     */
    fun copyFrom(other: SiteSettingsModel?) {
        if (other == null) {
            return
        }

        isInLocalTable = other.isInLocalTable
        hasVerifiedCredentials = other.hasVerifiedCredentials
        localTableId = other.localTableId
        address = other.address
        username = other.username
        password = other.password
        title = other.title
        tagline = other.tagline
        language = other.language
        languageId = other.languageId
        siteIconMediaId = other.siteIconMediaId
        privacy = other.privacy
        location = other.location
        defaultCategory = other.defaultCategory
        categories = other.categories
        defaultPostFormat = other.defaultPostFormat
        postFormats = other.postFormats
        showRelatedPosts = other.showRelatedPosts
        showRelatedPostHeader = other.showRelatedPostHeader
        showRelatedPostImages = other.showRelatedPostImages
        allowComments = other.allowComments
        sendPingbacks = other.sendPingbacks
        receivePingbacks = other.receivePingbacks
        shouldCloseAfter = other.shouldCloseAfter
        closeCommentAfter = other.closeCommentAfter
        sortCommentsBy = other.sortCommentsBy
        shouldThreadComments = other.shouldThreadComments
        threadingLevels = other.threadingLevels
        shouldPageComments = other.shouldPageComments
        commentsPerPage = other.commentsPerPage
        commentApprovalRequired = other.commentApprovalRequired
        commentsRequireIdentity = other.commentsRequireIdentity
        commentsRequireUserAccount = other.commentsRequireUserAccount
        commentAutoApprovalKnownUsers = other.commentAutoApprovalKnownUsers
        maxLinks = other.maxLinks
        startOfWeek = other.startOfWeek
        dateFormat = other.dateFormat
        timeFormat = other.timeFormat
        timezone = other.timezone
        postsPerPage = other.postsPerPage
        ampSupported = other.ampSupported
        ampEnabled = other.ampEnabled
        isPortfolioEnabled = other.isPortfolioEnabled
        portfolioPostsPerPage = other.portfolioPostsPerPage
        if (other.holdForModeration != null) {
            holdForModeration = ArrayList(other.holdForModeration!!)
        }
        if (other.blacklist != null) {
            blacklist = ArrayList(other.blacklist!!)
        }
        if (other.sharingLabel != null) {
            sharingLabel = other.sharingLabel
        }
        if (other.sharingButtonStyle != null) {
            sharingButtonStyle = other.sharingButtonStyle
        }
        allowReblogButton = other.allowReblogButton
        allowLikeButton = other.allowLikeButton
        allowCommentLikes = other.allowCommentLikes
        if (other.twitterUsername != null) {
            twitterUsername = other.twitterUsername
        }
        quotaDiskSpace = other.quotaDiskSpace
    }

    /**
     * Sets values from a local database [Cursor].
     */
    fun deserializeOptionsDatabaseCursor(cursor: Cursor?, models: SparseArrayCompat<CategoryModel>?) {
        if (cursor == null || !cursor.moveToFirst() || cursor.count == 0) {
            return
        }

        localTableId = getIntFromCursor(cursor, ID_COLUMN_NAME).toLong()
        address = getStringFromCursor(cursor, ADDRESS_COLUMN_NAME)
        username = getStringFromCursor(cursor, USERNAME_COLUMN_NAME)
        password = getStringFromCursor(cursor, PASSWORD_COLUMN_NAME)
        title = getStringFromCursor(cursor, TITLE_COLUMN_NAME)
        tagline = getStringFromCursor(cursor, TAGLINE_COLUMN_NAME)
        languageId = getIntFromCursor(cursor, LANGUAGE_COLUMN_NAME)
        siteIconMediaId = getIntFromCursor(cursor, SITE_ICON_COLUMN_NAME)
        privacy = getIntFromCursor(cursor, PRIVACY_COLUMN_NAME)
        defaultCategory = getIntFromCursor(cursor, DEF_CATEGORY_COLUMN_NAME)
        defaultPostFormat = getStringFromCursor(cursor, DEF_POST_FORMAT_COLUMN_NAME)
        location = getBooleanFromCursor(cursor, LOCATION_COLUMN_NAME)
        hasVerifiedCredentials = getBooleanFromCursor(cursor, CREDS_VERIFIED_COLUMN_NAME)
        allowComments = getBooleanFromCursor(cursor, ALLOW_COMMENTS_COLUMN_NAME)
        sendPingbacks = getBooleanFromCursor(cursor, SEND_PINGBACKS_COLUMN_NAME)
        receivePingbacks = getBooleanFromCursor(cursor, RECEIVE_PINGBACKS_COLUMN_NAME)
        shouldCloseAfter = getBooleanFromCursor(cursor, SHOULD_CLOSE_AFTER_COLUMN_NAME)
        closeCommentAfter = getIntFromCursor(cursor, CLOSE_AFTER_COLUMN_NAME)
        sortCommentsBy = getIntFromCursor(cursor, SORT_BY_COLUMN_NAME)
        shouldThreadComments = getBooleanFromCursor(cursor, SHOULD_THREAD_COLUMN_NAME)
        threadingLevels = getIntFromCursor(cursor, THREADING_COLUMN_NAME)
        shouldPageComments = getBooleanFromCursor(cursor, SHOULD_PAGE_COLUMN_NAME)
        commentsPerPage = getIntFromCursor(cursor, PAGING_COLUMN_NAME)
        commentApprovalRequired = getBooleanFromCursor(cursor, MANUAL_APPROVAL_COLUMN_NAME)
        commentsRequireIdentity = getBooleanFromCursor(cursor, IDENTITY_REQUIRED_COLUMN_NAME)
        commentsRequireUserAccount = getBooleanFromCursor(cursor, USER_ACCOUNT_REQUIRED_COLUMN_NAME)
        commentAutoApprovalKnownUsers = getBooleanFromCursor(cursor, WHITELIST_COLUMN_NAME)
        startOfWeek = getStringFromCursor(cursor, START_OF_WEEK_COLUMN_NAME)
        dateFormat = getStringFromCursor(cursor, DATE_FORMAT_COLUMN_NAME)
        timeFormat = getStringFromCursor(cursor, TIME_FORMAT_COLUMN_NAME)
        timezone = getStringFromCursor(cursor, TIMEZONE_COLUMN_NAME)
        postsPerPage = getIntFromCursor(cursor, POSTS_PER_PAGE_COLUMN_NAME)
        ampSupported = getBooleanFromCursor(cursor, AMP_SUPPORTED_COLUMN_NAME)
        ampEnabled = getBooleanFromCursor(cursor, AMP_ENABLED_COLUMN_NAME)
        isPortfolioEnabled = getBooleanFromCursor(cursor, PORTFOLIO_ENABLED_COLUMN_NAME)
        portfolioPostsPerPage = getIntFromCursor(cursor, PORTFOLIO_POSTS_PER_PAGE_COLUMN_NAME)

        val moderationKeys = getStringFromCursor(cursor, MODERATION_KEYS_COLUMN_NAME)
        val blacklistKeys = getStringFromCursor(cursor, BLACKLIST_KEYS_COLUMN_NAME)

        holdForModeration = moderationKeys?.split('\n')?: emptyList()
        blacklist = blacklistKeys?.split('\n')?: emptyList()
        sharingLabel = getStringFromCursor(cursor, SHARING_LABEL_COLUMN_NAME)
        sharingButtonStyle = getStringFromCursor(cursor, SHARING_BUTTON_STYLE_COLUMN_NAME)
        allowReblogButton = getBooleanFromCursor(cursor, ALLOW_REBLOG_BUTTON_COLUMN_NAME)
        allowLikeButton = getBooleanFromCursor(cursor, ALLOW_LIKE_BUTTON_COLUMN_NAME)
        allowCommentLikes = getBooleanFromCursor(cursor, ALLOW_COMMENT_LIKES_COLUMN_NAME)
        twitterUsername = getStringFromCursor(cursor, TWITTER_USERNAME_COLUMN_NAME)

        relatedPostsFlags = Math.max(0, getIntFromCursor(cursor, RELATED_POSTS_COLUMN_NAME))

        val cachedCategories = getStringFromCursor(cursor, CATEGORIES_COLUMN_NAME)
        val cachedFormats = getStringFromCursor(cursor, POST_FORMATS_COLUMN_NAME)
        if (models != null && !TextUtils.isEmpty(cachedCategories)) {
            categories = cachedCategories!!
                    .split(',')
                    .map { Integer.parseInt(it) }
                    .map { models.get(it) }
                    .toTypedArray()
        }
        if (!TextUtils.isEmpty(cachedFormats)) {
            postFormats = cachedFormats!!
                    .split(';')
                    .map { it.split(',').zipWithNext().first() }
                    .toMap()
                    .toMutableMap()
        }

        val cachedRelatedPosts = getIntFromCursor(cursor, RELATED_POSTS_COLUMN_NAME)
        if (cachedRelatedPosts != -1) {
            relatedPostsFlags = cachedRelatedPosts
        }

        isInLocalTable = true
    }

    /**
     * Creates the [ContentValues] object to store this category data in a local database.
     */
    fun serializeToDatabase(): ContentValues {
        val values = ContentValues()
        values.put(ID_COLUMN_NAME, localTableId)
        values.put(ADDRESS_COLUMN_NAME, address)
        values.put(USERNAME_COLUMN_NAME, username)
        values.put(PASSWORD_COLUMN_NAME, password)
        values.put(TITLE_COLUMN_NAME, title)
        values.put(TAGLINE_COLUMN_NAME, tagline)
        values.put(PRIVACY_COLUMN_NAME, privacy)
        values.put(LANGUAGE_COLUMN_NAME, languageId)
        values.put(SITE_ICON_COLUMN_NAME, siteIconMediaId)
        values.put(LOCATION_COLUMN_NAME, location)
        values.put(DEF_CATEGORY_COLUMN_NAME, defaultCategory)
        values.put(CATEGORIES_COLUMN_NAME, categoryIdList(categories))
        values.put(DEF_POST_FORMAT_COLUMN_NAME, defaultPostFormat)
        values.put(POST_FORMATS_COLUMN_NAME, postFormatList(postFormats))
        values.put(CREDS_VERIFIED_COLUMN_NAME, hasVerifiedCredentials)
        values.put(RELATED_POSTS_COLUMN_NAME, relatedPostsFlags)
        values.put(ALLOW_COMMENTS_COLUMN_NAME, allowComments)
        values.put(SEND_PINGBACKS_COLUMN_NAME, sendPingbacks)
        values.put(RECEIVE_PINGBACKS_COLUMN_NAME, receivePingbacks)
        values.put(SHOULD_CLOSE_AFTER_COLUMN_NAME, shouldCloseAfter)
        values.put(CLOSE_AFTER_COLUMN_NAME, closeCommentAfter)
        values.put(SORT_BY_COLUMN_NAME, sortCommentsBy)
        values.put(SHOULD_THREAD_COLUMN_NAME, shouldThreadComments)
        values.put(THREADING_COLUMN_NAME, threadingLevels)
        values.put(SHOULD_PAGE_COLUMN_NAME, shouldPageComments)
        values.put(PAGING_COLUMN_NAME, commentsPerPage)
        values.put(MANUAL_APPROVAL_COLUMN_NAME, commentApprovalRequired)
        values.put(IDENTITY_REQUIRED_COLUMN_NAME, commentsRequireIdentity)
        values.put(USER_ACCOUNT_REQUIRED_COLUMN_NAME, commentsRequireUserAccount)
        values.put(WHITELIST_COLUMN_NAME, commentAutoApprovalKnownUsers)
        values.put(START_OF_WEEK_COLUMN_NAME, startOfWeek)
        values.put(DATE_FORMAT_COLUMN_NAME, dateFormat)
        values.put(TIME_FORMAT_COLUMN_NAME, timeFormat)
        values.put(TIMEZONE_COLUMN_NAME, timezone)
        values.put(POSTS_PER_PAGE_COLUMN_NAME, postsPerPage)
        values.put(AMP_SUPPORTED_COLUMN_NAME, ampSupported)
        values.put(AMP_ENABLED_COLUMN_NAME, ampEnabled)
        values.put(PORTFOLIO_ENABLED_COLUMN_NAME, isPortfolioEnabled)
        values.put(PORTFOLIO_POSTS_PER_PAGE_COLUMN_NAME, portfolioPostsPerPage)

        var moderationKeys = holdForModeration?.joinToString { it + '\n' } ?: ""
        var blacklistKeys = blacklist?.joinToString { it + '\n' } ?: ""

        values.put(MODERATION_KEYS_COLUMN_NAME, moderationKeys)
        values.put(BLACKLIST_KEYS_COLUMN_NAME, blacklistKeys)
        values.put(SHARING_LABEL_COLUMN_NAME, sharingLabel)
        values.put(SHARING_BUTTON_STYLE_COLUMN_NAME, sharingButtonStyle)
        values.put(ALLOW_REBLOG_BUTTON_COLUMN_NAME, allowReblogButton)
        values.put(ALLOW_LIKE_BUTTON_COLUMN_NAME, allowLikeButton)
        values.put(ALLOW_COMMENT_LIKES_COLUMN_NAME, allowCommentLikes)
        values.put(TWITTER_USERNAME_COLUMN_NAME, twitterUsername)

        return values
    }

    /**
     * Helper method to get an integer value from a given column in a Cursor.
     */
    private fun getIntFromCursor(cursor: Cursor, columnName: String): Int {
        val columnIndex = cursor.getColumnIndex(columnName)
        return if (columnIndex != -1) cursor.getInt(columnIndex) else -1
    }

    /**
     * Helper method to get a String value from a given column in a Cursor.
     */
    private fun getStringFromCursor(cursor: Cursor, columnName: String): String? {
        val columnIndex = cursor.getColumnIndex(columnName)
        return if (columnIndex != -1) cursor.getString(columnIndex) else ""
    }

    /**
     * Helper method to get a boolean value (stored as an int) from a given column in a Cursor.
     */
    private fun getBooleanFromCursor(cursor: Cursor, columnName: String): Boolean {
        val columnIndex = cursor.getColumnIndex(columnName)
        return columnIndex != -1 && cursor.getInt(columnIndex) != 0
    }

    companion object {
        private const val RELATED_POSTS_ENABLED_FLAG = 0x1
        private const val RELATED_POST_HEADER_FLAG = 0x2
        private const val RELATED_POST_IMAGE_FLAG = 0x4

        // Settings table column names
        const val ID_COLUMN_NAME = "id"
        private const val ADDRESS_COLUMN_NAME = "address"
        private const val USERNAME_COLUMN_NAME = "username"
        private const val PASSWORD_COLUMN_NAME = "password"
        private const val TITLE_COLUMN_NAME = "title"
        private const val TAGLINE_COLUMN_NAME = "tagline"
        private const val LANGUAGE_COLUMN_NAME = "language"
        private const val SITE_ICON_COLUMN_NAME = "siteIcon"
        private const val PRIVACY_COLUMN_NAME = "privacy"
        private const val LOCATION_COLUMN_NAME = "location"
        private const val DEF_CATEGORY_COLUMN_NAME = "defaultCategory"
        private const val DEF_POST_FORMAT_COLUMN_NAME = "defaultPostFormat"
        private const val CATEGORIES_COLUMN_NAME = "categories"
        private const val POST_FORMATS_COLUMN_NAME = "postFormats"
        private const val CREDS_VERIFIED_COLUMN_NAME = "credsVerified"
        private const val RELATED_POSTS_COLUMN_NAME = "relatedPosts"
        private const val ALLOW_COMMENTS_COLUMN_NAME = "allowComments"
        private const val SEND_PINGBACKS_COLUMN_NAME = "sendPingbacks"
        private const val RECEIVE_PINGBACKS_COLUMN_NAME = "receivePingbacks"
        private const val SHOULD_CLOSE_AFTER_COLUMN_NAME = "shouldCloseAfter"
        private const val CLOSE_AFTER_COLUMN_NAME = "closeAfter"
        private const val SORT_BY_COLUMN_NAME = "sortBy"
        private const val SHOULD_THREAD_COLUMN_NAME = "shouldThread"
        private const val THREADING_COLUMN_NAME = "threading"
        private const val SHOULD_PAGE_COLUMN_NAME = "shouldPage"
        private const val PAGING_COLUMN_NAME = "paging"
        private const val MANUAL_APPROVAL_COLUMN_NAME = "manualApproval"
        private const val IDENTITY_REQUIRED_COLUMN_NAME = "identityRequired"
        private const val USER_ACCOUNT_REQUIRED_COLUMN_NAME = "userAccountRequired"
        private const val WHITELIST_COLUMN_NAME = "whitelist"
        private const val MODERATION_KEYS_COLUMN_NAME = "moderationKeys"
        private const val BLACKLIST_KEYS_COLUMN_NAME = "blacklistKeys"
        private const val SHARING_LABEL_COLUMN_NAME = "sharingLabel"
        private const val SHARING_BUTTON_STYLE_COLUMN_NAME = "sharingButtonStyle"
        private const val ALLOW_REBLOG_BUTTON_COLUMN_NAME = "allowReblogButton"
        private const val ALLOW_LIKE_BUTTON_COLUMN_NAME = "allowLikeButton"
        private const val ALLOW_COMMENT_LIKES_COLUMN_NAME = "allowCommentLikes"
        private const val TWITTER_USERNAME_COLUMN_NAME = "twitterUsername"
        private const val START_OF_WEEK_COLUMN_NAME = "startOfWeek"
        private const val DATE_FORMAT_COLUMN_NAME = "dateFormat"
        private const val TIME_FORMAT_COLUMN_NAME = "timeFormat"
        private const val TIMEZONE_COLUMN_NAME = "siteTimezone"
        private const val POSTS_PER_PAGE_COLUMN_NAME = "postsPerPage"
        private const val AMP_SUPPORTED_COLUMN_NAME = "ampSupported"
        private const val AMP_ENABLED_COLUMN_NAME = "ampEnabled"
        private const val PORTFOLIO_ENABLED_COLUMN_NAME = "portfolioEnabled"
        private const val PORTFOLIO_POSTS_PER_PAGE_COLUMN_NAME = "portfolioPostsPerPage"

        const val SETTINGS_TABLE_NAME = "site_settings"

        const val ADD_SHARING_LABEL = ("alter table $SETTINGS_TABLE_NAME add $SHARING_LABEL_COLUMN_NAME TEXT;")
        const val ADD_SHARING_BUTTON_STYLE = ("alter table $SETTINGS_TABLE_NAME " +
                "add $SHARING_BUTTON_STYLE_COLUMN_NAME TEXT;")
        const val ADD_ALLOW_REBLOG_BUTTON = ("alter table $SETTINGS_TABLE_NAME " +
                "add $ALLOW_REBLOG_BUTTON_COLUMN_NAME BOOLEAN;")
        const val ADD_ALLOW_LIKE_BUTTON = ("alter table $SETTINGS_TABLE_NAME " +
                "add $ALLOW_LIKE_BUTTON_COLUMN_NAME BOOLEAN;")
        const val ADD_ALLOW_COMMENT_LIKES = ("alter table $SETTINGS_TABLE_NAME " +
                "add $ALLOW_COMMENT_LIKES_COLUMN_NAME BOOLEAN;")
        const val ADD_TWITTER_USERNAME = ("alter table $SETTINGS_TABLE_NAME " +
                "add $TWITTER_USERNAME_COLUMN_NAME TEXT;")
        const val ADD_START_OF_WEEK = ("alter table $SETTINGS_TABLE_NAME " +
                "add $START_OF_WEEK_COLUMN_NAME TEXT;")
        const val ADD_TIME_FORMAT = ("alter table $SETTINGS_TABLE_NAME " +
                "add $TIME_FORMAT_COLUMN_NAME TEXT;")
        const val ADD_DATE_FORMAT = ("alter table $SETTINGS_TABLE_NAME " +
                "add $DATE_FORMAT_COLUMN_NAME TEXT;")
        const val ADD_TIMEZONE = ("alter table $SETTINGS_TABLE_NAME " +
                "add $TIMEZONE_COLUMN_NAME TEXT;")
        const val ADD_POSTS_PER_PAGE = ("alter table $SETTINGS_TABLE_NAME " +
                "add $POSTS_PER_PAGE_COLUMN_NAME INTEGER;")
        const val ADD_AMP_ENABLED = ("alter table $SETTINGS_TABLE_NAME " +
                "add $AMP_ENABLED_COLUMN_NAME BOOLEAN;")
        const val ADD_AMP_SUPPORTED = ("alter table $SETTINGS_TABLE_NAME " +
                "add $AMP_SUPPORTED_COLUMN_NAME BOOLEAN;")
        const val ADD_SITE_ICON = ("alter table $SETTINGS_TABLE_NAME " +
                "add $SITE_ICON_COLUMN_NAME INTEGER;")
        const val ADD_PORTFOLIO_ENABLED = ("alter table $SETTINGS_TABLE_NAME " +
                "add $PORTFOLIO_ENABLED_COLUMN_NAME BOOLEAN;")
        const val ADD_PORTFOLIO_POST_PER_PAGE = ("alter table $SETTINGS_TABLE_NAME " +
                "add $PORTFOLIO_POSTS_PER_PAGE_COLUMN_NAME INTEGER;")
        const val CREATE_SETTINGS_TABLE_SQL = ("CREATE TABLE IF NOT EXISTS $SETTINGS_TABLE_NAME (" +
                "$ID_COLUMN_NAME INTEGER PRIMARY KEY, $ADDRESS_COLUMN_NAME TEXT, $USERNAME_COLUMN_NAME TEXT, " +
                "$PASSWORD_COLUMN_NAME TEXT, $TITLE_COLUMN_NAME TEXT, $TAGLINE_COLUMN_NAME TEXT, " +
                "$LANGUAGE_COLUMN_NAME INTEGER, $PRIVACY_COLUMN_NAME INTEGER, $LOCATION_COLUMN_NAME BOOLEAN, " +
                "$DEF_CATEGORY_COLUMN_NAME TEXT, $DEF_POST_FORMAT_COLUMN_NAME TEXT, $CATEGORIES_COLUMN_NAME TEXT, " +
                "$POST_FORMATS_COLUMN_NAME TEXT, $CREDS_VERIFIED_COLUMN_NAME BOOLEAN, " +
                "$RELATED_POSTS_COLUMN_NAME INTEGER, $ALLOW_COMMENTS_COLUMN_NAME BOOLEAN, " +
                "$SEND_PINGBACKS_COLUMN_NAME BOOLEAN, $RECEIVE_PINGBACKS_COLUMN_NAME BOOLEAN, " +
                "$SHOULD_CLOSE_AFTER_COLUMN_NAME BOOLEAN, $CLOSE_AFTER_COLUMN_NAME INTEGER, " +
                "$SORT_BY_COLUMN_NAME INTEGER, $SHOULD_THREAD_COLUMN_NAME BOOLEAN, $THREADING_COLUMN_NAME INTEGER, " +
                "$SHOULD_PAGE_COLUMN_NAME BOOLEAN, $PAGING_COLUMN_NAME INTEGER, " +
                "$MANUAL_APPROVAL_COLUMN_NAME BOOLEAN, " + "$IDENTITY_REQUIRED_COLUMN_NAME BOOLEAN, " +
                "$USER_ACCOUNT_REQUIRED_COLUMN_NAME BOOLEAN, " + "$WHITELIST_COLUMN_NAME BOOLEAN, " +
                "$MODERATION_KEYS_COLUMN_NAME TEXT, $BLACKLIST_KEYS_COLUMN_NAME TEXT);")

        /**
         * Used to serialize post formats to store in a local database.
         *
         * @param formats map of post formats where the key is the format ID and the value is the format name
         * @return a String of semi-colon separated KVP's of Post Formats; Post Format ID -> Post Format Name
         */
        private fun postFormatList(formats: Map<String, String>?): String {
            if (formats == null || formats.isEmpty()) {
                return ""
            }

            val builder = StringBuilder()
            for (key in formats.keys) {
                builder.append(key).append(",").append(formats[key]).append(";")
            }
            builder.setLength(builder.length - 1)

            return builder.toString()
        }

        /**
         * Used to serialize categories to store in a local database.
         *
         * @param elements [CategoryModel] array to create String ID list from
         * @return a String of comma-separated integer Category ID's
         */
        private fun categoryIdList(elements: Array<CategoryModel>?): String {
            if (elements == null || elements.isEmpty()) {
                return ""
            }

            val builder = StringBuilder()
            for (element in elements) {
                builder.append(element.id.toString()).append(",")
            }
            builder.setLength(builder.length - 1)

            return builder.toString()
        }
    }
}
