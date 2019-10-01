package org.wordpress.android.fluxc.persistence;

import android.content.Context;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.preference.PreferenceManager;

import androidx.annotation.StringDef;

import com.yarolegovich.wellsql.DefaultWellConfig;
import com.yarolegovich.wellsql.WellSql;
import com.yarolegovich.wellsql.WellTableManager;
import com.yarolegovich.wellsql.core.Identifiable;
import com.yarolegovich.wellsql.core.TableClass;
import com.yarolegovich.wellsql.mapper.SQLiteMapper;

import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;

import static java.lang.annotation.RetentionPolicy.SOURCE;

public class WellSqlConfig extends DefaultWellConfig {
    @Retention(SOURCE)
    @StringDef({ADDON_WOOCOMMERCE})
    @Target(ElementType.PARAMETER)
    public @interface AddOn {}
    public static final String ADDON_WOOCOMMERCE = "WC";

    public WellSqlConfig(Context context) {
        super(context);
    }

    public WellSqlConfig(Context context, @AddOn String... addOns) {
        super(context, new HashSet<>(Arrays.asList(addOns)));
    }

    @Override
    public int getDbVersion() {
        return 88;
    }

    @Override
    public String getDbName() {
        return "wp-fluxc";
    }

    @Override
    public void onCreate(SQLiteDatabase db, WellTableManager helper) {
        for (Class<? extends Identifiable> table : mTables) {
            helper.createTable(table);
        }
    }

