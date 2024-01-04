package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.WordPress;
import org.wordpress.android.models.Tag;
import org.wordpress.android.models.UserSuggestion;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.SqlUtils;

import java.util.ArrayList;
import java.util.List;

public class UserSuggestionTable {
    private static final String SUGGESTIONS_TABLE = "suggestions";
    private static final String TAXONOMY_TABLE = "taxonomy";

    public static void createTables(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS " + SUGGESTIONS_TABLE + " ("
                   + " site_id INTEGER DEFAULT 0,"
                   + " user_login TEXT,"
                   + " display_name TEXT,"
                   + " image_url TEXT,"
                   + " taxonomy TEXT,"
                   + " PRIMARY KEY (user_login)"
                   + " );");
        db.execSQL("CREATE TABLE IF NOT EXISTS " + TAXONOMY_TABLE + " ("
                   + " site_id INTEGER DEFAULT 0,"
                   + " tag TEXT,"
                   + " PRIMARY KEY (site_id, tag)"
                   + " );");
    }

    private static void dropTables(SQLiteDatabase db) {
        db.execSQL("DROP TABLE IF EXISTS " + SUGGESTIONS_TABLE);
        db.execSQL("DROP TABLE IF EXISTS " + TAXONOMY_TABLE);
    }

    public static void reset(SQLiteDatabase db) {
        AppLog.i(AppLog.T.SUGGESTION, "resetting suggestion tables");
        dropTables(db);
        createTables(db);
    }

    private static SQLiteDatabase getReadableDb() {
        return WordPress.wpDB.getDatabase();
    }

    private static SQLiteDatabase getWritableDb() {
        return WordPress.wpDB.getDatabase();
    }

    public static void insertSuggestionsForSite(final long siteId, final List<UserSuggestion> suggestions) {
        // we want to delete the current suggestions, so that removed users will not show up as a suggestion
        deleteSuggestionsForSite(siteId);

        // Including the insertion of all suggestions in a single transaction dramatically improves insertion
        // performance when there are a lot of suggestions
        getWritableDb().beginTransaction();
        if (suggestions != null) {
            for (UserSuggestion suggestion : suggestions) {
                addSuggestion(suggestion);
            }
        }
        getWritableDb().setTransactionSuccessful();
        getWritableDb().endTransaction();
    }

    public static void addSuggestion(final UserSuggestion suggestion) {
        if (suggestion == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("site_id", suggestion.siteID);
        values.put("user_login", suggestion.getUserLogin());
        values.put("display_name", suggestion.getDisplayName());
        values.put("image_url", suggestion.getImageUrl());
        values.put("taxonomy", suggestion.getTaxonomy());

        getWritableDb().insertWithOnConflict(SUGGESTIONS_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static List<UserSuggestion> getSuggestionsForSite(long siteId) {
        List<UserSuggestion> suggestions = new ArrayList<UserSuggestion>();

        String[] args = {Long.toString(siteId)};
        Cursor c = getReadableDb()
                .rawQuery("SELECT * FROM " + SUGGESTIONS_TABLE + " WHERE site_id=? ORDER BY user_login ASC", args);

        try {
            if (c.moveToFirst()) {
                do {
                    UserSuggestion comment = getSuggestionFromCursor(c);
                    suggestions.add(comment);
                } while (c.moveToNext());
            }

            return suggestions;
        } finally {
            SqlUtils.closeCursor(c);
        }
    }

    public static int deleteSuggestionsForSite(long siteId) {
        return getWritableDb().delete(SUGGESTIONS_TABLE, "site_id=?", new String[]{Long.toString(siteId)});
    }

    private static UserSuggestion getSuggestionFromCursor(Cursor c) {
        final String userLogin = c.getString(c.getColumnIndexOrThrow("user_login"));
        final String displayName = c.getString(c.getColumnIndexOrThrow("display_name"));
        final String imageUrl = c.getString(c.getColumnIndexOrThrow("image_url"));
        final String taxonomy = c.getString(c.getColumnIndexOrThrow("taxonomy"));

        long siteId = c.getLong(c.getColumnIndexOrThrow("site_id"));

        return new UserSuggestion(
                siteId,
                userLogin,
                displayName,
                imageUrl,
                taxonomy);
    }

    public static void insertTagsForSite(final long siteId, final List<Tag> tags) {
        // we want to delete the current tags, so that removed tags will not show up
        deleteTagsForSite(siteId);

        if (tags != null) {
            for (Tag tag : tags) {
                addTag(tag);
            }
        }
    }

    public static void addTag(final Tag tag) {
        if (tag == null) {
            return;
        }

        ContentValues values = new ContentValues();
        values.put("site_id", tag.siteID);
        values.put("tag", tag.getTag());

        getWritableDb().insertWithOnConflict(TAXONOMY_TABLE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public static int deleteTagsForSite(long siteId) {
        return getWritableDb().delete(TAXONOMY_TABLE, "site_id=?", new String[]{Long.toString(siteId)});
    }
}
