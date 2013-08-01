
package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.sqlite.SQLiteDatabase;

import org.wordpress.android.models.StatsSearchEngineTerm;

public class StatsSearchEngineTermsTable extends SQLTable {

    private static final String NAME = "search_engine_terms";

    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String SEARCH = "search";
        public static final String VIEWS = "views";
    }

    private static final class Holder {
        public static final StatsSearchEngineTermsTable INSTANCE = new StatsSearchEngineTermsTable();
    }

    public static synchronized StatsSearchEngineTermsTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsSearchEngineTermsTable() {}
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.DATE + ", " + Columns.SEARCH + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.DATE, "DATE");
        map.put(Columns.SEARCH, "TEXT");
        map.put(Columns.VIEWS, "INTEGER");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub
        
    }
    
    public static ContentValues getContentValues(StatsSearchEngineTerm item) {
        ContentValues values = new ContentValues();
        values.put(Columns.BLOG_ID, item.getBlogId());
        values.put(Columns.DATE, item.getDate());
        values.put(Columns.SEARCH, item.getSearch());
        values.put(Columns.VIEWS, item.getViews());
        return values;
    }

}
