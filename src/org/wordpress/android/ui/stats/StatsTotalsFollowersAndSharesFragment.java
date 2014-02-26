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
        if (WordPress.getCurrentBlog() == null)
            return;

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                String blogId = WordPress.getCurrentBlog().getDotComBlogId();
                if (TextUtils.isEmpty(blogId))
                    blogId = "0";
                final StatsSummary summary = StatUtils.getSummary(blogId);
                handler.post(new Runnable() {
                    public void run() {
                        if (getActivity() != null)
                            refreshViews(summary);
                    }
                });
            }
        }.start();
    }
    
    @Override
    public String getTitle() {
        return getString(R.string.stats_view_totals_followers_and_shares);
    }

    void refreshViews(StatsSummary stats) {
        int posts = 0;
        int categories = 0;
        int tags = 0;
        int followers = 0;
        int comments = 0;
        int shares = 0;
        
        if (stats != null) {
            posts = stats.getPosts();
            categories = stats.getCategories();
            tags = stats.getTags();
            followers = stats.getFollowersBlog();
            comments = stats.getFollowersComments();
            shares = stats.getShares();
        }

         mPostsCountView.setText(FormatUtils.formatDecimal(posts));
         mCategoriesCountView.setText(FormatUtils.formatDecimal(categories));
         mTagsCountView.setText(FormatUtils.formatDecimal(tags));
         mFollowersCountView.setText(FormatUtils.formatDecimal(followers));
         mCommentsCountView.setText(FormatUtils.formatDecimal(comments));
         mSharesCountView.setText(FormatUtils.formatDecimal(shares));
    }

}
