package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.models.ReaderTagType;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.SqlUtils;

import java.util.Date;

/**
 *  tbl_tags stores the list of tags the user subscribed to or has by default
 *  tbl_tags_recommended stores the list of recommended tags returned by the api
 *  tbl_tag_updates stores the iso8601 dates each tag was updated by the app as follows:
 *      date_updated is the date the tag was last updated
 *      date_newest is used when retrieving new posts - only get posts newer than date_newest
 *      date_oldest is used when retrieving old posts - only get posts older than date_oldest
 */
public class ReaderTagTable {
    private static final String COLUMN_NAMES = "tag_name, tag_type, endpoint";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_tags ("
                 + "	tag_name    TEXT COLLATE NOCASE,"
                 + "    tag_type    INTEGER DEFAULT 0,"
                 + "    endpoint    TEXT,"
                 + "    PRIMARY KEY (tag_name, tag_type)"
                 + ")");

        db.execSQL("CREATE TABLE tbl_tags_recommended ("
                 + "	tag_name	TEXT COLLATE NOCASE,"
                 + "    tag_type    INTEGER DEFAULT 0,"
                 + "    endpoint    TEXT,"
                 + "    PRIMARY KEY (tag_name, tag_type)"
                 + ")");

        db.execSQL("CREATE TABLE tbl_tag_updates ("
                + "	    tag_name	 TEXT COLLATE NOCASE,"
                + "     tag_type     INTEGER DEFAULT 0,"
                + " 	date_updated TEXT,"
                + " 	date_oldest	 TEXT,"
                + " 	date_newest	 TEXT,"
                + "     PRIMARY KEY (tag_name, tag_type)"
                + ")");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_tags");
        db.execSQL("DROP TABLE IF EXISTS tbl_tags_recommended");
        db.execSQL("DROP TABLE IF EXISTS tbl_tag_updates");
    }

    /*
     * remove update data for tags that no longer exist
     */
    protected static int purge(SQLiteDatabase db) {
        return db.delete("tbl_tag_updates", "tag_name NOT IN (SELECT DISTINCT tag_name FROM tbl_tags)", null);
    }

    /*
     * returns true if tbl_tags is empty
     */
    public static boolean isEmpty() {
        return (SqlUtils.getRowCount(ReaderDatabase.getReadableDb(), "tbl_tags") == 0);
    }

    /*
     * replaces all tags with the passed list
     */
    public static void replaceTags(ReaderTagList tags) {
        if (tags == null || tags.size() == 0) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            try {
                // first delete all existing tags
                db.execSQL("DELETE FROM tbl_tags");

                // then insert the passed ones
                addOrUpdateTags(tags);

                db.setTransactionSuccessful();

            } catch (SQLException e) {
                AppLog.e(T.READER, e);
            }
        } finally {
            db.endTransaction();
        }
    }

    public static void addOrUpdateTag(ReaderTag tag) {
        if (tag == null) {
            return;
        }
        ReaderTagList tags = new ReaderTagList();
        tags.add(tag);
        addOrUpdateTags(tags);
    }

    private static void addOrUpdateTags(ReaderTagList tagList) {
        if (tagList == null || tagList.size() == 0) {
            return;
        }
        SQLiteStatement stmt = null;
        try {
            stmt = ReaderDatabase.getWritableDb().compileStatement(
                    "INSERT OR REPLACE INTO tbl_tags ("
                            + COLUMN_NAMES
                            + ") VALUES (?1,?2,?3)");

            for (ReaderTag tag: tagList) {
                stmt.bindString(1, tag.getTagName());
                stmt.bindLong  (2, tag.tagType.toInt());
                stmt.bindString(3, tag.getEndpoint());
                stmt.execute();
                stmt.clearBindings();
            }

        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    /*
     * returns true if the passed tag exists, regardless of type
     */
    public static boolean tagExists(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return false;
        }
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT 1 FROM tbl_tags WHERE tag_name=?1", new String[]{tagName});
    }

    /*
     * returns true if the passed tag exists and it has the passed type
     */
    private static boolean tagExistsOfType(String tagName, ReaderTagType tagType) {
        if (TextUtils.isEmpty(tagName)) {
            return false;
        }
        // look for any tag with this name if tagType isn't passed
        if (tagType == null) {
            return tagExists(tagName);
        }
        String[] args = {tagName, Integer.toString(tagType.toInt())};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT 1 FROM tbl_tags WHERE tag_name=?1 AND tag_type=?2",
                args);
    }

    public static boolean isFollowedTag(String tagName) {
        return tagExistsOfType(tagName, ReaderTagType.FOLLOWED);
    }

    public static boolean isDefaultTag(String tagName) {
        return tagExistsOfType(tagName, ReaderTagType.DEFAULT);
    }

    private static ReaderTag getTagFromCursor(Cursor c) {
        if (c == null) {
            throw new IllegalArgumentException("null tag cursor");
        }

        String tagName = c.getString(c.getColumnIndex("tag_name"));
        String endpoint = c.getString(c.getColumnIndex("endpoint"));
        ReaderTagType tagType = ReaderTagType.fromInt(c.getInt(c.getColumnIndex("tag_type")));

        return new ReaderTag(tagName, endpoint, tagType);
    }

    public static ReaderTag getTag(String tagName, ReaderTagType tagType) {
        if (TextUtils.isEmpty(tagName)) {
            return null;
        }

        String[] args = {tagName, Integer.toString(tagType.toInt())};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags WHERE tag_name=? AND tag_type=? LIMIT 1", args);
        try {
            if (!c.moveToFirst()) {
                return null;
            }
            return getTagFromCursor(c);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static String getEndpointForTag(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return null;
        }
        String[] args = {tagName};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), "SELECT endpoint FROM tbl_tags WHERE tag_name=?", args);
    }

    public static ReaderTagList getDefaultTags() {
        return getTagsOfType(ReaderTagType.DEFAULT);
    }

    public static ReaderTagList getFollowedTags() {
        return getTagsOfType(ReaderTagType.FOLLOWED);
    }

    private static ReaderTagList getTagsOfType(ReaderTagType tagType) {
        String[] args = {Integer.toString(tagType.toInt())};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags WHERE tag_type=? ORDER BY tag_name", args);
        try {
            ReaderTagList tagList = new ReaderTagList();
            if (c.moveToFirst()) {
                do {
                    tagList.add(getTagFromCursor(c));
                } while (c.moveToNext());
            }
            return tagList;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void deleteTag(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return;
        }
        String[] args = {tagName};
        ReaderDatabase.getWritableDb().delete("tbl_tags", "tag_name=?", args);
        ReaderDatabase.getWritableDb().delete("tbl_tag_updates", "tag_name=?", args);
    }

    /**
     * tbl_tag_updates routines
     **/
    public static String getTagNewestDate(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return "";
        }
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), "SELECT date_newest FROM tbl_tag_updates WHERE tag_name=?", new String[]{tagName});
    }
    public static void setTagNewestDate(String tagName, String date) {
        if (TextUtils.isEmpty(tagName)) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("tag_name", tagName);
        values.put("date_newest", date);
        try {
            ReaderDatabase.getWritableDb().insertWithOnConflict("tbl_tag_updates", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (SQLException e) {
            AppLog.e(T.READER, e);
        }
    }

    public static String getTagOldestDate(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return "";
        }
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), "SELECT date_oldest FROM tbl_tag_updates WHERE tag_name=?", new String[]{tagName});
    }
    public static void setTagOldestDate(String tagName, String date) {
        if (TextUtils.isEmpty(tagName)) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("tag_name", tagName);
        values.put("date_oldest", date);
        try {
            ReaderDatabase.getWritableDb().insertWithOnConflict("tbl_tag_updates", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (SQLException e) {
            AppLog.e(T.READER, e);
        }
    }

    private static String getTagLastUpdated(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return "";
        }
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), "SELECT date_updated FROM tbl_tag_updates WHERE tag_name=?", new String[]{tagName});
    }

    public static void setTagLastUpdated(String tagName, String date) {
        if (TextUtils.isEmpty(tagName)) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("tag_name", tagName);
        values.put("date_updated", date);
        try {
            ReaderDatabase.getWritableDb().insertWithOnConflict("tbl_tag_updates", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (SQLException e) {
            AppLog.e(T.READER, e);
        }
    }

    /*
     * determine whether the passed tag should be auto-updated based on when it was last updated
     */
    public static boolean shouldAutoUpdateTag(String tagName) {
        int minutes = minutesSinceLastUpdate(tagName);
        if (minutes == NEVER_UPDATED) {
            return true;
        }
        return (minutes >= ReaderConstants.READER_AUTO_UPDATE_DELAY_MINUTES);
    }

    private static final int NEVER_UPDATED = -1;
    private static int minutesSinceLastUpdate(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return 0;
        }

        String updated = getTagLastUpdated(tagName);
        if (TextUtils.isEmpty(updated)) {
            return NEVER_UPDATED;
        }

        Date dtUpdated = DateTimeUtils.iso8601ToJavaDate(updated);
        if (dtUpdated == null) {
            return 0;
        }

        Date dtNow = new Date();
        return DateTimeUtils.minutesBetween(dtUpdated, dtNow);
    }

    /**
     * recommended tags - stored in a separate table from default/subscribed tags, but have the same column names
     **/
    public static ReaderTagList getRecommendedTags(boolean excludeSubscribed) {
        Cursor c;
        if (excludeSubscribed) {
            c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags_recommended WHERE tag_name NOT IN (SELECT tag_name FROM tbl_tags) ORDER BY tag_name", null);
        } else {
            c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags_recommended ORDER BY tag_name", null);
        }
        try {
            ReaderTagList tagList = new ReaderTagList();
            if (c.moveToFirst()) {
                do {
                    tagList.add(getTagFromCursor(c));
                } while (c.moveToNext());
            }
            return tagList;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void setRecommendedTags(ReaderTagList tagList) {
        if (tagList == null) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        SQLiteStatement stmt = db.compileStatement("INSERT INTO tbl_tags_recommended (" + COLUMN_NAMES + ") VALUES (?1,?2,?3)");
        db.beginTransaction();
        try {
            try {
                // first delete all recommended tags
                db.execSQL("DELETE FROM tbl_tags_recommended");

                // then insert the passed ones
                for (ReaderTag tag: tagList) {
                    stmt.bindString(1, tag.getTagName());
                    stmt.bindLong  (2, tag.tagType.toInt());
                    stmt.bindString(3, tag.getEndpoint());
                    stmt.execute();
                    stmt.clearBindings();
                }

                db.setTransactionSuccessful();

            } catch (SQLException e) {
                AppLog.e(T.READER, e);
            }
        } finally {
            SqlUtils.closeStatement(stmt);
            db.endTransaction();
        }
    }
}