    @SuppressWarnings({"FallThrough", "MethodLength"})
    @Override
    public void onUpgrade(SQLiteDatabase db, WellTableManager helper, int oldVersion, int newVersion) {
        AppLog.d(T.DB, "Upgrading database from version " + oldVersion + " to " + newVersion);

        db.beginTransaction();
        switch (oldVersion) {
            case 1:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add ICON_URL text;");
                oldVersion++;
            case 2:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add FRAME_NONCE text;");
                oldVersion++;
            case 3:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table AccountModel add EMAIL_VERIFIED boolean;");
                oldVersion++;
            case 4:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add ORIGIN integer;");
                oldVersion++;
            case 5:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add HAS_FREE_PLAN boolean;");
                oldVersion++;
            case 6:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add UNMAPPED_URL text;");
                oldVersion++;
            case 7:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table MediaModel add LOCAL_POST_ID integer;");
                oldVersion++;
            case 8:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table MediaModel add FILE_URL_MEDIUM_SIZE text;");
                db.execSQL("alter table MediaModel add FILE_URL_MEDIUM_LARGE_SIZE text;");
                db.execSQL("alter table MediaModel add FILE_URL_LARGE_SIZE text;");
                oldVersion++;
            case 9:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add MAX_UPLOAD_SIZE integer;");
                oldVersion++;
            case 10:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add MEMORY_LIMIT integer;");
                oldVersion++;
            case 11:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("CREATE TABLE RoleModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,SITE_ID INTEGER,"
                           + "NAME TEXT,DISPLAY_NAME TEXT)");
                oldVersion++;
            case 12:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("CREATE TABLE PluginModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                           + "NAME TEXT,DISPLAY_NAME TEXT,PLUGIN_URL TEXT,VERSION TEXT,SLUG TEXT,DESCRIPTION TEXT,"
                           + "AUTHOR_NAME TEXT,AUTHOR_URL TEXT,IS_ACTIVE INTEGER,IS_AUTO_UPDATE_ENABLED INTEGER)");
                oldVersion++;
            case 13:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("CREATE TABLE PluginInfoModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "NAME TEXT,SLUG TEXT,VERSION TEXT,RATING TEXT,ICON TEXT)");
                oldVersion++;
            case 14:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("CREATE TABLE MediaUploadModel (_id INTEGER PRIMARY KEY,UPLOAD_STATE INTEGER,"
                           + "PROGRESS REAL,ERROR_TYPE TEXT,ERROR_MESSAGE TEXT,FOREIGN KEY(_id) REFERENCES "
                           + "MediaModel(_id) ON DELETE CASCADE)");
                db.execSQL("CREATE TABLE PostUploadModel (_id INTEGER PRIMARY KEY,UPLOAD_STATE INTEGER,"
                           + "ASSOCIATED_MEDIA_IDS TEXT,ERROR_TYPE TEXT,ERROR_MESSAGE TEXT,FOREIGN KEY(_id) REFERENCES "
                           + "PostModel(_id) ON DELETE CASCADE)");
                oldVersion++;
            case 15:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("CREATE TABLE ThemeModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                           + "THEME_ID TEXT,NAME TEXT,DESCRIPTION TEXT,SLUG TEXT,VERSION TEXT,AUTHOR_NAME TEXT,"
                           + "AUTHOR_URL TEXT,THEME_URL TEXT,SCREENSHOT_URL TEXT,DEMO_URL TEXT,DOWNLOAD_URL TEXT,"
                           + "STYLESHEET TEXT,CURRENCY TEXT,PRICE REAL,ACTIVE INTEGER,AUTO_UPDATE INTEGER,"
                           + "AUTO_UPDATE_TRANSLATION INTEGER,IS_WP_COM_THEME INTEGER)");
                oldVersion++;
            case 16:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table ThemeModel add FREE integer;");
                db.execSQL("alter table ThemeModel add PRICE_TEXT integer;");
                oldVersion++;
            case 17:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add EMAIL text;");
                db.execSQL("alter table SiteModel add DISPLAY_NAME text;");
                oldVersion++;
            case 18:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add JETPACK_VERSION text;");
                oldVersion++;
            case 19:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table TermModel add POST_COUNT integer;");
                oldVersion++;
            case 20:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table PluginModel rename to SitePluginModel;");
                db.execSQL("alter table PluginInfoModel rename to WPOrgPluginModel;");
                oldVersion++;
            case 21:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SitePluginModel add SETTINGS_URL text;");
                db.execSQL("alter table WPOrgPluginModel add AUTHOR_AS_HTML TEXT;");
                db.execSQL("alter table WPOrgPluginModel add BANNER TEXT;");
                db.execSQL("alter table WPOrgPluginModel add DESCRIPTION_AS_HTML TEXT;");
                db.execSQL("alter table WPOrgPluginModel add FAQ_AS_HTML TEXT;");
                db.execSQL("alter table WPOrgPluginModel add HOMEPAGE_URL TEXT;");
                db.execSQL("alter table WPOrgPluginModel add INSTALLATION_INSTRUCTIONS_AS_HTML TEXT;");
                db.execSQL("alter table WPOrgPluginModel add LAST_UPDATED TEXT;");
                db.execSQL("alter table WPOrgPluginModel add REQUIRED_WORD_PRESS_VERSION TEXT;");
                db.execSQL("alter table WPOrgPluginModel add WHATS_NEW_AS_HTML TEXT;");
                db.execSQL("alter table WPOrgPluginModel add DOWNLOAD_COUNT INTEGER;");
                db.execSQL("alter table WPOrgPluginModel add NUMBER_OF_RATINGS INTEGER;");
                db.execSQL("alter table WPOrgPluginModel add NUMBER_OF_RATINGS_OF_ONE INTEGER;");
                db.execSQL("alter table WPOrgPluginModel add NUMBER_OF_RATINGS_OF_TWO INTEGER;");
                db.execSQL("alter table WPOrgPluginModel add NUMBER_OF_RATINGS_OF_THREE INTEGER;");
                db.execSQL("alter table WPOrgPluginModel add NUMBER_OF_RATINGS_OF_FOUR INTEGER;");
                db.execSQL("alter table WPOrgPluginModel add NUMBER_OF_RATINGS_OF_FIVE INTEGER;");
                oldVersion++;
            case 22:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table ThemeModel add MOBILE_FRIENDLY_CATEGORY_SLUG text;");
                oldVersion++;
            case 23:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("CREATE TABLE PluginDirectoryModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "SLUG TEXT,DIRECTORY_TYPE TEXT,PAGE INTEGER)");
                oldVersion++;
            case 24:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                // Start with a clean slate for Plugins. This migration adds unique constraints for SitePluginModel
                // and WPOrgPluginModel tables. Adds `authorName` column and renames `name` column to `displayName` in
                // WPOrgPluginModel table. Since these records are only used as cache and would & should be refreshed
                // often, there is no real harm to do this other than a slightly longer loading time for the first usage
                // after the migration. This migration would be much more complicated otherwise.
                db.execSQL("DELETE FROM PluginDirectoryModel");
                db.execSQL("DROP TABLE IF EXISTS SitePluginModel");
                db.execSQL("DROP TABLE IF EXISTS WPOrgPluginModel");
                db.execSQL("CREATE TABLE SitePluginModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                           + "NAME TEXT,DISPLAY_NAME TEXT,PLUGIN_URL TEXT,VERSION TEXT,SLUG TEXT,DESCRIPTION TEXT,"
                           + "AUTHOR_NAME TEXT,AUTHOR_URL TEXT,SETTINGS_URL TEXT,IS_ACTIVE INTEGER,"
                           + "IS_AUTO_UPDATE_ENABLED INTEGER,UNIQUE (SLUG, LOCAL_SITE_ID))");
                db.execSQL("CREATE TABLE WPOrgPluginModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,AUTHOR_AS_HTML TEXT,"
                           + "AUTHOR_NAME TEXT,BANNER TEXT,DESCRIPTION_AS_HTML TEXT,DISPLAY_NAME TEXT,FAQ_AS_HTML TEXT,"
                           + "HOMEPAGE_URL TEXT,ICON TEXT,INSTALLATION_INSTRUCTIONS_AS_HTML TEXT,LAST_UPDATED TEXT,"
                           + "RATING TEXT,REQUIRED_WORD_PRESS_VERSION TEXT,SLUG TEXT,VERSION TEXT,WHATS_NEW_AS_HTML "
                           + "TEXT,"
                           + "DOWNLOAD_COUNT INTEGER,NUMBER_OF_RATINGS INTEGER,NUMBER_OF_RATINGS_OF_ONE INTEGER,"
                           + "NUMBER_OF_RATINGS_OF_TWO INTEGER,NUMBER_OF_RATINGS_OF_THREE INTEGER,"
                           + "NUMBER_OF_RATINGS_OF_FOUR INTEGER,NUMBER_OF_RATINGS_OF_FIVE INTEGER,UNIQUE (SLUG))");
                oldVersion++;
            case 25:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add SPACE_AVAILABLE INTEGER");
                db.execSQL("alter table SiteModel add SPACE_ALLOWED INTEGER");
                db.execSQL("alter table SiteModel add SPACE_USED INTEGER");
                db.execSQL("alter table SiteModel add SPACE_PERCENT_USED REAL");
                oldVersion++;
            case 26:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("ALTER TABLE SiteModel ADD IS_WP_COM_STORE INTEGER");
                db.execSQL("ALTER TABLE SiteModel ADD HAS_WOO_COMMERCE INTEGER");
                oldVersion++;
            case 27:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table AccountModel add TRACKS_OPT_OUT boolean;");
                oldVersion++;
            case 28:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL(
                        "CREATE TABLE ActivityLogModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "REMOTE_SITE_ID INTEGER,ACTIVITY_ID TEXT NOT NULL,SUMMARY TEXT NOT NULL,TEXT TEXT NOT NULL,"
                        + "NAME TEXT,TYPE TEXT,GRIDICON TEXT,STATUS TEXT,REWINDABLE INTEGER,REWIND_ID TEXT,PUBLISHED "
                        + "TEXT NOT NULL,DISCARDED INTEGER,DISPLAY_NAME TEXT,ACTOR_TYPE TEXT,WPCOM_USER_ID INTEGER,"
                        + "AVATAR_URL TEXT,ROLE TEXT)");
                db.execSQL(
                        "CREATE TABLE RewindStatus (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "REMOTE_SITE_ID INTEGER,REWIND_STATE TEXT,REASON TEXT,RESTORE_ID TEXT,RESTORE_STATE TEXT,"
                        + "RESTORE_PROGRESS INTEGER,RESTORE_MESSAGE TEXT,RESTORE_ERROR_CODE TEXT,"
                        + "RESTORE_FAILURE_REASON TEXT)");
                oldVersion++;
            case 29:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("CREATE TABLE SubscriptionModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "SUBSCRIPTION_ID TEXT,BLOG_ID TEXT,BLOG_NAME TEXT,FEED_ID TEXT,URL TEXT,"
                           + "SHOULD_NOTIFY_POSTS INTEGER,SHOULD_EMAIL_POSTS INTEGER,EMAIL_POSTS_FREQUENCY TEXT,"
                           + "SHOULD_EMAIL_COMMENTS INTEGER)");
                oldVersion++;
            case 30:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS ActivityLogModel");
                db.execSQL(
                        "CREATE TABLE IF NOT EXISTS ActivityLog (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID "
                        + "INTEGER,REMOTE_SITE_ID INTEGER,ACTIVITY_ID TEXT NOT NULL,SUMMARY TEXT NOT NULL,TEXT TEXT "
                        + "NOT NULL,NAME TEXT,TYPE TEXT,GRIDICON TEXT,STATUS TEXT,REWINDABLE INTEGER,REWIND_ID TEXT,"
                        + "PUBLISHED INTEGER,DISCARDED INTEGER,DISPLAY_NAME TEXT,ACTOR_TYPE TEXT,WPCOM_USER_ID "
                        + "INTEGER,AVATAR_URL TEXT,ROLE TEXT)");
                oldVersion++;
            case 31:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 32:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS RewindStatus");
                db.execSQL(
                        "CREATE TABLE RewindStatus (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "REMOTE_SITE_ID INTEGER,STATE TEXT NOT NULL,LAST_UPDATED INTEGER,REASON TEXT,"
                        + "CAN_AUTOCONFIGURE INTEGER,REWIND_ID TEXT,REWIND_STATUS TEXT,REWIND_STARTED_AT INTEGER,"
                        + "REWIND_PROGRESS INTEGER,REWIND_REASON TEXT)");
                db.execSQL(
                        "CREATE TABLE RewindStatusCredentials (_id INTEGER PRIMARY KEY AUTOINCREMENT,REWIND_STATE_ID "
                        + "INTEGER,TYPE TEXT NOT NULL,ROLE TEXT NOT NULL,STILL_VALID INTEGER,HOST TEXT,PORT INTEGER)");
                oldVersion++;
            case 33:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS RewindStatusCredentials");
                db.execSQL(
                        "CREATE TABLE RewindStatusCredentials (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID "
                        + "INTEGER,REMOTE_SITE_ID INTEGER,REWIND_STATE_ID INTEGER,TYPE TEXT NOT NULL,ROLE TEXT NOT "
                        + "NULL,STILL_VALID INTEGER,HOST TEXT,PORT INTEGER)");
                oldVersion++;
            case 34:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS RewindStatus");
                db.execSQL(
                        "CREATE TABLE RewindStatus (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "REMOTE_SITE_ID INTEGER,STATE TEXT NOT NULL,LAST_UPDATED INTEGER,REASON TEXT,"
                        + "CAN_AUTOCONFIGURE INTEGER,REWIND_ID TEXT,REWIND_STATUS TEXT,REWIND_PROGRESS INTEGER,"
                        + "REWIND_REASON TEXT)");
                oldVersion++;
            case 35:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 36:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS RewindStatus");
                db.execSQL(
                        "CREATE TABLE RewindStatus (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "REMOTE_SITE_ID INTEGER,STATE TEXT NOT NULL,LAST_UPDATED INTEGER,REASON TEXT,"
                        + "CAN_AUTOCONFIGURE INTEGER,REWIND_ID TEXT,RESTORE_ID INTEGER,REWIND_STATUS TEXT,"
                        + "REWIND_PROGRESS INTEGER,REWIND_REASON TEXT)");
                oldVersion++;
            case 37:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS QuickStartModel");
                db.execSQL("CREATE TABLE QuickStartTaskModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "SITE_ID INTEGER,TASK_NAME TEXT,IS_DONE INTEGER,IS_SHOWN INTEGER)");
                db.execSQL("CREATE TABLE QuickStartStatusModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "SITE_ID INTEGER,IS_COMPLETED INTEGER,IS_NOTIFICATION_RECEIVED INTEGER)");
                oldVersion++;
            case 38:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                SharedPreferences defaultSharedPrefs = PreferenceManager.getDefaultSharedPreferences(getContext());
                String token = defaultSharedPrefs.getString("ACCOUNT_TOKEN_PREF_KEY", "");
                if (!token.isEmpty()) {
                    AppLog.d(T.DB, "Migrating token to fluxc-preferences");
                    SharedPreferences fluxCPreferences = getContext().getSharedPreferences(
                            getContext().getPackageName() + "_fluxc-preferences", Context.MODE_PRIVATE);
                    fluxCPreferences.edit().putString("ACCOUNT_TOKEN_PREF_KEY", token).apply();
                    defaultSharedPrefs.edit().remove("ACCOUNT_TOKEN_PREF_KEY").apply();
                }
                oldVersion++;
            case 39:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS QuickStartModel");
                db.execSQL("CREATE TABLE IF NOT EXISTS QuickStartTaskModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "SITE_ID INTEGER,TASK_NAME TEXT,IS_DONE INTEGER,IS_SHOWN INTEGER)");
                db.execSQL("CREATE TABLE IF NOT EXISTS QuickStartStatusModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "SITE_ID INTEGER,IS_COMPLETED INTEGER,IS_NOTIFICATION_RECEIVED INTEGER)");
                oldVersion++;
            case 40:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS ActivityLog");
                db.execSQL(
                        "CREATE TABLE ActivityLog (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "REMOTE_SITE_ID INTEGER,ACTIVITY_ID TEXT NOT NULL,SUMMARY TEXT NOT NULL,FORMATTABLE_CONTENT"
                        + " TEXT NOT NULL,NAME TEXT,TYPE TEXT,GRIDICON TEXT,STATUS TEXT,REWINDABLE INTEGER,REWIND_ID "
                        + "TEXT,PUBLISHED INTEGER,DISCARDED INTEGER,DISPLAY_NAME TEXT,ACTOR_TYPE TEXT,WPCOM_USER_ID "
                        + "INTEGER,AVATAR_URL TEXT,ROLE TEXT)");
                oldVersion++;
            case 41:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("CREATE TABLE LocalDiffModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "REVISION_ID INTEGER,POST_ID INTEGER,SITE_ID INTEGER,OPERATION TEXT,VALUE TEXT,"
                           + "DIFF_TYPE TEXT)");
                db.execSQL("CREATE TABLE LocalRevisionModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "REVISION_ID INTEGER,POST_ID INTEGER,SITE_ID INTEGER,DIFF_FROM_VERSION INTEGER,"
                           + "TOTAL_ADDITIONS INTEGER,TOTAL_DELETIONS INTEGER,POST_CONTENT TEXT,POST_EXCERPT TEXT,"
                           + "POST_TITLE TEXT,POST_DATE_GMT TEXT,POST_MODIFIED_GMT TEXT,POST_AUTHOR_ID TEXT)");
                oldVersion++;
            case 42:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL(
                        "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,TYPE "
                        + "TEXT NOT NULL,JSON TEXT NOT NULL)");
                oldVersion++;
            case 43:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("CREATE TABLE ListModel (LAST_MODIFIED TEXT,DESCRIPTOR_UNIQUE_IDENTIFIER_DB_VALUE INTEGER,"
                           + "DESCRIPTOR_TYPE_IDENTIFIER_DB_VALUE INTEGER,STATE_DB_VALUE INTEGER,"
                           + "_id INTEGER PRIMARY KEY AUTOINCREMENT)");
                db.execSQL("CREATE TABLE ListItemModel (LIST_ID INTEGER,REMOTE_ITEM_ID INTEGER,_id INTEGER PRIMARY KEY "
                           + "AUTOINCREMENT,FOREIGN KEY(LIST_ID) REFERENCES ListModel(_id) ON DELETE CASCADE,"
                           + "UNIQUE(LIST_ID, REMOTE_ITEM_ID) ON CONFLICT IGNORE)");
                db.execSQL("ALTER TABLE PostModel ADD LAST_MODIFIED TEXT");
                oldVersion++;
            case 44:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS StatsBlock");
                db.execSQL(
                        "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,JSON TEXT NOT NULL)");
                oldVersion++;
            case 45:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                db.execSQL("CREATE TABLE NotificationModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "REMOTE_NOTE_ID INTEGER,LOCAL_SITE_ID INTEGER,NOTE_HASH INTEGER,TYPE TEXT,"
                           + "SUBTYPE TEXT,READ INTEGER,ICON TEXT,NOTICON TEXT,TIMESTAMP TEXT,URL TEXT,"
                           + "TITLE TEXT,FORMATTABLE_BODY TEXT,FORMATTABLE_SUBJECT TEXT,FORMATTABLE_META TEXT)");
                oldVersion++;
            case 46:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS StatsBlock");
                db.execSQL(
                        "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT NOT NULL,JSON TEXT NOT NULL)");
                oldVersion++;
            case 47:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS StatsBlock");
                db.execSQL(
                        "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT NOT NULL,JSON TEXT NOT NULL)");
                oldVersion++;
            case 48:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("ALTER TABLE PostModel ADD REMOTE_LAST_MODIFIED TEXT");
                oldVersion++;
            case 49:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 50:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL(
                        "CREATE TABLE PlanOffers (_id INTEGER PRIMARY KEY AUTOINCREMENT,INTERNAL_PLAN_ID INTEGER,"
                        + "NAME TEXT,SHORT_NAME TEXT,TAGLINE TEXT,DESCRIPTION TEXT,ICON TEXT)");
                db.execSQL(
                        "CREATE TABLE PlanOffersId (_id INTEGER PRIMARY KEY AUTOINCREMENT,PRODUCT_ID INTEGER,"
                        + "INTERNAL_PLAN_ID INTEGER)");
                db.execSQL(
                        "CREATE TABLE PlanOffersFeature (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                        + "INTERNAL_PLAN_ID INTEGER,STRING_ID TEXT UNIQUE,NAME TEXT,DESCRIPTION TEXT)");
                oldVersion++;
            case 51:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 52:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("CREATE TABLE PlanOffersFeatureTemp (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "INTERNAL_PLAN_ID INTEGER,STRING_ID TEXT,NAME TEXT,DESCRIPTION TEXT)");
                db.execSQL("INSERT INTO PlanOffersFeatureTemp SELECT * FROM PlanOffersFeature");
                db.execSQL("DROP TABLE PlanOffersFeature");
                db.execSQL("ALTER TABLE PlanOffersFeatureTemp RENAME TO PlanOffersFeature");
                oldVersion++;
            case 53:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("ALTER TABLE QuickStartTaskModel ADD TASK_TYPE TEXT");
                oldVersion++;
            case 54:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 55:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 56:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 57:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 58:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 59:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS StatsBlock");
                db.execSQL(
                        "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT,POST_ID INTEGER,JSON "
                        + "TEXT NOT NULL)");
                oldVersion++;
            case 60:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE StatsBlock");
                db.execSQL(
                        "CREATE TABLE StatsBlock (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT,POST_ID INTEGER,JSON TEXT NOT "
                        + "NULL)");
                db.execSQL(
                        "CREATE TABLE StatsRequest (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT,TIME_STAMP INTEGER,"
                        + "REQUESTED_ITEMS INTEGER)");
                oldVersion++;
            case 61:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                db.execSQL("DROP TABLE StatsRequest");
                db.execSQL(
                        "CREATE TABLE StatsRequest (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT,POST_ID INTEGER,TIME_STAMP "
                        + "INTEGER,REQUESTED_ITEMS INTEGER)");
                oldVersion++;
            case 62:
                db.execSQL("DROP TABLE StatsRequest");
                db.execSQL(
                        "CREATE TABLE StatsRequest (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "BLOCK_TYPE TEXT NOT NULL,STATS_TYPE TEXT NOT NULL,DATE TEXT,POST_ID INTEGER,TIME_STAMP "
                        + "INTEGER,REQUESTED_ITEMS INTEGER)");
                oldVersion++;
            case 63:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 64:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 65:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 66:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL(
                        "CREATE TABLE InsightTypes (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "REMOTE_SITE_ID INTEGER,INSIGHT_TYPE TEXT NOT NULL,POSITION INTEGER,STATUS TEXT NOT NULL)");
                oldVersion++;
            case 67:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 68:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS NotificationModel");
                db.execSQL("CREATE TABLE NotificationModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "REMOTE_NOTE_ID INTEGER,LOCAL_SITE_ID INTEGER,NOTE_HASH INTEGER,TYPE TEXT,"
                           + "SUBTYPE TEXT,READ INTEGER,ICON TEXT,NOTICON TEXT,TIMESTAMP TEXT,URL TEXT,"
                           + "TITLE TEXT,FORMATTABLE_BODY TEXT,FORMATTABLE_SUBJECT TEXT,FORMATTABLE_META TEXT, "
                           + "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE)");
                oldVersion++;
            case 69:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 70:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS NotificationModel");
                db.execSQL("CREATE TABLE NotificationModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "REMOTE_NOTE_ID INTEGER,LOCAL_SITE_ID INTEGER,NOTE_HASH INTEGER,TYPE TEXT,"
                           + "SUBTYPE TEXT,READ INTEGER,ICON TEXT,NOTICON TEXT,TIMESTAMP TEXT,URL TEXT,"
                           + "TITLE TEXT,FORMATTABLE_BODY TEXT,FORMATTABLE_SUBJECT TEXT,FORMATTABLE_META TEXT)");
                oldVersion++;
            case 71:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("ALTER TABLE MediaModel ADD MARKED_LOCALLY_AS_FEATURED INTEGER");
                oldVersion++;
            case 72:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("ALTER TABLE PostUploadModel ADD NUMBER_OF_UPLOAD_ERRORS_OR_CANCELLATIONS INTEGER");
                oldVersion++;
            case 73:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("DROP TABLE IF EXISTS NotificationModel");
                db.execSQL("CREATE TABLE NotificationModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                           + "REMOTE_NOTE_ID INTEGER,REMOTE_SITE_ID INTEGER,NOTE_HASH INTEGER,TYPE TEXT,"
                           + "SUBTYPE TEXT,READ INTEGER,ICON TEXT,NOTICON TEXT,TIMESTAMP TEXT,URL TEXT,"
                           + "TITLE TEXT,FORMATTABLE_BODY TEXT,FORMATTABLE_SUBJECT TEXT,FORMATTABLE_META TEXT)");
                oldVersion++;
            case 74:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table SiteModel add WEB_EDITOR TEXT;");
                db.execSQL("alter table SiteModel add MOBILE_EDITOR TEXT;");
                oldVersion++;
            case 75:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 76:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL(
                        "CREATE TABLE PostSchedulingReminder (_id INTEGER PRIMARY KEY AUTOINCREMENT,POST_ID INTEGER,"
                        + "SCHEDULED_TIME TEXT NOT NULL)");
                oldVersion++;
            case 77:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table PostModel add AUTHOR_ID INTEGER;");
                db.execSQL("alter table PostModel add AUTHOR_DISPLAY_NAME TEXT;");
                oldVersion++;
            case 78:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 79:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("alter table PostModel add CHANGES_CONFIRMED_CONTENT_HASHCODE INTEGER;");
                oldVersion++;
            case 80:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 81:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 82:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 83:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
            case 84:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("ALTER TABLE AccountModel ADD USERNAME_CAN_BE_CHANGED boolean");
                oldVersion++;
            case 85:
                db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_REVISION_ID INTEGER");
                db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_MODIFIED TEXT");
                db.execSQL("ALTER TABLE PostModel ADD REMOTE_AUTO_SAVE_MODIFIED TEXT");
                db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_PREVIEW_URL TEXT");
                db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_TITLE TEXT");
                db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_CONTENT TEXT");
                db.execSQL("ALTER TABLE PostModel ADD AUTO_SAVE_EXCERPT TEXT");
                oldVersion++;
            case 86:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                db.execSQL("ALTER TABLE PostUploadModel ADD NUMBER_OF_AUTO_UPLOAD_ATTEMPTS INTEGER");
                oldVersion++;
            case 87:
                AppLog.d(T.DB, "Migrating to version " + (oldVersion + 1));
                migrateAddOn(ADDON_WOOCOMMERCE, db, oldVersion);
                oldVersion++;
        }
        db.setTransactionSuccessful();
        db.endTransaction();
    }

