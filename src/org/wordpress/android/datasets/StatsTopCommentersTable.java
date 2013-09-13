package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.wordpress.android.models.StatsTopCommenter;

/**
 * A database table to represent the stats for top commenters.  
 */
public class StatsTopCommentersTable extends SQLTable {

    private static final String NAME = "top_commenters";

    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String USER_ID = "userId";
        public static final String NAME = "name";
        public static final String COMMENTS = "comments";
        public static final String IMAGE_URL = "imageUrl";
    }

    private static final class Holder {
        public static final StatsTopCommentersTable INSTANCE = new StatsTopCommentersTable();
    }

    public static synchronized StatsTopCommentersTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsTopCommentersTable() {}
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.USER_ID + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.USER_ID, "TEXT");
        map.put(Columns.NAME, "TEXT");
        map.put(Columns.COMMENTS, "INTEGER");
        map.put(Columns.IMAGE_URL, "TEXT");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }
    
    public static ContentValues getContentValues(StatsTopCommenter item) {
        ContentValues values = new ContentValues();
        values.put(Columns.BLOG_ID, item.getBlogId());
        values.put(Columns.USER_ID, item.getUserId());
        values.put(Columns.NAME, item.getName());
        values.put(Columns.COMMENTS, item.getComments());
        values.put(Columns.IMAGE_URL, item.getImageUrl());
        return values;
    }

    @Override
    public Cursor query(SQLiteDatabase database, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return super.query(database, uri, projection, selection, selectionArgs, Columns.COMMENTS + " DESC, " + Columns.NAME + " ASC");
    }
}
