package org.wordpress.android.datasets;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.models.ReaderUrlList;
import org.wordpress.android.models.ReaderUser;
import org.wordpress.android.models.ReaderUserIdList;
import org.wordpress.android.models.ReaderUserList;
import org.wordpress.android.ui.prefs.ReaderPrefs;
import org.wordpress.android.util.SqlUtils;

/**
 * Created by nbradbury on 6/22/13.
 * stores info about the current user and liking users
 */
public class ReaderUserTable {
    private static final String COLUMN_NAMES =
            "user_id, user_name, display_name, url, profile_url, avatar_url";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE tbl_users ("
                + "	user_id	        INTEGER PRIMARY KEY,"
                + "	user_name	    TEXT,"
                + "	display_name	TEXT COLLATE NOCASE,"
                + " url             TEXT,"
                + " profile_url     TEXT,"
                + " avatar_url      TEXT)");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS tbl_users");
    }

    public static void addOrUpdateUser(ReaderUser user) {
        if (user==null)
            return;

        ReaderUserList users = new ReaderUserList();
        users.add(user);
        addOrUpdateUsers(users);
    }

    public static void addOrUpdateUsers(ReaderUserList users) {
        if (users==null || users.size()==0)
            return;

        SQLiteDatabase db = ReaderDatabase.getWritableDb();
        db.beginTransaction();
        SQLiteStatement stmt = db.compileStatement("INSERT OR REPLACE INTO tbl_users (" + COLUMN_NAMES + ") VALUES (?1,?2,?3,?4,?5,?6)");
        try {
            for (ReaderUser user: users) {
                stmt.bindLong  (1, user.userId);
                stmt.bindString(2, user.getUserName());
                stmt.bindString(3, user.getDisplayName());
                stmt.bindString(4, user.getUrl());
                stmt.bindString(5, user.getProfileUrl());
                stmt.bindString(6, user.getAvatarUrl());
                stmt.execute();
                stmt.clearBindings();
            }

            db.setTransactionSuccessful();

        } finally {
            db.endTransaction();
            SqlUtils.closeStatement(stmt);
        }
    }

    /*
     * returns avatar urls for the passed user ids - used by post detail to show avatars for liking users
     */
    public static ReaderUrlList getAvatarsUrls(ReaderUserIdList userIds, int max) {
        ReaderUrlList avatars = new ReaderUrlList();
        if (userIds==null || userIds.size()==0)
            return avatars;

        StringBuilder sb = new StringBuilder("SELECT avatar_url FROM tbl_users WHERE user_id IN (");

        // make sure current user's avatar is returned if the passed list contains them - this is
        // important since it may not otherwise be returned when a "max" is passed, and we want
        // the current user to appear in post detail when they like a post
        long currentUserId = ReaderPrefs.getCurrentUserId();
        boolean containsCurrentUser = userIds.contains(currentUserId);
        if (containsCurrentUser)
            sb.append(currentUserId);

        int numAdded = (containsCurrentUser ? 1 : 0);
        for (Long id: userIds) {
            // skip current user since we added them already
            if (id!=currentUserId) {
                if (numAdded > 0)
                    sb.append(",");
                sb.append(id);
                numAdded++;
                if (max > 0 && numAdded >= max)
                    break;
            }
        }
        sb.append(")");

        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sb.toString(), null);
        try {
            if (c.moveToFirst()) {
                do {
                    avatars.add(c.getString(0));
                } while (c.moveToNext());
            }
            return avatars;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static ReaderUser getCurrentUser() {
        return getUser(ReaderPrefs.getCurrentUserId());
    }

    public static ReaderUser getUser(long userId) {
        String args[] = {Long.toString(userId)};
        Cursor c = ReaderDatabase.getReadableDb().rawQuery("SELECT " + COLUMN_NAMES + " FROM tbl_users WHERE user_id=?", args);
        try {
            if (!c.moveToFirst())
                return null;
            return getUserFromCursor(c);
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    private static String getAvatarForUser(long userId) {
        String args[] = {Long.toString(userId)};
        return SqlUtils.stringForQuery(ReaderDatabase.getReadableDb(), "SELECT avatar_url FROM tbl_users WHERE user_id=?", args);
    }

    public static ReaderUserList getUsersWhoLikePost(ReaderPost post, int max) {
        if (post==null)
            return new ReaderUserList();

        String[] args = {Long.toString(post.blogId), Long.toString(post.postId)};
        String sql = "SELECT " + COLUMN_NAMES + " from tbl_users WHERE user_id IN (SELECT user_id FROM tbl_post_likes WHERE blog_id=? AND post_id=?) ORDER BY display_name";
        if (max > 0)
            sql += " LIMIT " + Integer.toString(max);

        Cursor c = ReaderDatabase.getReadableDb().rawQuery(sql, args);
        try {
            ReaderUserList users = new ReaderUserList();
            if (c.moveToFirst()) {
                do {
                    users.add(getUserFromCursor(c));
                } while (c.moveToNext());
            }
            return users;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    private static final int COL_USER_ID = 0;
    private static final int COL_USER_NAME = 1;
    private static final int COL_DISPLAY_NAME = 2;
    private static final int COL_URL = 3;
    private static final int COL_PROFILE_URL = 4;
    private static final int COL_AVATAR_URL = 5;

    public static ReaderUser getUserFromCursor(Cursor c) {
        ReaderUser user = new ReaderUser();

        user.userId = c.getLong(COL_USER_ID);
        user.setUserName(c.getString(COL_USER_NAME));
        user.setDisplayName(c.getString(COL_DISPLAY_NAME));
        user.setUrl(c.getString(COL_URL));
        user.setProfileUrl(c.getString(COL_PROFILE_URL));
        user.setAvatarUrl(c.getString(COL_AVATAR_URL));

        return user;
    }
}
