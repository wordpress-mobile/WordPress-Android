package org.wordpress.android.ui.stats;

import android.os.AsyncTask;
import android.os.Bundle;
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
import org.wordpress.android.models.StatsVisitorsAndViewsSummary;
import org.wordpress.android.util.StatUtils;

public class StatsVisitorsAndViewsFragment extends StatsAbsViewFragment {
    
    private TextView mVisitorsToday;
    private TextView mViewsToday;
    private TextView mVisitorsBestEver;
    private TextView mViewsAllTime;
    private TextView mCommentsAllTime;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        
        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);
        
        mVisitorsToday = (TextView) view.findViewById(R.id.stats_visitors_and_views_today_visitors_count);
        mViewsToday = (TextView) view.findViewById(R.id.stats_visitors_and_views_today_views_count);
        mVisitorsBestEver = (TextView) view.findViewById(R.id.stats_visitors_and_views_best_ever_visitor_count);
        mViewsAllTime = (TextView) view.findViewById(R.id.stats_visitors_and_views_all_time_view_count);
        mCommentsAllTime = (TextView) view.findViewById(R.id.stats_visitors_and_views_all_time_comment_count);
        
        return view;
    }

    @Override
    public String getTitle() {
        return getString(R.string.stats_view_visitors_and_views);
    }

    @Override
    public void refresh() {
        if (WordPress.getCurrentBlog() == null)
            return; 
        
        final String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        
        new AsyncTask<String, Void, StatsVisitorsAndViewsSummary>() {

            @Override
            protected StatsVisitorsAndViewsSummary doInBackground(String... params) {
                final String blogId = params[0];
                
                StatsVisitorsAndViewsSummary stats = StatUtils.getVisitorsAndViewsSummary(blogId);
                if (stats == null || StatUtils.isDayOld(stats.getDate())) {
                    refreshStatsFromServer(blogId);
                }
                
                return stats;
            }
            
            protected void onPostExecute(StatsVisitorsAndViewsSummary result) {
                refreshViews(result);
            };
        }.execute(blogId);
    }


    private void refreshStatsFromServer(final String blogId) {
        WordPress.restClient.getStatsVisitorsAndViewsSummary(blogId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject response) {
                        StatUtils.saveVisitorsAndViewsSummary(blogId, response);
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
    
    protected void refreshViews(StatsVisitorsAndViewsSummary result) {
        int visitorsToday = 0;
        int viewsToday = 0;
        int visitorsBestEver = 0;
        int viewsAllTime = 0;
        int commentsAllTime = 0;
        
        if (result != null) {
            visitorsToday = result.getVisitorsToday();
            viewsToday = result.getViewsToday();
            visitorsBestEver = result.getVisitorsBestEver();
            viewsAllTime = result.getViewsAllTime();
            commentsAllTime = result.getCommentsAllTime();
        }

        mVisitorsToday.setText(visitorsToday + "");
        mViewsToday.setText(viewsToday + "");
        mVisitorsBestEver.setText(visitorsBestEver + "");
        mViewsAllTime.setText(viewsAllTime + "");
        mCommentsAllTime.setText(commentsAllTime + "");
    }


}
