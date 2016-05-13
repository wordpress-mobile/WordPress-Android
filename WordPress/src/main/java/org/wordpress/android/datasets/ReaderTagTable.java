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
 */
public class ReaderTagTable {

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_tags ("
                + "     tag_slug            TEXT COLLATE NOCASE,"
                + "     tag_display_name    TEXT COLLATE NOCASE,"
                + "     tag_title           TEXT COLLATE NOCASE,"
                + "     tag_type            INTEGER DEFAULT 0,"
                + "     endpoint            TEXT,"
                + "     date_updated        TEXT,"
                + "     PRIMARY KEY (tag_slug, tag_type)"
                + ")");

        db.execSQL("CREATE TABLE tbl_tags_recommended ("
                + "     tag_slug	        TEXT COLLATE NOCASE,"
                + "     tag_display_name    TEXT COLLATE NOCASE,"
                + "     tag_title           TEXT COLLATE NOCASE,"
                + "     tag_type            INTEGER DEFAULT 0,"
                + "     endpoint            TEXT,"
                + "     PRIMARY KEY (tag_slug, tag_type)"
                + ")");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_tags");
        db.execSQL("DROP TABLE IF EXISTS tbl_tags_recommended");
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

    /*
     * similar to the above but only replaces followed tags
     */
    public static void replaceFollowedTags(ReaderTagList tags) {
        if (tags == null || tags.size() == 0) {
            return;
        }

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            try {
                // first delete all existing followed tags, then insert the passed ones
                String[] args = {Integer.toString(ReaderTagType.FOLLOWED.toInt())};
                db.execSQL("DELETE FROM tbl_tags WHERE tag_type=?", args);
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
                    "INSERT OR REPLACE INTO tbl_tags (tag_slug, tag_display_name, tag_title, tag_type, endpoint) VALUES (?1,?2,?3,?4,?5)"
            );

            for (ReaderTag tag: tagList) {
                stmt.bindString(1, tag.getTagSlug());
                stmt.bindString(2, tag.getTagDisplayName());
                stmt.bindString(3, tag.getTagTitle());
                stmt.bindLong  (4, tag.tagType.toInt());
                stmt.bindString(5, tag.getEndpoint());
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
        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT 1 FROM tbl_tags WHERE tag_slug=?1 AND tag_type=?2",
                args);
    }

    /*
     * returns true if the passed tag exists and it has the passed type
     */
    private static boolean tagExistsOfType(String tagSlug, ReaderTagType tagType) {
        if (TextUtils.isEmpty(tagSlug) || tagType == null) {
            return false;
        }

        String[] args = {tagSlug, Integer.toString(tagType.toInt())};
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(),
                "SELECT 1 FROM tbl_tags WHERE tag_slug=?1 AND tag_type=?2",
                args);
    }

    public static boolean isFollowedTagName(String tagSlug) {
        return tagExistsOfType(tagSlug, ReaderTagType.FOLLOWED);
    }

    private static ReaderTag getTagFromCursor(Cursor c) {
        if (c == null) {
            throw new IllegalArgumentException("null tag cursor");
        }

        String tagSlug = c.getString(c.getColumnIndex("tag_slug"));
        String tagDisplayName = c.getString(c.getColumnIndex("tag_display_name"));
        String tagTitle = c.getString(c.getColumnIndex("tag_title"));
        String endpoint = c.getString(c.getColumnIndex("endpoint"));
        ReaderTagType tagType = ReaderTagType.fromInt(c.getInt(c.getColumnIndex("tag_type")));

        return new ReaderTag(tagSlug, tagDisplayName, tagTitle, endpoint, tagType);
    }

    public static ReaderTag getTag(String tagSlug, ReaderTagType tagType) {
        if (TextUtils.isEmpty(tagSlug)) {
            return null;
        }

        String[] args = {tagSlug, Integer.toString(tagType.toInt())};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags WHERE tag_slug=? AND tag_type=? LIMIT 1", args);
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
        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
               "SELECT endpoint FROM tbl_tags WHERE tag_slug=? AND tag_type=?",
               args);
    }

    public static ReaderTagList getDefaultTags() {
        return getTagsOfType(ReaderTagType.DEFAULT);
    }

    public static ReaderTagList getFollowedTags() {
        return getTagsOfType(ReaderTagType.FOLLOWED);
    }

    public static ReaderTagList getCustomListTags() {
        return getTagsOfType(ReaderTagType.CUSTOM_LIST);
    }

    private static ReaderTagList getTagsOfType(ReaderTagType tagType) {
        String[] args = {Integer.toString(tagType.toInt())};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags WHERE tag_type=? ORDER BY tag_slug", args);
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

    static ReaderTagList getAllTags() {
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags ORDER BY tag_slug", null);
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
        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        ReaderDatabase.getWritableDb().delete("tbl_tags", "tag_slug=? AND tag_type=?", args);
    }


    public static String getTagLastUpdated(ReaderTag tag) {
        if (tag == null) {
            return "";
        }
        String[] args = {tag.getTagSlug(), Integer.toString(tag.tagType.toInt())};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(),
                "SELECT date_updated FROM tbl_tags WHERE tag_slug=? AND tag_type=?",
                args);
    }

    public static void setTagLastUpdated(ReaderTag tag) {
       if (tag == null) {
            return;
        }

        String date = DateTimeUtils.javaDateToIso8601(new Date());
        String sql = "UPDATE tbl_tags SET date_updated=?1 WHERE tag_slug=?2 AND tag_type=?3";
        SQLiteStatement stmt = ReaderDatabase.getWritableDb().compileStatement(sql);
        try {
            stmt.bindString(1, date);
            stmt.bindString(2, tag.getTagSlug());
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
            c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags_recommended WHERE tag_slug NOT IN (SELECT tag_slug FROM tbl_tags) ORDER BY tag_slug", null);
        } else {
            c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_tags_recommended ORDER BY tag_slug", null);
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
        SQLiteStatement stmt = db.compileStatement
                ("INSERT INTO tbl_tags_recommended (tag_slug, tag_display_name, tag_title, tag_type, endpoint) VALUES (?1,?2,?3,?4,?5)");
        db.beginTransaction();
        try {
            try {
                // first delete all recommended tags
                db.execSQL("DELETE FROM tbl_tags_recommended");

                // then insert the passed ones
                for (ReaderTag tag: tagList) {
                    stmt.bindString(1, tag.getTagSlug());
                    stmt.bindString(2, tag.getTagDisplayName());
                    stmt.bindString(3, tag.getTagTitle());
                    stmt.bindLong  (4, tag.tagType.toInt());
                    stmt.bindString(5, tag.getEndpoint());
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
