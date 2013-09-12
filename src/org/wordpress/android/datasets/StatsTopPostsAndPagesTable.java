package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.wordpress.android.models.StatsTopPostsAndPages;
import org.wordpress.android.ui.stats.StatsTimeframe;

/**
 * A database table to represent the stats for the top posts and pages.  
 */
public class StatsTopPostsAndPagesTable extends SQLTable {

    private static final String NAME = "top_post_and_pages";

    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String POST_ID = "postId";
        public static final String TITLE = "title";
        public static final String VIEWS = "views";
        public static final String URL = "url";
    }

    private static final class Holder {
        public static final StatsTopPostsAndPagesTable INSTANCE = new StatsTopPostsAndPagesTable();
    }

    public static synchronized StatsTopPostsAndPagesTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsTopPostsAndPagesTable() {}
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.DATE + ", " + Columns.POST_ID + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.DATE, "DATE");
        map.put(Columns.POST_ID, "INTEGER");
        map.put(Columns.TITLE, "TEXT");
        map.put(Columns.VIEWS, "INTEGER");
        map.put(Columns.URL, "TEXT");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }
    
    public static ContentValues getContentValues(StatsTopPostsAndPages item) {
        ContentValues values = new ContentValues();
        values.put(Columns.BLOG_ID, item.getBlogId());
        values.put(Columns.DATE, item.getDate());
        values.put(Columns.POST_ID, item.getPostId());
        values.put(Columns.TITLE, item.getTitle());
        values.put(Columns.VIEWS, item.getViews());
        values.put(Columns.URL, item.getUrl());
        return values;
    }

    @Override
    public Cursor query(SQLiteDatabase database, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String sort = NAME + "." + Columns.VIEWS + " DESC, " + NAME + "." + Columns.TITLE + " ASC";
        
        String timeframe = uri.getQueryParameter("timeframe");
        if (timeframe == null)
            return super.query(database, uri, projection, selection, selectionArgs, sort);
        
        // get the latest for "Today", and the next latest for "Yesterday"
        if (timeframe.equals(StatsTimeframe.TODAY.name())) {
            return database.rawQuery("SELECT * FROM " + NAME +", " +
                            "(SELECT MAX(date) AS date FROM " + NAME + ") AS temp " +
                            "WHERE temp.date = " + NAME + ".date AND " + selection + " ORDER BY " + sort, selectionArgs);

        } else if (timeframe.equals(StatsTimeframe.YESTERDAY.name())) {
            return database.rawQuery(
                    "SELECT * FROM " + NAME + ", " +
                            "(SELECT MAX(date) AS date FROM " + NAME + ", " +
                                "( SELECT MAX(date) AS max FROM " + NAME + ")" +
                            " WHERE " + NAME + ".date < max) AS temp " + 
                    "WHERE temp.date = " + NAME + ".date AND " + selection + " ORDER BY " + sort, selectionArgs);
        }

        return super.query(database, uri, projection, selection, selectionArgs, sort);
    }
}
