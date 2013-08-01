
package org.wordpress.android.ui.stats;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;

public enum StatsViewType {

    VISITORS_AND_VIEWS(R.string.stats_view_visitors_and_views),
    VIEWS_BY_COUNTRY(R.string.stats_view_views_by_country),
    TOP_POSTS_AND_PAGES(R.string.stats_view_top_posts_and_pages),
    TOTALS_FOLLOWERS_AND_SHARES(R.string.stats_view_totals_followers_and_shares),
    CLICKS(R.string.stats_view_clicks),
    TAGS_AND_CATEGORIES(R.string.stats_view_tags_and_categories),
    TOP_AUTHORS(R.string.stats_view_top_authors),
    REFERRERS(R.string.stats_view_referrers),
    VIDEO_PLAYS(R.string.stats_view_video_plays),
    COMMENTS(R.string.stats_view_comments),
    SEARCH_ENGINE_TERMS(R.string.stats_view_search_engine_terms), ;

    private int mLabelResId;

    private StatsViewType(int viewsLabelResId) {
        mLabelResId = viewsLabelResId;
    }

    public String getLabel() {
        return WordPress.getContext().getString(mLabelResId);
    }

    public static String[] toStringArray() {
        String[] types = new String[StatsViewType.values().length];
        for (int i = 0; i < StatsViewType.values().length; i++) {
            types[i] = StatsViewType.values()[i].getLabel();
        }
        return types;
    }
}
