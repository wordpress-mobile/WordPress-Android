package org.wordpress.android;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import org.wordpress.android.datasets.StatsClicksTable;
import org.wordpress.android.datasets.StatsGeoviewsTable;
import org.wordpress.android.datasets.StatsMostCommentedTable;
import org.wordpress.android.datasets.StatsReferrersTable;
import org.wordpress.android.datasets.StatsSearchEngineTermsTable;
import org.wordpress.android.datasets.StatsTagsAndCategoriesTable;
import org.wordpress.android.datasets.StatsTopAuthorsTable;
import org.wordpress.android.datasets.StatsTopCommentersTable;
import org.wordpress.android.datasets.StatsTopPostsAndPagesTable;
import org.wordpress.android.datasets.StatsVideosTable;

public class WordPressStatsDB extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "wordpress_stats";
    
    private Context mContext;
    
    public WordPressStatsDB(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        mContext = ctx;
        getWritableDatabase();
    }
    
    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(StatsClicksTable.getInstance().toCreateQuery());
        db.execSQL(StatsGeoviewsTable.getInstance().toCreateQuery());
        db.execSQL(StatsMostCommentedTable.getInstance().toCreateQuery());
        db.execSQL(StatsReferrersTable.getInstance().toCreateQuery());
        db.execSQL(StatsSearchEngineTermsTable.getInstance().toCreateQuery());
        db.execSQL(StatsTagsAndCategoriesTable.getInstance().toCreateQuery());
        db.execSQL(StatsTopAuthorsTable.getInstance().toCreateQuery());
        db.execSQL(StatsTopCommentersTable.getInstance().toCreateQuery());
        db.execSQL(StatsTopPostsAndPagesTable.getInstance().toCreateQuery());
        db.execSQL(StatsVideosTable.getInstance().toCreateQuery());
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        
        onCreate(db);
        
        StatsClicksTable.getInstance().onUpgrade(db, oldVersion, newVersion);
        StatsGeoviewsTable.getInstance().onUpgrade(db, oldVersion, newVersion);
        StatsMostCommentedTable.getInstance().onUpgrade(db, oldVersion, newVersion);
        StatsReferrersTable.getInstance().onUpgrade(db, oldVersion, newVersion);
        StatsSearchEngineTermsTable.getInstance().onUpgrade(db, oldVersion, newVersion);
        StatsTagsAndCategoriesTable.getInstance().onUpgrade(db, oldVersion, newVersion);
        StatsTopAuthorsTable.getInstance().onUpgrade(db, oldVersion, newVersion);
        StatsTopCommentersTable.getInstance().onUpgrade(db, oldVersion, newVersion);
        StatsTopPostsAndPagesTable.getInstance().onUpgrade(db, oldVersion, newVersion);
        StatsVideosTable.getInstance().onUpgrade(db, oldVersion, newVersion);
    }

}
