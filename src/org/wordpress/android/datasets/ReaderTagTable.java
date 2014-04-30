package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.models.ReaderTag;
import org.wordpress.android.models.ReaderTag.ReaderTagType;
import org.wordpress.android.models.ReaderTagList;
import org.wordpress.android.ui.reader.ReaderConstants;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.SqlUtils;

import java.util.Date;

/**
 *  tbl_tags stores the list of topics the user subscribed to or has by default
 *  tbl_recommended_tags stores the list of recommended topics returned by the api
 *  tbl_tags_updates stores the iso8601 dates each topic was updated by the app as follows:
 *      date_updated is the date the topic was last updated
 *      date_newest is used when retrieving new posts - only get posts newer than date_newest
 *      date_oldest is used when retrieving old posts - only get posts older than date_oldest
 */
public class ReaderTagTable {
    private static final String COLUMN_NAMES = "tag_name, endpoint, topic_type";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_tags ("
                 + "	tag_name	    TEXT COLLATE NOCASE PRIMARY KEY,"
                 + "    endpoint        TEXT,"
                 + "    topic_type      INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE tbl_recommended_tags ("
                 + "	tag_name	    TEXT COLLATE NOCASE PRIMARY KEY,"
                 + "    endpoint        TEXT,"
                 + "    topic_type      INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE tbl_tag_updates ("
                + "	    tag_name	 TEXT COLLATE NOCASE PRIMARY KEY,"
                + " 	date_updated TEXT,"
                + " 	date_oldest	 TEXT,"
                + " 	date_newest	 TEXT)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_tags");
        db.execSQL("DROP TABLE IF EXISTS tbl_recommended_tags");
        db.execSQL("DROP TABLE IF EXISTS tbl_tag_updates");
    }

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
                // first delete all existing topics
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

    public static void addOrUpdateTags(ReaderTagList tags) {
        if (tags == null || tags.size() == 0) {
            return;
        }
        SQLiteStatement stmt = null;
        try {
            stmt = ReaderDatabase.getWritableDb().compileStatement(
                    "INSERT OR REPLACE INTO tbl_tags ("
                            + COLUMN_NAMES
                            + ") VALUES (?1,?2,?3)");

            for (ReaderTag topic: tags) {
                stmt.bindString(1, topic.getTagName());
                stmt.bindString(2, topic.getEndpoint());
                stmt.bindLong  (3, topic.tagType.toInt());
                stmt.execute();
                stmt.clearBindings();
            }

        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    public static boolean tagExists(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return false;
        }
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT 1 FROM tbl_tags WHERE tag_name=?1", new String[]{tagName});
    }

    private static ReaderTag getTagFromCursor(Cursor c) {
        if (c == null) {
            throw new IllegalArgumentException("null topic cursor");
        }

        String tagName = c.getString(c.getColumnIndex("tag_name"));
        String endpoint = c.getString(c.getColumnIndex("endpoint"));
        ReaderTagType tagType = ReaderTag.ReaderTagType.fromInt(c.getInt(c.getColumnIndex("topic_type")));

        return new ReaderTag(tagName, endpoint, tagType);
    }

    public static ReaderTag getTag(String tagName) {
        if (TextUtils.isEmpty(tagName)) {
            return null;
        }

        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags WHERE tag_name=? LIMIT 1", new String[]{tagName});
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
        String[] args = {Integer.toString(ReaderTag.ReaderTagType.DEFAULT.toInt())};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags WHERE topic_type=? ORDER BY tag_name", args);
        try {
            ReaderTagList topics = new ReaderTagList();
            if (c.moveToFirst()) {
                do {
                    topics.add(getTagFromCursor(c));
                } while (c.moveToNext());
            }
            return topics;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static ReaderTagList getFollowedTags() {
        String[] args = {Integer.toString(ReaderTag.ReaderTagType.FOLLOWED.toInt())};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags WHERE topic_type=? ORDER BY tag_name", args);
        try {
            ReaderTagList topics = new ReaderTagList();
            if (c.moveToFirst()) {
                do {
                    topics.add(getTagFromCursor(c));
                } while (c.moveToNext());
            }
            return topics;
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

    /*
     * returns true if the passed topic has ever been updated - used to determine whether a topic
     * has no posts because it has never been updated in the app, or it has been updated and just
     * doesn't have any posts
     */
    public static boolean hasEverUpdatedTag(String tagName) {
        return !TextUtils.isEmpty(getTagLastUpdated(tagName));
    }

    public static String getTagLastUpdated(String tagName) {
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
     * determine whether the passed topic should be auto-updated based on when it was last updated
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
     * recommended topics - stored in a separate table from default/subscribed topics, but have the same column names
     **/
    public static ReaderTagList getRecommendedTags(boolean excludeSubscribed) {
        Cursor c;
        if (excludeSubscribed) {
            c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_recommended_tags WHERE tag_name NOT IN (SELECT tag_name FROM tbl_tags) ORDER BY tag_name", null);
        } else {
            c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_recommended_tags ORDER BY tag_name", null);
        }
        try {
            ReaderTagList topics = new ReaderTagList();
            if (c.moveToFirst()) {
                do {
                    topics.add(getTagFromCursor(c));
                } while (c.moveToNext());
            }
            return topics;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void setRecommendedTags(ReaderTagList topics) {
        if (topics == null) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        SQLiteStatement stmt = db.compileStatement("INSERT INTO tbl_recommended_tags (" + COLUMN_NAMES + ") VALUES (?1,?2,?3)");
        db.beginTransaction();
        try {
            try {
                // first delete all recommended topics
                db.execSQL("DELETE FROM tbl_recommended_tags");

                // then insert the passed ones
                for (ReaderTag topic: topics) {
                    stmt.bindString(1, topic.getTagName());
                    stmt.bindString(2, topic.getEndpoint());
                    stmt.bindLong  (3, topic.tagType.toInt());
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
