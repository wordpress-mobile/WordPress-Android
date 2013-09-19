package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.text.TextUtils;

import org.wordpress.android.Constants;
import org.wordpress.android.models.ReaderTopic;
import org.wordpress.android.models.ReaderTopic.ReaderTopicType;
import org.wordpress.android.models.ReaderTopicList;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.ReaderLog;
import org.wordpress.android.util.SqlUtils;

import java.util.Date;

/**
 *  Created by nbradbury on 6/23/13.
 *  tbl_topics stores the list of topics the user subscribed to or has by default
 *  tbl_topics_updates stored the iso8601 dates each topic was updated by the app as follows:
 *      date_updated is the date the topic was last updated
 *      date_newest is used when retrieving new posts - only get posts newer than date_newest
 *      date_oldest is used when retrieving old posts - only get posts older than date_oldest
 */
public class ReaderTopicTable {
    private static final String COLUMN_NAMES = "topic_name, endpoint, topic_type";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_topics ("
                 + "	topic_name	    TEXT COLLATE NOCASE PRIMARY KEY,"
                 + "    endpoint        TEXT,"
                 + "    topic_type      INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE tbl_recommended_topics ("
                 + "	topic_name	    TEXT COLLATE NOCASE PRIMARY KEY,"
                 + "    endpoint        TEXT,"
                 + "    topic_type      INTEGER DEFAULT 0)");

