package org.wordpress.android.fluxc.persistence;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.os.Build;
import android.support.annotation.StringDef;

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
        return 27;
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

    @SuppressWarnings({"FallThrough"})
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
                        + "RATING TEXT,REQUIRED_WORD_PRESS_VERSION TEXT,SLUG TEXT,VERSION TEXT,WHATS_NEW_AS_HTML TEXT,"
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
                db.execSQL(
                        "CREATE TABLE ActivityLogModel (_id INTEGER PRIMARY KEY AUTOINCREMENT,LOCAL_SITE_ID INTEGER,"
                        + "REMOTE_SITE_ID INTEGER,ACTIVITY_ID TEXT NOT NULL,SUMMARY TEXT NOT NULL,TEXT TEXT NOT NULL,"
                        + "NAME TEXT,TYPE TEXT,GRIDICON TEXT,STATUS TEXT,REWINDABLE INTEGER,REWIND_ID TEXT,PUBLISHED "
                        + "TEXT NOT NULL,DISCARDED INTEGER,DISPLAY_NAME TEXT,ACTOR_TYPE TEXT,WPCOM_USER_ID INTEGER,"
                        + "AVATAR_URL TEXT,ROLE TEXT)");
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

    private void migrateAddOn(@AddOn String addOnName, SQLiteDatabase db, int oldDbVersion) {
        if (mActiveAddOns.contains(addOnName)) {
            switch (oldDbVersion) {
                // TODO
            }
        }
    }
}