    @Override
    public void onConfigure(SQLiteDatabase db, WellTableManager helper) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            db.setForeignKeyConstraintsEnabled(true);
        } else {
            db.execSQL("PRAGMA foreign_keys=ON;");
        }
    }

    @Override
    protected Map<Class<? extends Identifiable>, SQLiteMapper<?>> registerMappers() {
        return super.registerMappers();
    }

    /**
     * Drop and create all tables
     */
    public void reset() {
        SQLiteDatabase db = WellSql.giveMeWritableDb();
        for (Class<? extends Identifiable> clazz : mTables) {
            TableClass table = getTable(clazz);
            db.execSQL("DROP TABLE IF EXISTS " + table.getTableName());
            db.execSQL(table.createStatement());
        }
    }

    /**
     * Recreates all the tables in this database - similar to the above but can be used from onDowngrade where we can't
     * call giveMeWritableDb (attempting to do so results in "IllegalStateException: getDatabase called recursively")
     */
    @SuppressWarnings("unused")
    public void reset(WellTableManager helper) {
        AppLog.d(T.DB, "resetting tables");
        for (Class<? extends Identifiable> table : mTables) {
            AppLog.d(T.DB, "dropping table " + table.getSimpleName());
            helper.dropTable(table);
            AppLog.d(T.DB, "creating table " + table.getSimpleName());
            helper.createTable(table);
        }
    }


    @SuppressWarnings({"MethodLength"})
    private void migrateAddOn(@AddOn String addOnName, SQLiteDatabase db, int oldDbVersion) {
        if (mActiveAddOns.contains(addOnName)) {
            switch (oldDbVersion) {
                case 31:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("DROP TABLE IF EXISTS WCOrderModel");
                    db.execSQL("DROP TABLE IF EXISTS WCOrderNoteModel");
                    db.execSQL("CREATE TABLE WCOrderModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                               + "REMOTE_ORDER_ID INTEGER,NUMBER TEXT NOT NULL,STATUS TEXT NOT NULL,"
                               + "CURRENCY TEXT NOT NULL,DATE_CREATED TEXT NOT NULL,TOTAL TEXT NOT NULL,"
                               + "TOTAL_TAX TEXT NOT NULL,SHIPPING_TOTAL TEXT NOT NULL,PAYMENT_METHOD TEXT NOT NULL,"
                               + "PAYMENT_METHOD_TITLE TEXT NOT NULL,PRICES_INCLUDE_TAX INTEGER,"
                               + "CUSTOMER_NOTE TEXT NOT NULL,DISCOUNT_TOTAL TEXT NOT NULL,"
                               + "DISCOUNT_CODES TEXT NOT NULL,REFUND_TOTAL REAL,BILLING_FIRST_NAME TEXT NOT NULL,"
                               + "BILLING_LAST_NAME TEXT NOT NULL,BILLING_COMPANY TEXT NOT NULL,"
                               + "BILLING_ADDRESS1 TEXT NOT NULL,BILLING_ADDRESS2 TEXT NOT NULL,"
                               + "BILLING_CITY TEXT NOT NULL,BILLING_STATE TEXT NOT NULL,"
                               + "BILLING_POSTCODE TEXT NOT NULL,BILLING_COUNTRY TEXT NOT NULL,"
                               + "BILLING_EMAIL TEXT NOT NULL,BILLING_PHONE TEXT NOT NULL,"
                               + "SHIPPING_FIRST_NAME TEXT NOT NULL,SHIPPING_LAST_NAME TEXT NOT NULL,"
                               + "SHIPPING_COMPANY TEXT NOT NULL,SHIPPING_ADDRESS1 TEXT NOT NULL,"
                               + "SHIPPING_ADDRESS2 TEXT NOT NULL,SHIPPING_CITY TEXT NOT NULL,"
                               + "SHIPPING_STATE TEXT NOT NULL,SHIPPING_POSTCODE TEXT NOT NULL,"
                               + "SHIPPING_COUNTRY TEXT NOT NULL,LINE_ITEMS TEXT NOT NULL)");
                    db.execSQL("CREATE TABLE WCOrderNoteModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "LOCAL_SITE_ID INTEGER,LOCAL_ORDER_ID INTEGER,REMOTE_NOTE_ID INTEGER,"
                               + "DATE_CREATED TEXT NOT NULL,NOTE TEXT NOT NULL,IS_CUSTOMER_NOTE INTEGER)");
                    break;
                case 35:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("CREATE TABLE WCOrderStatsModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "LOCAL_SITE_ID INTEGER,UNIT TEXT NOT NULL,FIELDS TEXT NOT NULL,DATA TEXT NOT NULL)");
                    break;
                case 45:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("ALTER TABLE WCOrderNoteModel ADD IS_SYSTEM_NOTE INTEGER");
                    break;
                case 49:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("CREATE TABLE WCSettingsModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "LOCAL_SITE_ID INTEGER,CURRENCY_CODE TEXT NOT NULL,CURRENCY_POSITION TEXT NOT NULL,"
                               + "CURRENCY_THOUSAND_SEPARATOR TEXT NOT NULL,CURRENCY_DECIMAL_SEPARATOR TEXT NOT NULL,"
                               + "CURRENCY_DECIMAL_NUMBER INTEGER)");
                    break;
                case 51:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("CREATE TABLE WCOrderStatusModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "LOCAL_SITE_ID INTEGER,STATUS_KEY TEXT NOT NULL,LABEL TEXT NOT NULL)");
                    break;
                case 54:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("ALTER TABLE WCOrderStatsModel ADD IS_CUSTOM_FIELD INTEGER");
                    db.execSQL("ALTER TABLE WCOrderStatsModel ADD DATE TEXT");
                    db.execSQL("ALTER TABLE WCOrderStatsModel ADD ENDDATE TEXT");
                    db.execSQL("ALTER TABLE WCOrderStatsModel ADD STARTDATE TEXT");
                    db.execSQL("ALTER TABLE WCOrderStatsModel ADD QUANTITY TEXT");
                    break;
                case 55:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("DROP TABLE IF EXISTS WCOrderStatsModel");
                    db.execSQL("CREATE TABLE WCOrderStatsModel(\n"
                               + "  LOCAL_SITE_ID INTEGER,\n"
                               + "  UNIT TEXT NOT NULL,\n"
                               + "  DATE TEXT NOT NULL,\n"
                               + "  START_DATE TEXT NOT NULL,\n"
                               + "  END_DATE TEXT NOT NULL,\n"
                               + "  QUANTITY TEXT NOT NULL,\n"
                               + "  IS_CUSTOM_FIELD INTEGER,\n"
                               + "  FIELDS TEXT NOT NULL,\n"
                               + "  DATA TEXT NOT NULL,\n"
                               + "  _id INTEGER PRIMARY KEY AUTOINCREMENT)");
                    break;
                case 56:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("DROP TABLE IF EXISTS WCProductModel");
                    db.execSQL("CREATE TABLE WCProductModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "LOCAL_SITE_ID INTEGER,REMOTE_PRODUCT_ID INTEGER,"
                               + "NAME TEXT NOT NULL,SLUG TEXT NOT NULL,PERMALINK TEXT NOT NULL,"
                               + "DATE_CREATED TEXT NOT NULL,DATE_MODIFIED TEXT NOT NULL,"
                               + "TYPE TEXT NOT NULL,STATUS TEXT NOT NULL,FEATURED INTEGER,"
                               + "CATALOG_VISIBILITY TEXT NOT NULL,DESCRIPTION TEXT NOT NULL,"
                               + "SHORT_DESCRIPTION TEXT NOT NULL,SKU TEXT NOT NULL,"
                               + "PRICE TEXT NOT NULL,REGULAR_PRICE TEXT NOT NULL, SALE_PRICE TEXT NOT NULL,"
                               + "ON_SALE INTEGER,TOTAL_SALES INTEGER,VIRTUAL INTEGER,DOWNLOADABLE INTEGER,"
                               + "TAX_STATUS TEXT NOT NULL,TAX_CLASS TEXT NOT NULL,"
                               + "MANAGE_STOCK INTEGER,STOCK_QUANTITY INTEGER,STOCK_STATUS TEXT NOT NULL,"
                               + "BACKORDERS TEXT NOT NULL,BACKORDERS_ALLOWED INTEGER,BACKORDERED INTEGER,"
                               + "SOLD_INDIVIDUALLY INTEGER,WEIGHT TEXT NOT NULL,LENGTH TEXT NOT NULL,"
                               + "WIDTH TEXT NOT NULL,HEIGHT TEXT NOT NULL,SHIPPING_REQUIRED INTEGER,"
                               + "SHIPPING_TAXABLE INTEGER,SHIPPING_CLASS TEXT NOT NULL,"
                               + "SHIPPING_CLASS_ID INTEGER,REVIEWS_ALLOWED INTEGER,AVERAGE_RATING TEXT NOT NULL,"
                               + "RATING_COUNT INTEGER,PARENT_ID INTEGER,PURCHASE_NOTE TEXT NOT NULL,"
                               + "CATEGORIES TEXT NOT NULL,TAGS TEXT NOT NULL,"
                               + "IMAGES TEXT NOT NULL,ATTRIBUTES TEXT NOT NULL,"
                               + "VARIATIONS TEXT NOT NULL)");
                    break;
                case 57:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("DELETE FROM WCOrderStatsModel");
                    break;
                case 58:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("DROP TABLE IF EXISTS WCProductVariationModel");
                    db.execSQL("CREATE TABLE WCProductVariationModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "LOCAL_SITE_ID INTEGER,"
                               + "REMOTE_PRODUCT_ID INTEGER,"
                               + "REMOTE_VARIATION_ID INTEGER,"
                               + "DATE_CREATED TEXT NOT NULL,"
                               + "DATE_MODIFIED TEXT NOT NULL,"
                               + "DESCRIPTION TEXT NOT NULL,"
                               + "PERMALINK TEXT NOT NULL,"
                               + "SKU TEXT NOT NULL,"
                               + "STATUS TEXT NOT NULL,"
                               + "PRICE TEXT NOT NULL,"
                               + "REGULAR_PRICE TEXT NOT NULL,"
                               + "SALE_PRICE TEXT NOT NULL,"
                               + "ON_SALE INTEGER,"
                               + "PURCHASABLE INTEGER,"
                               + "VIRTUAL INTEGER,"
                               + "DOWNLOADABLE INTEGER,"
                               + "MANAGE_STOCK INTEGER,"
                               + "STOCK_QUANTITY INTEGER,"
                               + "STOCK_STATUS TEXT NOT NULL,"
                               + "IMAGE_URL TEXT NOT NULL,"
                               + "WEIGHT TEXT NOT NULL,"
                               + "LENGTH TEXT NOT NULL,"
                               + "WIDTH TEXT NOT NULL,"
                               + "HEIGHT TEXT NOT NULL,"
                               + "ATTRIBUTES TEXT NOT NULL)");
                    break;
                case 61:
                    db.execSQL("DROP TABLE IF EXISTS WCProductModel");
                    db.execSQL("CREATE TABLE WCProductModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "LOCAL_SITE_ID INTEGER,"
                               + "REMOTE_PRODUCT_ID INTEGER,"
                               + "NAME TEXT NOT NULL,"
                               + "SLUG TEXT NOT NULL,"
                               + "PERMALINK TEXT NOT NULL,"
                               + "DATE_CREATED TEXT NOT NULL,"
                               + "DATE_MODIFIED TEXT NOT NULL,"
                               + "TYPE TEXT NOT NULL,"
                               + "STATUS TEXT NOT NULL,"
                               + "FEATURED INTEGER,"
                               + "CATALOG_VISIBILITY TEXT NOT NULL,"
                               + "DESCRIPTION TEXT NOT NULL,"
                               + "SHORT_DESCRIPTION TEXT NOT NULL,"
                               + "SKU TEXT NOT NULL,"
                               + "PRICE TEXT NOT NULL,"
                               + "REGULAR_PRICE TEXT NOT NULL,"
                               + "SALE_PRICE TEXT NOT NULL,"
                               + "ON_SALE INTEGER,"
                               + "TOTAL_SALES INTEGER,"
                               + "VIRTUAL INTEGER,"
                               + "DOWNLOADABLE INTEGER,"
                               + "DOWNLOAD_LIMIT INTEGER,"
                               + "DOWNLOAD_EXPIRY INTEGER,"
                               + "DOWNLOADS TEXT NOT NULL,"
                               + "EXTERNAL_URL TEXT NOT NULL,"
                               + "TAX_STATUS TEXT NOT NULL,"
                               + "TAX_CLASS TEXT NOT NULL,"
                               + "MANAGE_STOCK INTEGER,"
                               + "STOCK_QUANTITY INTEGER,"
                               + "STOCK_STATUS TEXT NOT NULL,"
                               + "BACKORDERS TEXT NOT NULL,"
                               + "BACKORDERS_ALLOWED INTEGER,"
                               + "BACKORDERED INTEGER,"
                               + "SOLD_INDIVIDUALLY INTEGER,"
                               + "WEIGHT TEXT NOT NULL,"
                               + "LENGTH TEXT NOT NULL,"
                               + "WIDTH TEXT NOT NULL,"
                               + "HEIGHT TEXT NOT NULL,"
                               + "SHIPPING_REQUIRED INTEGER,"
                               + "SHIPPING_TAXABLE INTEGER,"
                               + "SHIPPING_CLASS TEXT NOT NULL,"
                               + "SHIPPING_CLASS_ID INTEGER,"
                               + "REVIEWS_ALLOWED INTEGER,"
                               + "AVERAGE_RATING TEXT NOT NULL,"
                               + "RATING_COUNT INTEGER,"
                               + "PARENT_ID INTEGER,"
                               + "PURCHASE_NOTE TEXT NOT NULL,"
                               + "CATEGORIES TEXT NOT NULL,"
                               + "TAGS TEXT NOT NULL,"
                               + "IMAGES TEXT NOT NULL,"
                               + "ATTRIBUTES TEXT NOT NULL,"
                               + "RELATED_IDS TEXT NOT NULL,"
                               + "CROSS_SELL_IDS TEXT NOT NULL,"
                               + "UPSELL_IDS TEXT NOT NULL,"
                               + "VARIATIONS TEXT NOT NULL)");
                    break;
                case 63:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("CREATE TABLE WCProductSettingsModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "LOCAL_SITE_ID INTEGER,"
                               + "WEIGHT_UNIT TEXT NOT NULL,"
                               + "DIMENSION_UNIT TEXT NOT NULL)");
                    break;
                case 64:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("CREATE TABLE WCOrderShipmentTrackingModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "LOCAL_SITE_ID INTEGER,"
                               + "LOCAL_ORDER_ID INTEGER,"
                               + "REMOTE_TRACKING_ID TEXT NOT NULL,"
                               + "TRACKING_NUMBER TEXT NOT NULL,"
                               + "TRACKING_PROVIDER TEXT NOT NULL,"
                               + "TRACKING_LINK TEXT NOT NULL,"
                               + "DATE_SHIPPED TEXT NOT NULL)");
                    break;
                case 65:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("CREATE TABLE WCOrderShipmentProviderModel ("
                               + "LOCAL_SITE_ID INTEGER,"
                               + "COUNTRY TEXT NOT NULL,"
                               + "CARRIER_NAME TEXT NOT NULL,"
                               + "CARRIER_LINK TEXT NOT NULL,"
                               + "_id INTEGER PRIMARY KEY AUTOINCREMENT)");
                    break;
                case 67:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("ALTER TABLE WCSettingsModel ADD COUNTRY_CODE TEXT");
                    break;
                case 69:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("ALTER TABLE WCOrderModel ADD DATE_MODIFIED TEXT");
                    db.execSQL("CREATE TABLE WCOrderSummaryModel (LOCAL_SITE_ID INTEGER,REMOTE_ORDER_ID INTEGER,"
                               + "DATE_CREATED TEXT NOT NULL,_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE,"
                               + "UNIQUE (REMOTE_ORDER_ID, LOCAL_SITE_ID) ON CONFLICT REPLACE)");
                    break;
                case 75:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("CREATE TABLE WCRevenueStatsModel(\n"
                               + "  LOCAL_SITE_ID INTEGER,\n"
                               + "  INTERVAL TEXT NOT NULL,\n"
                               + "  START_DATE TEXT NOT NULL,\n"
                               + "  END_DATE TEXT NOT NULL,\n"
                               + "  DATA TEXT NOT NULL,\n"
                               + "  TOTAL TEXT NOT NULL,\n"
                               + "  _id INTEGER PRIMARY KEY AUTOINCREMENT)");
                    break;
                case 78:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("CREATE TABLE WCVisitorStatsModel(\n"
                               + "  LOCAL_SITE_ID INTEGER,\n"
                               + "  UNIT TEXT NOT NULL,\n"
                               + "  DATE TEXT NOT NULL,\n"
                               + "  START_DATE TEXT NOT NULL,\n"
                               + "  END_DATE TEXT NOT NULL,\n"
                               + "  QUANTITY TEXT NOT NULL,\n"
                               + "  IS_CUSTOM_FIELD INTEGER,\n"
                               + "  FIELDS TEXT NOT NULL,\n"
                               + "  DATA TEXT NOT NULL,\n"
                               + "  _id INTEGER PRIMARY KEY AUTOINCREMENT)");
                    break;
                case 80:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("CREATE TABLE WCNewVisitorStatsModel(\n"
                               + "  LOCAL_SITE_ID INTEGER,\n"
                               + "  GRANULARITY TEXT NOT NULL,\n"
                               + "  DATE TEXT NOT NULL,\n"
                               + "  START_DATE TEXT NOT NULL,\n"
                               + "  END_DATE TEXT NOT NULL,\n"
                               + "  QUANTITY TEXT NOT NULL,\n"
                               + "  IS_CUSTOM_FIELD INTEGER,\n"
                               + "  FIELDS TEXT NOT NULL,\n"
                               + "  DATA TEXT NOT NULL,\n"
                               + "  _id INTEGER PRIMARY KEY AUTOINCREMENT)");
                    break;
                case 81:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("CREATE TABLE WCProductReviewModel ("
                               + "LOCAL_SITE_ID INTEGER,"
                               + "REMOTE_PRODUCT_REVIEW_ID INTEGER,"
                               + "REMOTE_PRODUCT_ID INTEGER,"
                               + "DATE_CREATED TEXT NOT NULL,"
                               + "STATUS TEXT NOT NULL,"
                               + "REVIEWER_NAME TEXT NOT NULL,"
                               + "REVIEWER_EMAIL TEXT NOT NULL,"
                               + "REVIEW TEXT NOT NULL,"
                               + "RATING INTEGER,"
                               + "VERIFIED INTEGER,"
                               + "REVIEWER_AVATARS_JSON TEXT NOT NULL,"
                               + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "FOREIGN KEY(LOCAL_SITE_ID) REFERENCES SiteModel(_id) ON DELETE CASCADE,"
                               + "UNIQUE (REMOTE_PRODUCT_REVIEW_ID, REMOTE_PRODUCT_ID, LOCAL_SITE_ID) "
                               + "ON CONFLICT REPLACE)");
                    break;
                case 82:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("ALTER TABLE WCOrderModel ADD COLUMN DATE_PAID TEXT NOT NULL DEFAULT '';");
                    break;
                case 83:
                    AppLog.d(T.DB, "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("ALTER TABLE WCOrderStatusModel ADD STATUS_COUNT INTEGER");
                    break;
                case 87:
                    AppLog.d(T.DB,
                            "Migrating addon " + addOnName + " to version " + (oldDbVersion + 1));
                    db.execSQL("DROP TABLE IF EXISTS WCRefunds");
                    db.execSQL("CREATE TABLE WCRefunds ("
                               + "_id INTEGER PRIMARY KEY AUTOINCREMENT,"
                               + "LOCAL_SITE_ID INTEGER,"
                               + "ORDER_ID INTEGER,"
                               + "REFUND_ID INTEGER,"
                               + "DATA TEXT NOT NULL)");
                    break;
            }
        }
    }
}
