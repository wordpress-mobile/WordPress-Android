package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.wordpress.android.models.StatsVideo;
import org.wordpress.android.ui.stats.StatsTimeframe;

/**
 * A database table to represent the stats for videos.  
 */
public class StatsVideosTable extends SQLTable {

    private static final String NAME = "videos";

    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String VIDEO_ID = "videoId";
        public static final String NAME = "name";
        public static final String PLAYS = "plays";
        public static final String URL = "url";
    }
    
    private static final class Holder {
        public static final StatsVideosTable INSTANCE = new StatsVideosTable();
    }

    public static synchronized StatsVideosTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsVideosTable() {}

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.DATE + ", " + Columns.VIDEO_ID + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.DATE, "DATE");
        map.put(Columns.VIDEO_ID, "INTEGER");
        map.put(Columns.NAME, "TEXT");
        map.put(Columns.PLAYS, "INTEGER");
        map.put(Columns.URL, "TEXT");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }

    public static ContentValues getContentValues(StatsVideo item) {
        ContentValues values = new ContentValues();
        values.put(Columns.BLOG_ID, item.getBlogId());
        values.put(Columns.DATE, item.getDate());
        values.put(Columns.VIDEO_ID, item.getVideoId());
        values.put(Columns.NAME, item.getName());
        values.put(Columns.PLAYS, item.getPlays());
        values.put(Columns.URL, item.getUrl());
        return values;
    }

    @Override
    public Cursor query(SQLiteDatabase database, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        String sort = NAME + "." + Columns.PLAYS + " DESC, " + NAME + "." + Columns.NAME + " ASC";
        
        String timeframe = uri.getQueryParameter("timeframe");
        if (timeframe == null)
            return super.query(database, uri, projection, selection, selectionArgs, sort);
        
        // get the latest for "Today", and the next latest for "Yesterday"
        if (timeframe.equals(StatsTimeframe.TODAY.name())) {
            return database.rawQuery("SELECT * FROM " + NAME +", " +
                            "(SELECT MAX(date) AS date FROM " + NAME + ") AS temp " +
                            "WHERE temp.date = " + NAME + ".date ORDER BY " + sort, null);

        } else if (timeframe.equals(StatsTimeframe.YESTERDAY.name())) {
            return database.rawQuery(
                    "SELECT * FROM " + NAME + ", " +
                            "(SELECT MAX(date) AS date FROM " + NAME + ", " +
                                "( SELECT MAX(date) AS max FROM " + NAME + ")" +
                            " WHERE " + NAME + ".date < max) AS temp " + 
                    "WHERE " + NAME + ".date = temp.date ORDER BY " + sort, null);
        }

        return super.query(database, uri, projection, selection, selectionArgs, sort);
    }
}
