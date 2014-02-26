package org.wordpress.android.ui.stats;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

/**
 * An enum of the different view types to appear on the stats view.
 * The views of the APIs that have not been implemented should be hidden 
 */

public enum StatsViewType {

    /**
     * A flag is set to indicate which APIs have been implemented.
     * Set the flag to true once the API is implemented for the view to be visible on phone.
     */
    
    VISITORS_AND_VIEWS(R.string.stats_view_visitors_and_views, true),
    VIEWS_BY_COUNTRY(R.string.stats_view_views_by_country, true),
    TOP_POSTS_AND_PAGES(R.string.stats_view_top_posts_and_pages, true),
    TOTALS_FOLLOWERS_AND_SHARES(R.string.stats_view_totals_followers_and_shares, true),
    CLICKS(R.string.stats_view_clicks, true),
    TAGS_AND_CATEGORIES(R.string.stats_view_tags_and_categories, false),
    TOP_AUTHORS(R.string.stats_view_top_authors, false),
    REFERRERS(R.string.stats_view_referrers, true),
    VIDEO_PLAYS(R.string.stats_view_video_plays, false),
    COMMENTS(R.string.stats_view_comments, false),
    SEARCH_ENGINE_TERMS(R.string.stats_view_search_engine_terms, true), ;

    private final int mLabelResId;

    private StatsViewType(int viewsLabelResId, boolean implemented) {
        mLabelResId = viewsLabelResId;
        //mIsImplemented = implemented;
    }

    String getLabel() {
        return WordPress.getContext().getString(mLabelResId);
    }
}
