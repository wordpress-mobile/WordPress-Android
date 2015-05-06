package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Account;
import org.wordpress.android.util.SqlUtils;

public class AccountTable {
    // Warning: the "accounts" table in WordPressDB is actually where blogs are stored.
    private static final String ACCOUNT_TABLE = "tbl_accounts";

    private static SQLiteDatabase getReadableDb() {
        return WordPress.wpDB.getDatabase();
    }
    private static SQLiteDatabase getWritableDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + ACCOUNT_TABLE + " ("
                + "local_id                INTEGER PRIMARY KEY DEFAULT 0,"
                + "user_name               TEXT,"
                + "user_id                 INTEGER DEFAULT 0,"
                + "display_name            TEXT,"
                + "profile_url             TEXT,"
                + "avatar_url              TEXT,"
                + "primary_blog_id         INTEGER DEFAULT 0,"
                + "site_count              INTEGER DEFAULT 0,"
                + "visible_site_count      INTEGER DEFAULT 0,"
                + "access_token            TEXT)");
    }

    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + ACCOUNT_TABLE);
    }

    public static void save(Account account) {
        save(account, getWritableDb());
    }

    public static void save(Account account, SQLiteDatabase database) {
        ContentValues values = new ContentValues();
        // we only support one wpcom user at the moment: local_id is always 0
        values.put("local_id", 0);
        values.put("user_name", account.getUserName());
        values.put("user_id", account.getUserId());
        values.put("display_name", account.getDisplayName());
        values.put("profile_url", account.getProfileUrl());
        values.put("avatar_url", account.getAvatarUrl());
        values.put("primary_blog_id", account.getPrimaryBlogId());
        values.put("site_count", account.getSiteCount());
        values.put("visible_site_count", account.getVisibleSiteCount());
        values.put("access_token", account.getAccessToken());
        database.insertWithOnConflict(ACCOUNT_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static Account getDefaultAccount() {
        return getAccountByLocalId(0);
    }

    private static Account getAccountByLocalId(long localId) {
        Account account = new Account();

        String[] args = {Long.toString(localId)};
        Cursor c = getReadableDb().rawQuery("SELECT * FROM " + ACCOUNT_TABLE + " WHERE local_id=?", args);

        try {
            if (c.moveToFirst()) {
                account.setUserName(c.getString(c.getColumnIndex("user_name")));
                account.setUserId(c.getLong(c.getColumnIndex("user_id")));
                account.setDisplayName(c.getString(c.getColumnIndex("display_name")));
                account.setProfileUrl(c.getString(c.getColumnIndex("profile_url")));
                account.setAvatarUrl(c.getString(c.getColumnIndex("avatar_url")));
                account.setPrimaryBlogId(c.getLong(c.getColumnIndex("primary_blog_id")));
                account.setSiteCount(c.getInt(c.getColumnIndex("site_count")));
                account.setVisibleSiteCount(c.getInt(c.getColumnIndex("visible_site_count")));
                account.setAccessToken(c.getString(c.getColumnIndex("access_token")));
            }
            return account;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }
}
