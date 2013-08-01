
package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.models.StatsReferrer;

public class StatsReferrersTable extends SQLTable {

    private static final String NAME = "referrers";

    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String TITLE = "title";
        public static final String VIEWS = "views";
        public static final String URL = "url";
        public static final String IMAGE_URL = "imageUrl";
    }

    private static final class Holder {
        public static final StatsReferrersTable INSTANCE = new StatsReferrersTable();
    }

    public static synchronized StatsReferrersTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsReferrersTable() {}
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.DATE + ", " + Columns.TITLE + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.DATE, "DATE");
        map.put(Columns.TITLE, "TEXT");
        map.put(Columns.VIEWS, "INTEGER");
        map.put(Columns.URL, "TEXT");
        map.put(Columns.IMAGE_URL, "TEXT");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }
    
    public static ContentValues getContentValues(StatsReferrer item) {
        ContentValues values = new ContentValues();
        values.put(Columns.BLOG_ID, item.getBlogId());
        values.put(Columns.DATE, item.getDate());
        values.put(Columns.TITLE, item.getTitle());
        values.put(Columns.VIEWS, item.getViews());
        values.put(Columns.URL, item.getUrl());
        values.put(Columns.IMAGE_URL, item.getImageUrl());
        return values;
    }

}
