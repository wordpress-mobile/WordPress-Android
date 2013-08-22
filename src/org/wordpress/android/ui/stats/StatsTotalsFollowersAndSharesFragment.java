package org.wordpress.android.ui.stats;

import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.util.StatUtils;

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
            followersHeader.setText(Html.fromHtml(getString(R.string.stats_totals_followers_shares_header_followers_publicize)));
            followersHeader.setMovementMethod(LinkMovementMethod.getInstance());
        }
        
        return view;
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_totals_followers_and_shares);
    }

    @Override
    public void onResume() {
        super.onResume();
        refreshStatsFromServer();
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


    private void refreshStatsFromServer() {
        if (WordPress.getCurrentBlog() == null)
            return; 

        final String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        WordPress.restClient.getStatsTotalsFollowersAndShares(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        StatUtils.saveSummary(blogId, response);
                        if (getActivity() != null)
                            getActivity().runOnUiThread(new Runnable() {
                                
                                @Override
                                public void run() {
                                    refresh();
                                    
                                }
                            });
                    }
                }, 
                new ErrorListener() {
                    
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // TODO Auto-generated method stub
                        
                    }
                });
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
