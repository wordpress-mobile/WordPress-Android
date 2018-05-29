package org.wordpress.android.datasets;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.util.SqlUtils;

/**
 * Table (tbl_quick_start) containing information about checklist progress for Quick Start including:
 * - Flag of done for each task
 * - Flag of all tasks completed
 * - Count of tasks done
 * - Count of suggestions shown
 */
public class QuickStartTable {
    private static final String COLUMN_HAS_COMPLETED = "has_completed";
    private static final String COLUMN_HAS_DONE_CHOOSE_THEME = "has_done_choose_theme";
    private static final String COLUMN_HAS_DONE_CREATE_SITE = "has_done_create_site";
    private static final String COLUMN_HAS_DONE_CUSTOMIZE_SITE = "has_done_customize_site";
    private static final String COLUMN_HAS_DONE_FOLLOW_SITE = "has_done_follow_site";
    private static final String COLUMN_HAS_DONE_PUBLISH_POST = "has_done_publish_post";
    private static final String COLUMN_HAS_DONE_SHARE_SITE = "has_done_share_site";
    private static final String COLUMN_HAS_DONE_VIEW_SITE = "has_done_view_site";
    private static final String COLUMN_NUM_DONE = "num_done";
    private static final String COLUMN_NUM_SHOWN = "num_shown";
    private static final String COLUMN_SITE_ID = "site_id";
    private static final String COLUMNS =
            COLUMN_SITE_ID + ","
            + COLUMN_HAS_DONE_CREATE_SITE + ","
            + COLUMN_HAS_DONE_VIEW_SITE + ","
            + COLUMN_HAS_DONE_CHOOSE_THEME + ","
            + COLUMN_HAS_DONE_CUSTOMIZE_SITE + ","
            + COLUMN_HAS_DONE_SHARE_SITE + ","
            + COLUMN_HAS_DONE_PUBLISH_POST + ","
            + COLUMN_HAS_DONE_FOLLOW_SITE + ","
            + COLUMN_HAS_COMPLETED + ","
            + COLUMN_NUM_DONE + ","
            + COLUMN_NUM_SHOWN;
    private static final String TABLE = "tbl_quick_start";

    protected static void createTables(SQLiteDatabase database) {
        database.execSQL("CREATE TABLE " + TABLE + " ("
                   + COLUMN_SITE_ID + " INTEGER DEFAULT 0, "
                   + COLUMN_HAS_DONE_CREATE_SITE + " INTEGER DEFAULT 0, "
                   + COLUMN_HAS_DONE_VIEW_SITE + " INTEGER DEFAULT 0, "
                   + COLUMN_HAS_DONE_CHOOSE_THEME + " INTEGER DEFAULT 0, "
                   + COLUMN_HAS_DONE_CUSTOMIZE_SITE + " INTEGER DEFAULT 0, "
                   + COLUMN_HAS_DONE_SHARE_SITE + " INTEGER DEFAULT 0, "
                   + COLUMN_HAS_DONE_PUBLISH_POST + " INTEGER DEFAULT 0, "
                   + COLUMN_HAS_DONE_FOLLOW_SITE + " INTEGER DEFAULT 0, "
                   + COLUMN_HAS_COMPLETED + " INTEGER DEFAULT 0, "
                   + COLUMN_NUM_DONE + " INTEGER DEFAULT 0, "
                   + COLUMN_NUM_SHOWN + " INTEGER DEFAULT 0, "
                   + "PRIMARY KEY (site_id)"
                   + ")");
    }

    protected static void dropTables(SQLiteDatabase database) {
        database.execSQL("DROP TABLE IF EXISTS " + TABLE);
    }

    private static boolean getBooleanWithColumnAndSiteId(String column, int siteId) {
        String sql = "SELECT " + column + " FROM " + TABLE
                   + " WHERE " + COLUMN_SITE_ID + "=" + Integer.toString(siteId);
        return SqlUtils.boolForQuery(QuickStartDatabase.getReadableDatabaseHelper(), sql, null);
    }

    private static int getIntegerWithColumnAndSiteId(String column, int siteId) {
        String sql = "SELECT " + column + " FROM " + TABLE
                   + " WHERE " + COLUMN_SITE_ID + "=" + Integer.toString(siteId);
        return SqlUtils.intForQuery(QuickStartDatabase.getReadableDatabaseHelper(), sql, null);
    }

    public static int getNumberDone(int siteId) {
        return getIntegerWithColumnAndSiteId(COLUMN_NUM_DONE, siteId);
    }

    public static int getNumberShown(int siteId) {
        return getIntegerWithColumnAndSiteId(COLUMN_NUM_SHOWN, siteId);
    }

    public static boolean hasCompleted(int siteId) {
        return getBooleanWithColumnAndSiteId(COLUMN_HAS_COMPLETED, siteId);
    }

    public static boolean hasDoneChooseTheme(int siteId) {
        return getBooleanWithColumnAndSiteId(COLUMN_HAS_DONE_CHOOSE_THEME, siteId);
    }

    public static boolean hasDoneCreateSite(int siteId) {
        return getBooleanWithColumnAndSiteId(COLUMN_HAS_DONE_CREATE_SITE, siteId);
    }

    public static boolean hasDoneCustomizeSite(int siteId) {
        return getBooleanWithColumnAndSiteId(COLUMN_HAS_DONE_CUSTOMIZE_SITE, siteId);
    }

