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

    public static void trustDomain(String domain) {
        if (TextUtils.isEmpty(domain)) {
            return;
        }
        ContentValues cv = new ContentValues();
        cv.put("domain", domain);
        getDb().insert(TABLE_NAME, null, cv);
    }

    public static void trustDomain(URI uri) {
        String domain = uri.getHost();
        trustDomain(domain);
    }

    public static int removeTrustedDomain(String domain) {
        return getDb().delete(TABLE_NAME, "domain=?", new String[]{domain});
    }

    public static void removeTrustedDomain(URI uri) {
        String domain = uri.getHost();
        removeTrustedDomain(domain);
    }

    public static boolean isDomainTrusted(String domain) {
        return domain != null && SqlUtils.intForQuery(getDb(), "SELECT COUNT(*) FROM " + TABLE_NAME + " WHERE domain=?",
                new String[]{domain}) > 0;
    }
}
