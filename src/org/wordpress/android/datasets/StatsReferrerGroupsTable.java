package org.wordpress.android.datasets;

import java.util.LinkedHashMap;
import java.util.Map;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;

import org.wordpress.android.models.StatsReferrerGroup;
import org.wordpress.android.ui.stats.StatsTimeframe;

/**
 * A database table to represent groups in the stats for referrers.
 * A group may or may not have children. 
 * See {@link StatsReferrersTable} for the children table structure.  
 */
public class StatsReferrerGroupsTable extends SQLTable {

    private static final String NAME = "referrer_groups";
    
    public static final class Columns {
        public static final String BLOG_ID = "blogId";
        public static final String DATE = "date";
        public static final String GROUP_ID = "groupId";
        public static final String NAME = "name";
        public static final String TOTAL = "total";
        public static final String URL = "url";
        public static final String ICON = "icon";
        public static final String CHILDREN = "children";
    }

    private static final class Holder {
        public static final StatsReferrerGroupsTable INSTANCE = new StatsReferrerGroupsTable();
    }

    public static synchronized StatsReferrerGroupsTable getInstance() {
        return Holder.INSTANCE;
    }
    
    private StatsReferrerGroupsTable() {}
    
    @Override
    public String getName() {
        return NAME;
    }

    @Override
    protected String getUniqueConstraint() {
        return "UNIQUE (" + Columns.BLOG_ID + ", " + Columns.DATE + ", " + Columns.GROUP_ID + ") ON CONFLICT REPLACE";
    }

    @Override
    protected Map<String, String> getColumnMapping() {
        final Map<String, String> map = new LinkedHashMap<String, String>();
        map.put(BaseColumns._ID, "INTEGER PRIMARY KEY AUTOINCREMENT");
        map.put(Columns.BLOG_ID, "TEXT");
        map.put(Columns.DATE, "DATE");
        map.put(Columns.GROUP_ID, "TEXT");
        map.put(Columns.NAME, "TEXT");
        map.put(Columns.TOTAL, "TOTAL");
        map.put(Columns.URL, "TEXT");
        map.put(Columns.ICON, "TEXT");
        map.put(Columns.CHILDREN, "INTEGER");
        return map;
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // TODO Auto-generated method stub

    }

    public static ContentValues getContentValues(StatsReferrerGroup item) {
        ContentValues values = new ContentValues();
        values.put(Columns.BLOG_ID, item.getBlogId());
        values.put(Columns.DATE, item.getDate());
        values.put(Columns.GROUP_ID, item.getGroupId());
        values.put(Columns.NAME, item.getName());
        values.put(Columns.TOTAL, item.getTotal());
        values.put(Columns.URL, item.getUrl());
        values.put(Columns.ICON, item.getIcon());
        values.put(Columns.CHILDREN, item.getChildren());
        return values;
    }
    
    @Override
    public Cursor query(SQLiteDatabase database, Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
        
        String sort = NAME + "." + Columns.TOTAL + " DESC, " + NAME + "." + Columns.NAME + " ASC";
        
        String timeframe = uri.getQueryParameter("timeframe");
        if (timeframe == null)
            return super.query(database, uri, projection, selection, selectionArgs, sort);
        
        // get the latest for "Today", and the next latest for "Yesterday"
        if (timeframe.equals(StatsTimeframe.TODAY.name())) {
            return database.rawQuery("SELECT * FROM " + NAME +", " +
                            "(SELECT MAX(date) AS date FROM " + NAME + ") AS temp " +
                            "WHERE temp.date = " + NAME + ".date AND " + selection + " ORDER BY " + sort, selectionArgs);

        } else if (timeframe.equals(StatsTimeframe.YESTERDAY.name())) {
            return database.rawQuery(
                    "SELECT * FROM " + NAME + ", " +
                            "(SELECT MAX(date) AS date FROM " + NAME + ", " +
                                "( SELECT MAX(date) AS max FROM " + NAME + ")" +
                            " WHERE " + NAME + ".date < max) AS temp " + 
                    "WHERE " + NAME + ".date = temp.date AND " + selection + " ORDER BY " + sort, selectionArgs);
        }

        return super.query(database, uri, projection, selection, selectionArgs, sort);
    }
}
