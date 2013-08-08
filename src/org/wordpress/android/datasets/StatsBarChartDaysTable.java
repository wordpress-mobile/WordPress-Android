package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.wordpress.android.models.StatsBarChartDay;

public class StatsBarChartDaysTable extends SQLTable {

    private static final String NAME = "bar_chart_days";
    
    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String VIEWS = "views";
        public static final String VISITORS = "visitors";
    }
    
    private static final class Holder {
        public static final StatsBarChartDaysTable INSTANCE = new StatsBarChartDaysTable();
    }

    public static synchronized StatsBarChartDaysTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsBarChartDaysTable() {}
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.DATE + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.DATE, "DATE");
        map.put(Columns.VIEWS, "INTEGER");
        map.put(Columns.VISITORS, "INTEGER");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub

    }
    
    public static ContentValues getContentValues(StatsBarChartDay item) {
        ContentValues values = new ContentValues();
        values.put(Columns.BLOG_ID, item.getBlogId());
        values.put(Columns.DATE, item.getDate());
        values.put(Columns.VIEWS, item.getViews());
        values.put(Columns.VISITORS, item.getVisitors());
        return values;
    }

    @Override
    public Cursor query(SQLiteDatabase database, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return super.query(database, uri, projection, selection, selectionArgs, Columns.DATE + " DESC");
    }
}
