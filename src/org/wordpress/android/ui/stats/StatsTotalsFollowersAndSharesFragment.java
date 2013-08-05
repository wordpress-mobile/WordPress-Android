package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;

public class StatsTotalsFollowersAndSharesFragment extends StatsAbsViewFragment {

    private TextView mPostsCountView;
    private TextView mCategoriesCountView;
    private TextView mTagsCountView;
    private TextView mFollowersCountView;
    private TextView mCommentsCountView;
    private TextView mSharesCountView;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_totals_followers_shares, container, false);
        
        mPostsCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_posts_count);
        mCategoriesCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_categories_count);
        mTagsCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_tags_count);
        mFollowersCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_followers_count);
        mCommentsCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_comments_count);
        mSharesCountView = (TextView) view.findViewById(R.id.stats_totals_followers_shares_shares_count);
        
        return view;
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_totals_followers_and_shares);
    }

    @Override
    public void refresh() {
        // TODO Auto-generated method stub
        
    }
}
