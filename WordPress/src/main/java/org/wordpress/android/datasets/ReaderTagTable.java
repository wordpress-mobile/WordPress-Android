package org.wordpress.android.datasets;

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
                // first delete all existing tags, then insert the passed ones
                db.execSQL("DELETE FROM tbl_tags");
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
            }

        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    /*
     * returns true if the passed tag exists, regardless of type
     */
    public static boolean tagExists(ReaderTag tag) {
        if (tag == null) {
            return false;
        }
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT 1 FROM tbl_tags WHERE tag_name=?1 AND tag_type=?2",
                args);
    }

    /*
     * returns true if the passed tag exists and it has the passed type
     */
    private static boolean tagExistsOfType(String tagName, ReaderTagType tagType) {
        if (TextUtils.isEmpty(tagName) || tagType == null) {
            return false;
        }

        String[] args = {tagName, Integer.toString(tagType.toInt())};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT 1 FROM tbl_tags WHERE tag_name=?1 AND tag_type=?2",
                args);
    }

    public static boolean isFollowedTagName(String tagName) {
        return tagExistsOfType(tagName, ReaderTagType.FOLLOWED);
    }

    public static boolean isDefaultTagName(String tagName) {
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

    public static String getEndpointForTag(ReaderTag tag) {
        if (tag == null) {
            return null;
        }
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
               "SELECT endpoint FROM tbl_tags WHERE tag_name=? AND tag_type=?",
               args);
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

    public static void deleteTag(ReaderTag tag) {
        if (tag == null) {
            return;
        }
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        ReaderDatabase.getWritableDb().delete("tbl_tags", "tag_name=? AND tag_type=?", args);
        ReaderDatabase.getWritableDb().delete("tbl_tag_updates", "tag_name=? AND tag_type=?", args);
    }


    /**
     * tbl_tag_updates routines
     **/
    public static String getTagNewestDate(ReaderTag tag) {
        return getDateColumn(tag, "date_newest");
    }
    public static void setTagNewestDate(ReaderTag tag, String date) {
        setDateColumn(tag, "date_newest", date);
    }

    public static String getTagOldestDate(ReaderTag tag) {
        return getDateColumn(tag, "date_oldest");
    }
    public static void setTagOldestDate(ReaderTag tag, String date) {
        setDateColumn(tag, "date_oldest", date);
    }

    private static String getTagLastUpdated(ReaderTag tag) {
        return getDateColumn(tag, "date_updated");
    }
    public static void setTagLastUpdated(ReaderTag tag, String date) {
        setDateColumn(tag, "date_updated", date);
    }

    private static String getDateColumn(ReaderTag tag, String colName) {
        if (tag == null) {
            return "";
        }
        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                "SELECT " + colName + " FROM tbl_tag_updates WHERE tag_name=? AND tag_type=?",
                args);
    }
    private static void setDateColumn(ReaderTag tag, String colName, String date) {
        if (tag == null) {
            return;
        }

        String[] args = {tag.getTagName(), Integer.toString(tag.tagType.toInt())};
        boolean rowExists = SqlUtils.boolForQuery(
                ReaderDatabase.getReadableDb(),
                "SELECT 1 FROM tbl_tag_updates WHERE tag_name=? AND tag_type=?",
                args);

        final String sql;
        if (rowExists) {
            sql = "UPDATE tbl_tag_updates SET " + colName + "=?1 WHERE tag_name=?2 AND tag_type=?3";
        } else {
            sql = "INSERT INTO tbl_tag_updates (" + colName + ", tag_name, tag_type) VALUES (?1,?2,?3)";
        }

        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement(sql);
        try {
            stmt.bindString(1, date);
            stmt.bindString(2, tag.getTagName());
            stmt.bindLong  (3, tag.tagType.toInt());
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    /*
     * determine whether the passed tag should be auto-updated based on when it was last updated
     */
    public static boolean shouldAutoUpdateTag(ReaderTag tag) {
        int minutes = minutesSinceLastUpdate(tag);
        if (minutes == NEVER_UPDATED) {
            return true;
        }
        return (minutes >= ReaderConstants.READER_AUTO_UPDATE_DELAY_MINUTES);
    }

    private static final int NEVER_UPDATED = -1;
    private static int minutesSinceLastUpdate(ReaderTag tag) {
        if (tag == null) {
            return 0;
        }

        String updated = getTagLastUpdated(tag);
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
