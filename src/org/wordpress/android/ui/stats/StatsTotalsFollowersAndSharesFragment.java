package org.wordpress.android.ui.stats;

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
import org.wordpress.android.util.MediaUploadService;
import org.wordpress.android.util.StatUtils;

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
                refresh();
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
            followersHeader.setText(Html.fromHtml(getString(R.string.stats_totals_followers_shares_header_followers_publicize)));
            followersHeader.setMovementMethod(LinkMovementMethod.getInstance());
        }
        
        return view;
    }
    @Override
    public void onResume() {
        super.onResume();
        
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(MediaUploadService.MEDIA_UPLOAD_INTENT_NOTIFICATION));
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

    @Override
    public void refresh() {
        if (WordPress.getCurrentBlog() == null)
            return; 
        
        final String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        new AsyncTask<String, Void, StatsSummary>() {

            @Override
            protected StatsSummary doInBackground(String... params) {
                final String blogId = params[0];
                
                StatsSummary stats = StatUtils.getSummary(blogId);

                return stats;
            }
            
            protected void onPostExecute(StatsSummary result) {
                refreshViews(result);
            };
        }.execute(blogId);
    }

    protected void refreshViews(StatsSummary result) {
        int posts = 0;
        int categories = 0;
        int tags = 0;
        int followers = 0;
        int comments = 0;
        int shares = 0;
        
        if (result != null) {
            posts = result.getPosts();
            categories = result.getCategories();
            tags = result.getTags();
            followers = result.getFollowersBlog();
            comments = result.getFollowersComments();
            shares = result.getShares();
        }

         mPostsCountView.setText(posts + "");
         mCategoriesCountView.setText(categories + "");
         mTagsCountView.setText(tags + "");
         mFollowersCountView.setText(followers + "");
         mCommentsCountView.setText(comments + "");
         mSharesCountView.setText(shares + "");
    }

}