    public static boolean hasDoneFollowSite(int siteId) {
        return getBooleanWithColumnAndSiteId(COLUMN_HAS_DONE_FOLLOW_SITE, siteId);
    }

    public static boolean hasDonePublishPost(int siteId) {
        return getBooleanWithColumnAndSiteId(COLUMN_HAS_DONE_PUBLISH_POST, siteId);
    }

    public static boolean hasDoneShareSite(int siteId) {
        return getBooleanWithColumnAndSiteId(COLUMN_HAS_DONE_SHARE_SITE, siteId);
    }

    public static boolean hasDoneViewSite(int siteId) {
        return getBooleanWithColumnAndSiteId(COLUMN_HAS_DONE_VIEW_SITE, siteId);
    }

    public static void insertOrReplaceSite(int siteId) {
        SQLiteDatabase database = QuickStartDatabase.getWritableDatabaseHelper();
        SQLiteStatement statement = database.compileStatement(
                "INSERT OR REPLACE INTO " + TABLE + " (" + COLUMNS + ") VALUES (?1,?2,?3,?4,?5,?6,?7,?8,?9,?10,?11)"
        );
        database.beginTransaction();

        try {
            // Start with create site done (i.e. COLUMN_HAS_DONE_CREATE_SITE and COLUMN_NUM_DONE are 1).
            statement.bindLong(1, siteId); // COLUMN_SITE_ID
            statement.bindLong(2, 1);      // COLUMN_HAS_DONE_CREATE_SITE
            statement.bindLong(3, 0);      // COLUMN_HAS_DONE_VIEW_SITE
            statement.bindLong(4, 0);      // COLUMN_HAS_DONE_CHOOSE_THEME
            statement.bindLong(5, 0);      // COLUMN_HAS_DONE_CUSTOMIZE_SITE
            statement.bindLong(6, 0);      // COLUMN_HAS_DONE_SHARE_SITE
            statement.bindLong(7, 0);      // COLUMN_HAS_DONE_PUBLISH_POST
            statement.bindLong(8, 0);      // COLUMN_HAS_DONE_FOLLOW_SITE
            statement.bindLong(9, 0);      // COLUMN_HAS_COMPLETED
            statement.bindLong(10, 1);     // COLUMN_NUM_DONE
            statement.bindLong(11, 0);     // COLUMN_NUM_SHOWN
            statement.execute();
            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
            SqlUtils.closeStatement(statement);
        }
    }

    private static void setBooleanWithColumnAndSiteId(boolean value, String column, int siteId) {
        String[] arguments = {String.valueOf(SqlUtils.boolToSql(value)), Integer.toString(siteId)};
        QuickStartDatabase.getWritableDatabaseHelper().execSQL(
                "UPDATE " + TABLE + " SET " + column + "=?"
                + " WHERE " + COLUMN_SITE_ID + "=?", arguments
        );
    }

    public static void setHasCompleted(boolean hasCompleted, int siteId) {
        setBooleanWithColumnAndSiteId(hasCompleted, COLUMN_HAS_COMPLETED, siteId);
    }

    public static void setHasDoneChooseSite(boolean hasDone, int siteId) {
        setBooleanWithColumnAndSiteId(hasDone, COLUMN_HAS_DONE_CHOOSE_THEME, siteId);
    }

    public static void setHasDoneCreateSite(boolean hasDone, int siteId) {
        setBooleanWithColumnAndSiteId(hasDone, COLUMN_HAS_DONE_CREATE_SITE, siteId);
    }

    public static void setHasDoneCustomizeSite(boolean hasDone, int siteId) {
        setBooleanWithColumnAndSiteId(hasDone, COLUMN_HAS_DONE_CUSTOMIZE_SITE, siteId);
    }

    public static void setHasDoneFollowSite(boolean hasDone, int siteId) {
        setBooleanWithColumnAndSiteId(hasDone, COLUMN_HAS_DONE_FOLLOW_SITE, siteId);
    }

    public static void setHasDonePublishPost(boolean hasDone, int siteId) {
        setBooleanWithColumnAndSiteId(hasDone, COLUMN_HAS_DONE_PUBLISH_POST, siteId);
    }

    public static void setHasDoneShareSite(boolean hasDone, int siteId) {
        setBooleanWithColumnAndSiteId(hasDone, COLUMN_HAS_DONE_SHARE_SITE, siteId);
    }

    public static void setHasDoneViewSite(boolean hasDone, int siteId) {
        setBooleanWithColumnAndSiteId(hasDone, COLUMN_HAS_DONE_VIEW_SITE, siteId);
    }

    private static void setIntegerWithColumnAndSiteId(int value, String column, int siteId) {
        String[] arguments = {Integer.toString(value), Integer.toString(siteId)};
        QuickStartDatabase.getWritableDatabaseHelper().execSQL(
                "UPDATE " + TABLE + " SET " + column + "=?"
                + " WHERE " + COLUMN_SITE_ID + "=?", arguments
        );
    }

    public static void setNumberDone(int numDone, int siteId) {
        setIntegerWithColumnAndSiteId(numDone, COLUMN_NUM_DONE, siteId);
    }

    public static void setNumberShown(int numShown, int siteId) {
        setIntegerWithColumnAndSiteId(numShown, COLUMN_NUM_SHOWN, siteId);
    }
}
