
package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

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
    public void onUpgrade(int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }

}
