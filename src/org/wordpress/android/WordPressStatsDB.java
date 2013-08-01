package org.wordpress.android;

import java.util.Random;

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
import org.wordpress.android.models.StatsClick;
import org.wordpress.android.models.StatsGeoview;
import org.wordpress.android.models.StatsMostCommented;
import org.wordpress.android.models.StatsReferrer;
import org.wordpress.android.models.StatsSearchEngineTerm;
import org.wordpress.android.models.StatsTagsandCategories;
import org.wordpress.android.models.StatsTagsandCategories.Type;
import org.wordpress.android.models.StatsTopAuthor;
import org.wordpress.android.models.StatsTopCommenter;
import org.wordpress.android.models.StatsTopPostsAndPages;
import org.wordpress.android.models.StatsVideo;
import org.wordpress.android.providers.StatsContentProvider;

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

    public boolean hasSampleData() {
        Cursor cursor = mContext.getContentResolver().query(StatsContentProvider.STATS_CLICKS_URI, null, null, null, null);
        return (cursor != null && cursor.getCount() > 0);
    }
    
    public void generateSampleData() {
        if (hasSampleData())
            return; 
        
        String blogId = WordPress.getCurrentBlog().getBlogId() + "";
        long date = System.currentTimeMillis();
        Random random = new Random();
        ContentValues values;
        
        String imageUrl = "http://placekitten.com/50/50";
        
        values = StatsClicksTable.getContentValues(new StatsClick(blogId, date, random.nextInt(900), "http://www.google.com", null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_CLICKS_URI, values);
        values = StatsClicksTable.getContentValues(new StatsClick(blogId, date, random.nextInt(900), "http://www.wordpress.com", imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_CLICKS_URI, values);
        values = StatsClicksTable.getContentValues(new StatsClick(blogId, date, random.nextInt(900), "http://www.github.com", null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_CLICKS_URI, values);
        
        values = StatsGeoviewsTable.getContentValues(new StatsGeoview(blogId, date, "Canada", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_GEOVIEWS_URI, values);
        values = StatsGeoviewsTable.getContentValues(new StatsGeoview(blogId, date, "United States", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_GEOVIEWS_URI, values);
        values = StatsGeoviewsTable.getContentValues(new StatsGeoview(blogId, date, "Mexico", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_GEOVIEWS_URI, values);
        
        values = StatsMostCommentedTable.getContentValues(new StatsMostCommented(blogId, random.nextInt(1000000), "Post A", random.nextInt(900), null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_MOST_COMMENTED_URI, values);
        values = StatsMostCommentedTable.getContentValues(new StatsMostCommented(blogId, random.nextInt(1000000), "Post B", random.nextInt(900), null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_MOST_COMMENTED_URI, values);
        values = StatsMostCommentedTable.getContentValues(new StatsMostCommented(blogId, random.nextInt(1000000), "Post C", random.nextInt(900), null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_MOST_COMMENTED_URI, values);
        
        values = StatsReferrersTable.getContentValues(new StatsReferrer(blogId, date, "Referrer A", random.nextInt(900), "http://www.google.com", imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_REFERRERS_URI, values);
        values = StatsReferrersTable.getContentValues(new StatsReferrer(blogId, date, "Referrer B", random.nextInt(900), "http://www.wordpress.com", null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_REFERRERS_URI, values);
        values = StatsReferrersTable.getContentValues(new StatsReferrer(blogId, date, "Referrer C", random.nextInt(900), "http://www.github.com", imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_REFERRERS_URI, values);
        
        values = StatsSearchEngineTermsTable.getContentValues(new StatsSearchEngineTerm(blogId, date, "Search A", random.nextInt(900)));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);
        values = StatsSearchEngineTermsTable.getContentValues(new StatsSearchEngineTerm(blogId, date, "Search B", random.nextInt(900)));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);
        values = StatsSearchEngineTermsTable.getContentValues(new StatsSearchEngineTerm(blogId, date, "Search C", random.nextInt(900)));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);
        
        values = StatsTagsAndCategoriesTable.getContentValues(new StatsTagsandCategories(blogId, date, "Topic A", Type.CATEGORY, random.nextInt(900)));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI, values);
        values = StatsTagsAndCategoriesTable.getContentValues(new StatsTagsandCategories(blogId, date, "Topic B", Type.STAT, random.nextInt(900)));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI, values);
        values = StatsTagsAndCategoriesTable.getContentValues(new StatsTagsandCategories(blogId, date, "Topic C", Type.CATEGORY, random.nextInt(900)));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI, values);
        values = StatsTagsAndCategoriesTable.getContentValues(new StatsTagsandCategories(blogId, date, "Topic D", Type.STAT, random.nextInt(900)));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TAGS_AND_CATEGORIES_URI, values);
        
        values = StatsTopAuthorsTable.getContentValues(new StatsTopAuthor(blogId, date, random.nextInt(1000000), "User A", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_AUTHORS_URI, values);
        values = StatsTopAuthorsTable.getContentValues(new StatsTopAuthor(blogId, date, random.nextInt(1000000), "User B", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_AUTHORS_URI, values);
        values = StatsTopAuthorsTable.getContentValues(new StatsTopAuthor(blogId, date, random.nextInt(1000000), "User C", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_AUTHORS_URI, values);
        values = StatsTopAuthorsTable.getContentValues(new StatsTopAuthor(blogId, date, random.nextInt(1000000), "User D", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_AUTHORS_URI, values);
        
        values = StatsTopCommentersTable.getContentValues(new StatsTopCommenter(blogId, random.nextInt(1000000), "Person A", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_COMMENTERS_URI, values);
        values = StatsTopCommentersTable.getContentValues(new StatsTopCommenter(blogId, random.nextInt(1000000), "Person B", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_COMMENTERS_URI, values);
        values = StatsTopCommentersTable.getContentValues(new StatsTopCommenter(blogId, random.nextInt(1000000), "Person C", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_COMMENTERS_URI, values);
        values = StatsTopCommentersTable.getContentValues(new StatsTopCommenter(blogId, random.nextInt(1000000), "Person D", random.nextInt(900), imageUrl));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_COMMENTERS_URI, values);
        
        values = StatsTopPostsAndPagesTable.getContentValues(new StatsTopPostsAndPages(blogId, date, random.nextInt(1000000), "Post A", random.nextInt(900), null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI, values);
        values = StatsTopPostsAndPagesTable.getContentValues(new StatsTopPostsAndPages(blogId, date, random.nextInt(1000000), "Post B", random.nextInt(900), null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI, values);
        values = StatsTopPostsAndPagesTable.getContentValues(new StatsTopPostsAndPages(blogId, date, random.nextInt(1000000), "Post C", random.nextInt(900), null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI, values);
        
        values = StatsVideosTable.getContentValues(new StatsVideo(blogId, date, random.nextInt(1000000), "Video A", random.nextInt(900), null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_VIDEOS_URI, values);
        values = StatsVideosTable.getContentValues(new StatsVideo(blogId, date, random.nextInt(1000000), "Video B", random.nextInt(900), null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_VIDEOS_URI, values);
        values = StatsVideosTable.getContentValues(new StatsVideo(blogId, date, random.nextInt(1000000), "Video C", random.nextInt(900), null));
        mContext.getContentResolver().insert(StatsContentProvider.STATS_VIDEOS_URI, values);
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
