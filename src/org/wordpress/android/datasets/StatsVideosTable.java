
package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

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
    public void onUpgrade(int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }

}
