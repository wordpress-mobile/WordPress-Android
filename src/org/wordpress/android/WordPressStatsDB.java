package org.wordpress.android;

import java.util.Random;

import android.annotation.SuppressLint;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
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
import org.wordpress.android.models.Stat;
import org.wordpress.android.ui.stats.Stats;
import org.wordpress.android.ui.stats.Stats.Category;

public class WordPressStatsDB extends SQLiteOpenHelper{

    private static final int DATABASE_VERSION = 1;
    private static final String DATABASE_NAME = "wordpress_stats";
    
    private SQLiteDatabase mDB;

    private Context mContext;

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(StatsClicksTable.getInstance().toCreateString());
        db.execSQL(StatsGeoviewsTable.getInstance().toCreateString());
        db.execSQL(StatsMostCommentedTable.getInstance().toCreateString());
        db.execSQL(StatsReferrersTable.getInstance().toCreateString());
        db.execSQL(StatsSearchEngineTermsTable.getInstance().toCreateString());
        db.execSQL(StatsTagsAndCategoriesTable.getInstance().toCreateString());
        db.execSQL(StatsTopAuthorsTable.getInstance().toCreateString());
        db.execSQL(StatsTopCommentersTable.getInstance().toCreateString());
        db.execSQL(StatsTopPostsAndPagesTable.getInstance().toCreateString());
        db.execSQL(StatsVideosTable.getInstance().toCreateString());
    }


    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        
        onCreate(db);
        db = getWritableDatabase();
        
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
    
    public WordPressStatsDB(Context ctx) {
        super(ctx, DATABASE_NAME, null, DATABASE_VERSION);
        this.mContext = ctx;
    }
    

    public void saveStat(Stat stat) {
        ContentValues values = new ContentValues();
        values.put("blogId", stat.getBlogId());
        values.put("category", stat.getCategory());
        values.put("entryType", stat.getEntryType());
        values.put("entry", stat.getEntry());
        values.put("total", stat.getTotal());
        values.put("timeframe", stat.getTimeframe());
        values.put("imageUrl", stat.getImageUrl());
        values.put("url", stat.getUrl());
    }
    
    @SuppressLint("DefaultLocale")
    public Cursor getStats(String blogId, Stats.Category category, int timeframe) {
//        return db.rawQuery("SELECT * FROM " + STATS_TABLE + " WHERE blogId=? AND category=? AND timeframe=? ORDER BY total DESC, entry ASC", 
//                new String [] { blogId, category.name().toLowerCase(), timeframe + "" });
        return null;
    }
    
    public int getStatsCount(int blogId) {
//        Cursor cursor = db.rawQuery("SELECT * FROM " + STATS_TABLE + " WHERE blogId=?", new String[] { blogId + "" });
//        return cursor.getCount();
        return 0;
    }
    
    public void loadSampleStats() {
        if (WordPress.getCurrentBlog() == null)
            return;
        
        String blogId = WordPress.getCurrentBlog().getBlogId() + "";
        int[] timeframes = new int[] {0, 1};
        Random random = new Random();
        
        String[] titles = new String[]{"My awesome video", "My awesome clip", "My awesome website", "My awesome trip", "My awesome article", "My awesome experience", 
                "My awesome cat gif", "My awesome weekend", "My awesome adventure", "My awesome holiday", "My awesome idea" };
        String[] countries = new String[] {"Canada", "United States", "Mexico", "Austrailia", "New Zealand", "France", "Germany", "Sweden", "Iceland", "Japan"};
        String[] tags = new String[] {"Uncategorized", "android", "ios", "web", "java", "c++", "windows", "blackberry", "ruby", "reader" };
        String[] referrers = new String[] {"WordPress.com", "Google.com", "Yahoo.com", "Bing.com", "WordPress.org", "Ask.com", "Stackoverflow.com", "Gmail.com", "Github.com", "Xtremelabs.com"};
        String[] authors = new String[] {"Anne", "Bob", "Cathy", "David", "Erin", "Fiona", "Greg", "Hanna", "Ivan", "Joanne" };
        
        Stats.Category[] categories = new Stats.Category[] { 
                Stats.Category.COUNTRY, Stats.Category.POSTS_AND_PAGES, Stats.Category.CLICKS, Stats.Category.TAGS_AND_CATEGORIES, Stats.Category.AUTHORS,
                Stats.Category.REFERRERS, Stats.Category.VIDEO_PLAYS, Stats.Category.SEARCH_ENGINE_TERMS, Stats.Category.TOP_COMMENTER, Stats.Category.MOST_COMMENTED
        };
        
        for (Stats.Category category : categories) {
            for (int i = 0; i < 10; i++) {
                ContentValues values = new ContentValues();
                values.put("blogId", blogId);
                values.put("category", category.name().toLowerCase());

                if (category == Category.TAGS_AND_CATEGORIES) {
                    if (random.nextBoolean())
                        values.put("entryType", "tag");
                    else
                        values.put("entryType", "category");
                }
                
                if (category == Stats.Category.COUNTRY)
                    values.put("entry", countries[i]);
                else if (category == Stats.Category.CLICKS || category == Stats.Category.SEARCH_ENGINE_TERMS)
                    values.put("entry", "https://www.google.ca/search?q=test_" + i);
                else if (category == Stats.Category.VIDEO_PLAYS)
                    values.put("entry", tags[i] + " video");
                else if (category == Stats.Category.POSTS_AND_PAGES || category == Stats.Category.MOST_COMMENTED)
                    values.put("entry", titles[i]);
                else if (category == Stats.Category.TAGS_AND_CATEGORIES)
                    values.put("entry", tags[i]);
                else if (category == Stats.Category.REFERRERS)
                    values.put("entry", referrers[i]);
                else if (category == Stats.Category.AUTHORS || category == Stats.Category.TOP_COMMENTER)
                    values.put("entry", authors[i]);
                
                values.put("total", random.nextInt(999));
                
                if (category == Stats.Category.TAGS_AND_CATEGORIES)
                    values.put("timeframe", 7);
                else
                    values.put("timeframe", timeframes[random.nextInt(timeframes.length)]);
                
                if(category == Stats.Category.COUNTRY || category == Stats.Category.TOP_COMMENTER || category == Stats.Category.CLICKS || category == Stats.Category.REFERRERS || category == Stats.Category.AUTHORS)
                    values.put("imageUrl", "http://placekitten.com/50/50");

                // never any url
                if (category == Stats.Category.AUTHORS || category == Stats.Category.COUNTRY || category == Stats.Category.SEARCH_ENGINE_TERMS)
                    values.putNull("url");
                // always a url
                else if (category == Stats.Category.POSTS_AND_PAGES || category == Stats.Category.CLICKS || category == Stats.Category.VIDEO_PLAYS || 
                        category == Stats.Category.TOP_COMMENTER || category == Stats.Category.MOST_COMMENTED || category == Stats.Category.TAGS_AND_CATEGORIES)
                    values.put("url", "http://www.google.com");
                // sometimes a url
                else if(category == Stats.Category.REFERRERS && random.nextBoolean())
                    values.put("url", "http://www.google.com");
                
//                db.insert(STATS_TABLE, null, values);
            }
        }
    }

}
