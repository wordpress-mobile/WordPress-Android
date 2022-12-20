package org.wordpress.android.datasets;

import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;

import org.wordpress.android.models.ReaderPost;
import org.wordpress.android.util.SqlUtils;

public class BlockedAuthorTable {
    protected static final String BLOCKED_AUTHORS_TABLE = "tbl_blocked_authors";
    private static final String AUTHOR_ID = "author_id";

    protected static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + BLOCKED_AUTHORS_TABLE + " ("
                   + AUTHOR_ID + " INTEGER DEFAULT 0,"
                   + " PRIMARY KEY (" + AUTHOR_ID + ")"
                   + ")");
    }

    protected static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + BLOCKED_AUTHORS_TABLE);
    }

    protected static void reset(SQLiteDatabase db) {
        dropTables(db);
        createTables(db);
    }

    public static void addBlockedAuthor(long authorId) {
        SQLiteStatement stmt = null;
        try {
            stmt = ReaderDatabase.getWritableDb().compileStatement(
                    "INSERT OR REPLACE INTO " + BLOCKED_AUTHORS_TABLE + " (" + AUTHOR_ID + ") VALUES (?1)");

            stmt.bindString(1, Long.toString(authorId));
            stmt.execute();
        } finally {
            SqlUtils.closeStatement(stmt);
        }
    }

    public static boolean isBlockedAuthor(long authorId) {
        if (authorId == 0) {
            return false;
        }
        return SqlUtils.intForQuery(ReaderDatabase.getReadableDb(),
                "SELECT count(*) FROM " + BLOCKED_AUTHORS_TABLE + " WHERE " + AUTHOR_ID + "=?",
                new String[]{Long.toString(authorId)}) > 0;
    }

    public static void removeBlockedAuthor(long authorId) {
        if (authorId == 0) {
            return;
        }
        String[] args = new String[]{Long.toString(authorId)};
        ReaderDatabase.getWritableDb().delete(BLOCKED_AUTHORS_TABLE, AUTHOR_ID + "=?", args);
    }

    public static void blacklistAuthorLocally(long authorId) {
        BlockedAuthorTable.addBlockedAuthor(authorId);
    }

    public static void whitelistAuthorLocally(long authorId) {
        BlockedAuthorTable.removeBlockedAuthor(authorId);
    }

    public static boolean isBlockedAuthor(ReaderPost post) {
        return BlockedAuthorTable.isBlockedAuthor(post.authorId);
    }
}
