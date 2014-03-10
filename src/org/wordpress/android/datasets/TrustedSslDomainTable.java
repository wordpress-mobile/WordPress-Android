package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;
import android.text.TextUtils;

import org.wordpress.android.WordPress;
import org.wordpress.android.util.SqlUtils;

import java.net.URI;

public class TrustedSslDomainTable {
    final private static String TABLE_NAME = "trusted_ssl_domains";

    private static SQLiteDatabase getDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static void createTable(SQLiteDatabase db) {
        // TODO; we should eventually store both md5 and sha1 hashes from the associated ssl certificate
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TABLE_NAME + " (" +
                   "domain TEXT COLLATE NOCASE PRIMARY KEY);");
    }

    public static void dropTable(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS" + TABLE_NAME);
    }

    public static int emptyTable() {
        return getDb().delete(TABLE_NAME, null, null);
    }

    public static boolean trustDomain(String domain) {
        if (TextUtils.isEmpty(domain)) {
            return false;
        }
        ContentValues cv = new ContentValues();
        cv.put("domain", domain);
        long res = getDb().insert(TABLE_NAME, null, cv);
        return res > 0;
    }

    public static boolean trustDomain(URI uri) {
        if (uri==null)
            return false;
        String domain = uri.getHost();
        return trustDomain(domain);
    }

    public static boolean removeTrustedDomain(String domain) {
        if (TextUtils.isEmpty(domain)) {
            return false;
        }
        int res = getDb().delete(TABLE_NAME, "domain=?", new String[]{domain});
        return res > 0;
    }

    public static boolean removeTrustedDomain(URI uri) {
        if (uri==null)
            return false;
        String domain = uri.getHost();
        return removeTrustedDomain(domain);
    }

    public static boolean isDomainTrusted(String domain) {
        return domain != null && SqlUtils.intForQuery(getDb(), "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE domain=?",
                new String[]{domain}) > 0;
    }
}
