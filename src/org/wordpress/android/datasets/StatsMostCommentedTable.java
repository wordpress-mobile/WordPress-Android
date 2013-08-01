
package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.database.sqlite.SQLiteDatabase;

public class StatsMostCommentedTable extends SQLTable {

    private static final String NAME = "top_post_and_pages";

    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String POST_ID = "postId";
        public static final String POST = "post";
        public static final String COMMENTS = "comments";
        public static final String URL = "url";
    }

    private static final class Holder {
        public static final StatsMostCommentedTable INSTANCE = new StatsMostCommentedTable();
    }

    public static synchronized StatsMostCommentedTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsMostCommentedTable() {}
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.POST_ID + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.POST_ID, "INTEGER");
        map.put(Columns.POST, "TEXT");
        map.put(Columns.COMMENTS, "INTEGER");
        map.put(Columns.URL, "TEXT");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }

}
