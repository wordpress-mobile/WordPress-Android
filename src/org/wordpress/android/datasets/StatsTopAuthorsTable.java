
package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

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
    public void onUpgrade(int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }

}
