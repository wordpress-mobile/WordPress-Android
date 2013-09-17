package org.wordpress.android.ui.stats;

import java.text.DecimalFormat;
import java.util.Locale;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.util.StatUtils;

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
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(StatUtils.STATS_SUMMARY_UPDATED)) {
                StatsSummary stats = (StatsSummary) intent.getSerializableExtra(StatUtils.STATS_SUMMARY_UPDATED_EXTRA);
                refreshViews(stats);
            }
        }
    };
    
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
            followersHeader.setMovementMethod(LinkMovementMethod.getInstance());
        }
        
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(StatUtils.STATS_SUMMARY_UPDATED));

        refreshSummary();
    }

    private void refreshSummary() {
        if (WordPress.getCurrentBlog() == null)
            return;
           
        final String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        new AsyncTask<Void, Void, StatsSummary>() {

            @Override
            protected StatsSummary doInBackground(Void... params) {
                return StatUtils.getSummary(blogId);
            }
            
            protected void onPostExecute(final StatsSummary result) {
                if (getActivity() == null)
                    return;
                getActivity().runOnUiThread(new Runnable() {
                    
                    @Override
                    public void run() {
                        refreshViews(result);      
                    }
                });
            };
            
        }.execute();
        
    }

    @Override
    public void onPause() {
        super.onPause();
        
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);
    }
    
    @Override
    public String getTitle() {
        return getString(R.string.stats_view_totals_followers_and_shares);
    }

    protected void refreshViews(StatsSummary stats) {
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

         DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());
         
         mPostsCountView.setText(formatter.format(posts));
         mCategoriesCountView.setText(formatter.format(categories));
         mTagsCountView.setText(formatter.format(tags));
         mFollowersCountView.setText(formatter.format(followers));
         mCommentsCountView.setText(formatter.format(comments));
         mSharesCountView.setText(formatter.format(shares));
    }

}
