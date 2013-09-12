package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.wordpress.android.models.StatsTopAuthor;
import org.wordpress.android.ui.stats.StatsTimeframe;

/**
 * A database table to represent the stats for the top authors.  
 */
public class StatsTopAuthorsTable extends SQLTable {

    private static final String NAME = "top_authors";

    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String USER_ID = "userId";
        public static final String NAME = "name";
        public static final String VIEWS = "views";
        public static final String IMAGE_URL = "imageUrl";
    }

    private static final class Holder {
        public static final StatsTopAuthorsTable INSTANCE = new StatsTopAuthorsTable();
    }

    public static synchronized StatsTopAuthorsTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsTopAuthorsTable() {}
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.DATE + ", " + Columns.USER_ID + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.DATE, "DATE");
        map.put(Columns.USER_ID, "TEXT");
        map.put(Columns.NAME, "TEXT");
        map.put(Columns.VIEWS, "INTEGER");
        map.put(Columns.IMAGE_URL, "TEXT");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }
    
    public static ContentValues getContentValues(StatsTopAuthor item) {
        ContentValues values = new ContentValues();
        values.put(Columns.BLOG_ID, item.getBlogId());
        values.put(Columns.DATE, item.getDate());
        values.put(Columns.USER_ID, item.getUserId());
        values.put(Columns.NAME, item.getName());
        values.put(Columns.VIEWS, item.getViews());
        values.put(Columns.IMAGE_URL, item.getImageUrl());
        return values;
    }

    @Override
    public Cursor query(SQLiteDatabase database, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String sort = NAME + "." + Columns.VIEWS + " DESC, " + NAME + "." + Columns.NAME + " ASC";
        
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
