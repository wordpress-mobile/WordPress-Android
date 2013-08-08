package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.wordpress.android.datasets.StatsBarChartDaysTable.Columns;
import org.wordpress.android.models.StatsBarChartMonth;

public class StatsBarChartMonthsTable extends SQLTable {

    private static final String NAME = "bar_chart_months";
    
    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String VIEWS = "views";
        public static final String VISITORS = "visitors";
    }
    
    private static final class Holder {
        public static final StatsBarChartMonthsTable INSTANCE = new StatsBarChartMonthsTable();
    }

    public static synchronized StatsBarChartMonthsTable getInstance() {
        return Holder.INSTANCE;
    }

    private StatsBarChartMonthsTable() {}
    
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
    
    public static ContentValues getContentValues(StatsBarChartMonth stat) {
        ContentValues values = new ContentValues();
        values.put(Columns.BLOG_ID, stat.getBlogId());
        values.put(Columns.DATE, stat.getDate());
        values.put(Columns.VIEWS, stat.getViews());
        values.put(Columns.VISITORS, stat.getVisitors());
        return values;
    }

    @Override
    public Cursor query(SQLiteDatabase database, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        return super.query(database, uri, projection, selection, selectionArgs, Columns.DATE + " DESC");
    }
}
