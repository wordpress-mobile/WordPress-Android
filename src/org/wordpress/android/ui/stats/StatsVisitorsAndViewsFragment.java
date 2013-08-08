package org.wordpress.android.ui.stats;

import java.util.Random;

import android.graphics.Color;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.StatsVisitorsAndViewsSummary;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.Utils;

public class StatsVisitorsAndViewsFragment extends StatsAbsListViewFragment {

    private static final String[] TITLES = new String [] { StatsTimeframe.DAYS.getLabel(), StatsTimeframe.WEEKS.getLabel(), StatsTimeframe.MONTHS.getLabel() };

    @Override
    public FragmentStatePagerAdapter getAdapter() {
        return new CustomPagerAdapter(getChildFragmentManager());
    }

    @Override
    public String[] getTabTitles() {
        return TITLES;
    }

    @Override
    protected Fragment getFragment(int position) {
        return new InnerFragment();
    }
    
    @Override
    public void refresh(int position) {

    }
    
    private class CustomPagerAdapter extends FragmentStatePagerAdapter {

        public CustomPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            return getFragment(position);
        }

        @Override
        public int getCount() {
            return TITLES.length;
        }
        
        @Override
        public CharSequence getPageTitle(int position) {
            return TITLES[position];
        }
        
    }
    
    @Override
    public String getTitle() {
        return getString(R.string.stats_view_visitors_and_views);
    }

    public static class InnerFragment extends Fragment {

        private LinearLayout mBarGraphLayout;
        private GraphView graphView;
        private GraphViewSeries exampleSeries1;
        private GraphViewSeries exampleSeries2;
        
        private TextView mVisitorsToday;
        private TextView mViewsToday;
        private TextView mVisitorsBestEver;
        private TextView mViewsAllTime;
        private TextView mCommentsAllTime;
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            
            View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);
            
            mBarGraphLayout = (LinearLayout) view.findViewById(R.id.stats_visitors_and_views_bar_graph_layout);
            
            mVisitorsToday = (TextView) view.findViewById(R.id.stats_visitors_and_views_today_visitors_count);
            mViewsToday = (TextView) view.findViewById(R.id.stats_visitors_and_views_today_views_count);
            mVisitorsBestEver = (TextView) view.findViewById(R.id.stats_visitors_and_views_best_ever_visitor_count);
            mViewsAllTime = (TextView) view.findViewById(R.id.stats_visitors_and_views_all_time_view_count);
            mCommentsAllTime = (TextView) view.findViewById(R.id.stats_visitors_and_views_all_time_comment_count);
            
            return view;
        }
        

        @Override
        public void onResume() {
            super.onResume();
            refreshSummary();
            refreshBarGraph();
        }

        private void refreshSummary() {
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
                                        refreshSummary();
                                        
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


        private void refreshBarGraph() {

            Random random = new Random();
            int range = 50;
            
            int numPoints = 30;
            GraphViewData[] data1 = new GraphViewData[numPoints];
            GraphViewData[] data2 = new GraphViewData[numPoints];
            String[] horLabels = new String[numPoints];
            for(int i = 0; i < numPoints; i++) {
                data1[i] = new GraphViewData(i, random.nextInt(range));
                data2[i] = new GraphViewData(i, random.nextInt(range/5));
                horLabels[i] = "" + i;
            }
            
            exampleSeries1 = new GraphViewSeries(data1);
            exampleSeries2 = new GraphViewSeries(data2);

            exampleSeries1.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
            exampleSeries1.getStyle().padding = Utils.dpToPx(1);
            exampleSeries2.getStyle().color = getResources().getColor(R.color.stats_bar_graph_visitors);
            exampleSeries2.getStyle().padding = Utils.dpToPx(3);
            
            graphView = new StatsBarGraph(getActivity(), "");
            graphView.addSeries(exampleSeries1);
            graphView.addSeries(exampleSeries2);

            graphView.getGraphViewStyle().setHorizontalLabelsColor(Color.BLACK);
            graphView.getGraphViewStyle().setVerticalLabelsColor(Color.BLACK);
            graphView.getGraphViewStyle().setNumHorizontalLabels(data1.length);
            graphView.getGraphViewStyle().setTextSize(Utils.spToPx(8));
            graphView.getGraphViewStyle().setGridXColor(Color.TRANSPARENT);
            graphView.getGraphViewStyle().setGridYColor(getResources().getColor(R.color.stats_bar_graph_grid));
            graphView.getGraphViewStyle().setNumVerticalLabels(6);
            graphView.getGraphViewStyle().setNumHorizontalLabels(horLabels.length/3);
            graphView.setHorizontalLabels(horLabels);
            
            mBarGraphLayout.removeAllViews();
            mBarGraphLayout.addView(graphView);   
        }
    }

}
