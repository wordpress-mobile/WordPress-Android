
package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.database.sqlite.SQLiteDatabase;

public class StatsClicksTable extends SQLTable {

    private static final String NAME = "clicks";

    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String CLICKS = "clicks";
        public static final String URL = "url";
        public static final String IMAGE_URL = "imageUrl";
    }
    
    private static final class Holder {
        public static final StatsClicksTable INSTANCE = new StatsClicksTable();
    }

    public static synchronized StatsClicksTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsClicksTable() {}

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.DATE + ", " + Columns.URL + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.DATE, "DATE");
        map.put(Columns.CLICKS, "INTEGER");
        map.put(Columns.URL, "TEXT");
        map.put(Columns.IMAGE_URL, "TEXT");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }

}
