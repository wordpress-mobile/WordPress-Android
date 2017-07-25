package org.wordpress.android.datasets;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;

import org.wordpress.android.models.Theme;

public class ThemeTable {
    private static final String COLUMN_NAME_ID = "_id";
    private static final String THEMES_TABLE = "themes";
    private static final String DROP_TABLE_PREFIX = "DROP TABLE IF EXISTS ";
    private static final String CREATE_TABLE_THEMES = "create table if not exists themes ("
            + COLUMN_NAME_ID + " integer primary key autoincrement, "
            + Theme.ID + " text, "
            + Theme.AUTHOR + " text, "
            + Theme.SCREENSHOT + " text, "
            + Theme.AUTHOR_URI + " text, "
            + Theme.DEMO_URI + " text, "
            + Theme.NAME + " text, "
            + Theme.STYLESHEET + " text, "
            + Theme.PRICE + " text, "
            + Theme.BLOG_ID + " text, "
            + Theme.IS_CURRENT + " boolean default false);";

    public static void createTables(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_THEMES);
    }

    public static void resetTables(SQLiteDatabase db) {
        db.execSQL(DROP_TABLE_PREFIX + THEMES_TABLE);
        db.execSQL(CREATE_TABLE_THEMES);
    }

    private static final Object mSyncObject = new Object();
    public static boolean saveTheme(SQLiteDatabase db, Theme theme) {
        boolean returnValue = false;

        ContentValues values = new ContentValues();
        values.put(Theme.ID, theme.getId());
        values.put(Theme.AUTHOR, theme.getAuthor());
        values.put(Theme.SCREENSHOT, theme.getScreenshot());
        values.put(Theme.AUTHOR_URI, theme.getAuthorURI());
        values.put(Theme.DEMO_URI, theme.getDemoURI());
        values.put(Theme.NAME, theme.getName());
        values.put(Theme.STYLESHEET, theme.getStylesheet());
        values.put(Theme.PRICE, theme.getPrice());
        values.put(Theme.BLOG_ID, theme.getBlogId());
        values.put(Theme.IS_CURRENT, theme.getIsCurrent() ? 1 : 0);

        synchronized (mSyncObject) {
            int result = db.update(
                    THEMES_TABLE,
                    values,
                    Theme.ID + "=?",
                    new String[]{theme.getId()});
            if (result == 0)
                returnValue = db.insert(THEMES_TABLE, null, values) > 0;
        }

        return (returnValue);
    }

    public static Cursor getThemesAll(SQLiteDatabase db, String blogId) {
        String[] columns = {COLUMN_NAME_ID, Theme.ID, Theme.NAME, Theme.SCREENSHOT, Theme.PRICE, Theme.IS_CURRENT};
        String[] selection = {blogId};

        return db.query(THEMES_TABLE, columns, Theme.BLOG_ID + "=?", selection, null, null, null);
    }

    public static Cursor getThemesFree(SQLiteDatabase db, String blogId) {
        String[] columns = {COLUMN_NAME_ID, Theme.ID, Theme.NAME, Theme.SCREENSHOT, Theme.PRICE, Theme.IS_CURRENT};
        String[] selection = {blogId, ""};

        return db.query(THEMES_TABLE, columns, Theme.BLOG_ID + "=? AND " + Theme.PRICE + "=?", selection, null, null, null);
    }

    public static Cursor getThemesPremium(SQLiteDatabase db, String blogId) {
        String[] columns = {COLUMN_NAME_ID, Theme.ID, Theme.NAME, Theme.SCREENSHOT, Theme.PRICE, Theme.IS_CURRENT};
        String[] selection = {blogId, ""};

        return db.query(THEMES_TABLE, columns, Theme.BLOG_ID + "=? AND " + Theme.PRICE + "!=?", selection, null, null, null);
    }

    public static String getCurrentThemeId(SQLiteDatabase db, String blogId) {
        String[] selection = {blogId, String.valueOf(1)};
        String currentThemeId;
        try {
            currentThemeId = DatabaseUtils.stringForQuery(db, "SELECT " + Theme.ID + " FROM " + THEMES_TABLE + " WHERE " + Theme.BLOG_ID + "=? and " + Theme.IS_CURRENT + "=?", selection);
        } catch (SQLiteException e) {
            currentThemeId = "";
        }

        return currentThemeId;
    }

    public static void setCurrentTheme(SQLiteDatabase db, String blogId, String id) {
        // update any old themes that are set to true to false
        ContentValues values = new ContentValues();
        values.put(Theme.IS_CURRENT, false);
        db.update(THEMES_TABLE, values, Theme.BLOG_ID + "=?", new String[] { blogId });

        values = new ContentValues();
        values.put(Theme.IS_CURRENT, true);
        db.update(THEMES_TABLE, values, Theme.BLOG_ID + "=? AND " + Theme.ID + "=?", new String[] { blogId, id });
    }

    public static int getThemeCount(SQLiteDatabase db, String blogId) {
        return getThemesAll(db, blogId).getCount();
    }

    public static Cursor getThemes(SQLiteDatabase db, String blogId, String searchTerm) {
        String[] columns = {COLUMN_NAME_ID, Theme.ID, Theme.NAME, Theme.SCREENSHOT, Theme.PRICE, Theme.IS_CURRENT};
        String[] selection = {blogId, "%" + searchTerm + "%"};

        return db.query(THEMES_TABLE, columns, Theme.BLOG_ID + "=? AND " + Theme.NAME + " LIKE ?", selection, null, null, null);
    }

    public static Theme getTheme(SQLiteDatabase db, String blogId, String themeId) {
        String[] columns = {COLUMN_NAME_ID, Theme.ID, Theme.AUTHOR, Theme.SCREENSHOT, Theme.AUTHOR_URI, Theme.DEMO_URI, Theme.NAME, Theme.STYLESHEET, Theme.PRICE, Theme.IS_CURRENT};
        String[] selection = {blogId, themeId};
        Cursor cursor = db.query(THEMES_TABLE, columns, Theme.BLOG_ID + "=? AND " + Theme.ID + "=?", selection, null, null, null);

        if (cursor.moveToFirst()) {
            String id = cursor.getString(cursor.getColumnIndex(Theme.ID));
            String author = cursor.getString(cursor.getColumnIndex(Theme.AUTHOR));
            String screenshot = cursor.getString(cursor.getColumnIndex(Theme.SCREENSHOT));
            String authorURI = cursor.getString(cursor.getColumnIndex(Theme.AUTHOR_URI));
            String demoURI = cursor.getString(cursor.getColumnIndex(Theme.DEMO_URI));
            String name = cursor.getString(cursor.getColumnIndex(Theme.NAME));
            String stylesheet = cursor.getString(cursor.getColumnIndex(Theme.STYLESHEET));
            String price = cursor.getString(cursor.getColumnIndex(Theme.PRICE));
            boolean isCurrent = cursor.getInt(cursor.getColumnIndex(Theme.IS_CURRENT)) > 0;

            Theme theme = new Theme(id, author, screenshot, authorURI, demoURI, name, stylesheet, price, blogId, isCurrent);
            cursor.close();

            return theme;
        } else {
            cursor.close();
            return null;
        }
    }

    public static Theme getCurrentTheme(SQLiteDatabase db, String blogId) {
        String currentThemeId = getCurrentThemeId(db, blogId);

        return getTheme(db, blogId, currentThemeId);
    }
}
