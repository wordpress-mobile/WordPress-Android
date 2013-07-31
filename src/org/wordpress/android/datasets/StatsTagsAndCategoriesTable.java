
package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

public class StatsTagsAndCategoriesTable extends SQLTable {

    private static final String NAME = "tags_and_categories";

    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String TOPIC = "topic";
        public static final String TYPE = "type";
        public static final String VIEWS = "views";
    }

    private static final class Holder {
        public static final StatsTagsAndCategoriesTable INSTANCE = new StatsTagsAndCategoriesTable();
    }

    public static synchronized StatsTagsAndCategoriesTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsTagsAndCategoriesTable() {}
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.DATE + ", " + Columns.TOPIC + ", " + Columns.TYPE + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.DATE, "DATE");
        map.put(Columns.TOPIC, "TEXT");
        map.put(Columns.TYPE, "TEXT");
        map.put(Columns.VIEWS, "INTEGER");
        return map;
    }

    @Override
    public void onUpgrade(int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }

}
