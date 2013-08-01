
package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.database.sqlite.SQLiteDatabase;

public class StatsGeoviewsTable extends SQLTable {

    private static final String NAME = "geoviews";

    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String COUNTRY = "country";
        public static final String VIEWS = "views";
    }

    private static final class Holder {
        public static final StatsGeoviewsTable INSTANCE = new StatsGeoviewsTable();
    }

    public static synchronized StatsGeoviewsTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsGeoviewsTable() {}
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID  + ", " + Columns.DATE + ", " + Columns.COUNTRY + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.DATE, "DATE");
        map.put(Columns.COUNTRY, "TEXT");
        map.put(Columns.VIEWS, "INTEGER");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }

}
