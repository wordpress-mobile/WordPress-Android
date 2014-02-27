package org.wordpress.android.ui.stats;

import android.os.Bundle;
import android.os.Handler;
import android.text.Html;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.WPLinkMovementMethod;

/**
 * Fragment for summary stats. Only a single page.
 */
public class StatsTotalsFollowersAndSharesFragment extends StatsAbsViewFragment {

    public static final String TAG = StatsTotalsFollowersAndSharesFragment.class.getSimpleName();
    
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
        
        TextView followersHeader = (TextView) view.findViewById(R.id.stats_totals_followers_shares_header_followers);
        if (followersHeader != null) {
            String headerText = getString(R.string.stats_totals_followers_shares_header_followers) +
                    " <font color=\"#9E9E9E\">(" +
                    String.format(getString(R.string.stats_totals_followers_shares_header_includes_publicize),
                    "</font><font color=\"#21759B\"><a href=\"http://support.wordpress.com/publicize/\">") +
                    "</font><font color=\"#9E9E9E\">)</font>";
            followersHeader.setText(Html.fromHtml(headerText));
            followersHeader.setMovementMethod(WPLinkMovementMethod.getInstance());
        }
        
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        refreshSummary();
    }

    private void refreshSummary() {
        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                if (WordPress.getCurrentBlog() == null)
                    return;

                String blogId = WordPress.getCurrentBlog().getDotComBlogId();
                if (TextUtils.isEmpty(blogId))
                    blogId = "0";

                StatsSummary stats = StatUtils.getSummary(blogId);
                int posts = (stats != null ? stats.getPosts() : 0);
                int categories = (stats != null ? stats.getCategories() : 0);
                int tags = (stats != null ? stats.getTags() : 0);
                int followers = (stats != null ? stats.getFollowersBlog() : 0);
                int comments = (stats != null ? stats.getFollowersComments() : 0);
                int shares = (stats != null ? stats.getShares() : 0);

                final String fmtPosts = FormatUtils.formatDecimal(posts);
                final String fmtCategories = FormatUtils.formatDecimal(categories);
                final String fmtTags = FormatUtils.formatDecimal(tags);
                final String fmtFollowers = FormatUtils.formatDecimal(followers);
                final String fmtComments = FormatUtils.formatDecimal(comments);
                final String fmtShares = FormatUtils.formatDecimal(shares);

                handler.post(new Runnable() {
                    public void run() {
                        if (getActivity() == null)
                            return;
                        mPostsCountView.setText(fmtPosts);
                        mCategoriesCountView.setText(fmtCategories);
                        mTagsCountView.setText(fmtTags);
                        mFollowersCountView.setText(fmtFollowers);
                        mCommentsCountView.setText(fmtComments);
                        mSharesCountView.setText(fmtShares);
                    }
                });
            }
        }.start();
    }
    
    @Override
    public String getTitle() {
        return getString(R.string.stats_view_totals_followers_and_shares);
    }
}