        db.execSQL("CREATE TABLE tbl_topic_updates ("
                + "	    topic_name	 TEXT COLLATE NOCASE PRIMARY KEY,"
                + " 	date_updated TEXT,"
                + " 	date_oldest	 TEXT,"
                + " 	date_newest	 TEXT)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_topics");
        db.execSQL("DROP TABLE IF EXISTS tbl_recommended_topics");
        db.execSQL("DROP TABLE IF EXISTS tbl_topic_updates");
    }

    protected static int purge(SQLiteDatabase db) {
        return db.delete("tbl_topic_updates", "topic_name NOT IN (SELECT DISTINCT topic_name FROM tbl_topics)", null);
    }

    protected static void resetTopicUpdates(SQLiteDatabase db) {
        db.delete("tbl_topic_updates", null, null);
    }

    /*
     * replaces all topics with the passed list
     */
    public static void replaceTopics(ReaderTopicList topics) {
        if (topics==null || topics.size()==0)
            return;

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        try {
            try {
                // first delete all existing topics
                db.execSQL("DELETE FROM tbl_topics");

                // then insert the passed ones
                addOrUpdateTopics(topics);

                db.setTransactionSuccessful();

            } catch (SQLException e) {
                ReaderLog.e(e);
            }
        } finally {
            db.endTransaction();
        }
    }

    public static void addOrUpdateTopic(ReaderTopic topic) {
        if (topic==null)
            return;
        ReaderTopicList topics = new ReaderTopicList();
        topics.add(topic);
        addOrUpdateTopics(topics);
    }

    public static void addOrUpdateTopics(ReaderTopicList topics) {
        if (topics==null || topics.size()==0)
            return;
        SQLiteStatement stmt = null;
        try {
            stmt = ReaderDatabase.getWritableDb().compileStatement(
                    "INSERT OR REPLACE INTO tbl_topics ("
                            + COLUMN_NAMES
                            + ") VALUES (?1,?2,?3)");

            for (ReaderTopic topic: topics) {
                stmt.bindString(1, topic.getTopicName());
                stmt.bindString(2, topic.getEndpoint());
                stmt.bindLong  (3, topic.topicType.toInt());
                stmt.execute();
                stmt.clearBindings();
            }

        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    public static boolean topicExists(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return false;
        return SqlUtils.boolForQuery(ReaderDatabase.getReadableDb(), "SELECT 1 FROM tbl_topics WHERE topic_name=?1", new String[]{topicName});
    }

    private static ReaderTopic getTopicFromCursor(Cursor c) {
        if (c==null)
            throw new IllegalArgumentException("null topic cursor");

        ReaderTopic topic = new ReaderTopic();

        topic.setTopicName(c.getString(c.getColumnIndex("topic_name")));
        topic.setEndpoint(c.getString(c.getColumnIndex("endpoint")));
        topic.topicType = ReaderTopicType.fromInt(c.getInt(c.getColumnIndex("topic_type")));

        return topic;
    }

    public static ReaderTopic getTopic(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return null;

        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_topics WHERE topic_name=? LIMIT 1", new String[]{topicName});
        try {
            if (!c.moveToFirst())
                return null;
            return getTopicFromCursor(c);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static ReaderTopicList getDefaultTopics() {
        String[] args = {Integer.toString(ReaderTopicType.DEFAULT.toInt())};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_topics WHERE topic_type=? ORDER BY topic_name", args);
        try {
            ReaderTopicList topics = new ReaderTopicList();
            if (c.moveToFirst()) {
                do {
                    topics.add(getTopicFromCursor(c));
                } while (c.moveToNext());
            }
            return topics;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static ReaderTopicList getSubscribedTopics() {
        String[] args = {Integer.toString(ReaderTopicType.SUBSCRIBED.toInt())};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_topics WHERE topic_type=? ORDER BY topic_name", args);
        try {
            ReaderTopicList topics = new ReaderTopicList();
            if (c.moveToFirst()) {
                do {
                    topics.add(getTopicFromCursor(c));
                } while (c.moveToNext());
            }
            return topics;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void deleteTopic(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return;
        String[] args = {topicName};
        ReaderDatabase.getWritableDb().delete("tbl_topics", "topic_name=?", args);
        ReaderDatabase.getWritableDb().delete("tbl_topic_updates", "topic_name=?", args);
    }

    /**
     * tbl_topic_updates routines
     **/
    public static String getTopicNewestDate(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return "";
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), "SELECT date_newest FROM tbl_topic_updates WHERE topic_name=?", new String[]{topicName});
    }
    public static void setTopicNewestDate(String topicName, String date) {
        if (TextUtils.isEmpty(topicName))
            return;

        ContentValues values = new ContentValues();
        values.put("topic_name", topicName);
        values.put("date_newest", date);
        try {
            ReaderDatabase.getWritableDb().insertWithOnConflict("tbl_topic_updates", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (SQLException e) {
            ReaderLog.e(e);
        }
    }

    public static String getTopicOldestDate(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return "";
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), "SELECT date_oldest FROM tbl_topic_updates WHERE topic_name=?", new String[]{topicName});
    }
    public static void setTopicOldestDate(String topicName, String date) {
        if (TextUtils.isEmpty(topicName))
            return;

        ContentValues values = new ContentValues();
        values.put("topic_name", topicName);
        values.put("date_oldest", date);
        try {
            ReaderDatabase.getWritableDb().insertWithOnConflict("tbl_topic_updates", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (SQLException e) {
            ReaderLog.e(e);
        }
    }

    /*
     * returns true if the passed topic has ever been updated - used to determine whether a topic
     * has no posts because it has never been updated in the app, or it has been updated and just
     * doesn't have any posts
     */
    public static boolean hasEverUpdatedTopic(String topicName) {
        return !TextUtils.isEmpty(getTopicLastUpdated(topicName));
    }

    public static String getTopicLastUpdated(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return "";
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), "SELECT date_updated FROM tbl_topic_updates WHERE topic_name=?", new String[]{topicName});
    }
    public static void setTopicLastUpdated(String topicName, String date) {
        if (TextUtils.isEmpty(topicName))
            return;

        ContentValues values = new ContentValues();
        values.put("topic_name", topicName);
        values.put("date_updated", date);
        try {
            ReaderDatabase.getWritableDb().insertWithOnConflict("tbl_topic_updates", null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (SQLException e) {
            ReaderLog.e(e);
        }
    }

    /*
     * determine whether the passed topic should be auto-updated based on when it was last updated
     */
    public static boolean shouldAutoUpdateTopic(String topicName) {
        int minutes = minutesSinceLastUpdate(topicName);
        if (minutes==NEVER_UPDATED)
            return true;
        return (minutes >= Constants.READER_AUTO_UPDATE_DELAY_MINUTES);
    }

    private static final int NEVER_UPDATED = -1;
    private static int minutesSinceLastUpdate(String topicName) {
        if (TextUtils.isEmpty(topicName))
            return 0;

        String updated = getTopicLastUpdated(topicName);
        if (TextUtils.isEmpty(updated))
            return NEVER_UPDATED;

        Date dtUpdated = DateTimeUtils.iso8601ToJavaDate(updated);
        if (dtUpdated==null)
            return 0;

        Date dtNow = new Date();
        return DateTimeUtils.minutesBetween(dtUpdated, dtNow);
    }

    /**
     * recommended topics - stored in a separate table from default/subscribed topics, but have the same column names
     **/
    public static ReaderTopicList getRecommendedTopics(boolean excludeSubscribed) {
        Cursor c;
        if (excludeSubscribed) {
            c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_recommended_topics WHERE topic_name NOT IN (SELECT topic_name FROM tbl_topics) ORDER BY topic_name", null);
        } else {
            c = ReaderDatabase.getReadableDb().rawQuery("SELECT * FROM tbl_recommended_topics ORDER BY topic_name", null);
        }
        try {
            ReaderTopicList topics = new ReaderTopicList();
            if (c.moveToFirst()) {
                do {
                    topics.add(getTopicFromCursor(c));
                } while (c.moveToNext());
            }
            return topics;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static void setRecommendedTopics(ReaderTopicList topics) {
        if (topics==null)
            return;

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        SQLiteStatement stmt = db.compileStatement("INSERT INTO tbl_recommended_topics (" + COLUMN_NAMES + ") VALUES (?1,?2,?3)");
        db.beginTransaction();
        try {
            try {
                // first delete all recommended topics
                db.execSQL("DELETE FROM tbl_recommended_topics");

                // then insert the passed ones
                for (ReaderTopic topic: topics) {
                    stmt.bindString(1, topic.getTopicName());
                    stmt.bindString(2, topic.getEndpoint());
                    stmt.bindLong  (3, topic.topicType.toInt());
                    stmt.execute();
                    stmt.clearBindings();
                }

                db.setTransactionSuccessful();

            } catch (SQLException e) {
                ReaderLog.e(e);
            }
        } finally {
            SqlUtils.closeStatement(stmt);
            db.endTransaction();
        }
    }

}
