package org.wordpress.android.ui.stats;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public class Stats {
    

    /**
     * An enum of the views in the stats
     */
    public enum ViewType {

        VISITORS_AND_VIEWS (R.string.stats_view_visitors_and_views),
        VIEWS_BY_COUNTRY (R.string.stats_view_views_by_country),
        TOP_POSTS_AND_PAGES (R.string.stats_view_top_posts_and_pages),
        TOTALS_FOLLOWERS_AND_SHARES (R.string.stats_view_totals_followers_and_shares),
        CLICKS (R.string.stats_view_clicks),
        TAGS_AND_CATEGORIES (R.string.stats_view_tags_and_categories), 
        TOP_AUTHORS (R.string.stats_view_top_authors),
        REFERRERS (R.string.stats_view_referrers),
        VIDEO_PLAYS (R.string.stats_view_video_plays),
        COMMENTS (R.string.stats_view_comments),
        SEARCH_ENGINE_TERMS (R.string.stats_view_search_engine_terms),
        ;

        private int mLabelResId;

        private ViewType(int viewsLabelResId) {
            mLabelResId = viewsLabelResId;
        }
        
        public String getLabel() {
            return WordPress.getContext().getString(mLabelResId);
        }
        
        public static String[] toStringArray() {
            String[] types = new String[ViewType.values().length];
            for (int i = 0; i < ViewType.values().length; i++) {
                types[i] = ViewType.values()[i].getLabel();
            }
            return types;
        }
    }
    
    
    /**
     * An enum of the different stats categories. More than one category can appear in a stats view. 
     */
    public enum Category {

        POSTS_AND_PAGES (
                R.string.stats_category_posts_and_pages, 
                R.string.stats_totals_views,
                false
            ),
        COUNTRY (
                R.string.stats_category_country, 
                R.string.stats_totals_views,
                true
            ),
        REFERRERS (
                R.string.stats_category_referrers, 
                R.string.stats_totals_views,
                true
            ),
        CLICKS (
                R.string.stats_category_clicks_url, 
                R.string.stats_totals_clicks,
                true
            ),
        TAGS_AND_CATEGORIES (
                R.string.stats_category_tags_and_categories, 
                R.string.stats_totals_views,
                false
            ),
        AUTHORS (
                R.string.stats_category_authors, 
                R.string.stats_totals_views,
                true
            ),
        VIDEO_PLAYS (
                R.string.stats_category_video_plays, 
                R.string.stats_totals_views,
                false
            ),
        TOP_COMMENTER (
                R.string.stats_category_top_commentor, 
                R.string.stats_totals_comments,
                true
            ),
        MOST_COMMENTED (
                R.string.stats_category_most_commented, 
                R.string.stats_totals_comments,
                false
            ),
        SEARCH_ENGINE_TERMS (
                R.string.stats_category_search_engine_terms, 
                R.string.stats_totals_views,
                false
            ),
        ;
        
        private int mCategoryLabelResId;
        private int mTotalsLabelResId;
        private boolean mHasImage;

        private Category(int categoryLabelResId, int totalsLabelResId, boolean hasImage) {
            mCategoryLabelResId = categoryLabelResId;
            mTotalsLabelResId = totalsLabelResId;
            mHasImage = hasImage;
        }
        
        public String getCategoryLabel() {
            return WordPress.getContext().getString(mCategoryLabelResId);
        }
        
        public String getTotalsLabel() {
            return WordPress.getContext().getString(mTotalsLabelResId);
        }
        
        public boolean hasImage() {
            return mHasImage;
        }
    }
    
    public enum Timeframe {
        
        TODAY(0, R.string.stats_timeframe_today),
        YESTERDAY(1, R.string.stats_timeframe_yesterday),
        WEEK(7, R.string.stats_timeframe_week),
        MONTH(30, R.string.stats_timeframe_month),
        QUARTER(90, R.string.stats_timeframe_quarter),
        YEAR(365, R.string.stats_timeframe_year),
        ALL_TIME(-1, R.string.stats_timeframe_all_time),
        ;
        
        private int mTimeframe;
        private int mLabelResId;

        private Timeframe(int timeframe, int labelResId) {
            mTimeframe = timeframe;
            mLabelResId = labelResId;
        }
        
        public String getLabel() {
            return WordPress.getContext().getString(mLabelResId); 
        }
        
        public int toInt() {
            return mTimeframe;
        }
        
    }

}
