package org.wordpress.android.ui.stats;

import android.content.ContentValues;
import android.content.Context;
import android.database.ContentObserver;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsBarChartDaysTable;
import org.wordpress.android.datasets.StatsBarChartMonthsTable;
import org.wordpress.android.datasets.StatsBarChartWeeksTable;
import org.wordpress.android.models.StatsBarChartDay;
import org.wordpress.android.models.StatsBarChartMonth;
import org.wordpress.android.models.StatsBarChartWeek;
import org.wordpress.android.models.StatsVisitorsAndViewsSummary;
import org.wordpress.android.providers.StatsContentProvider;
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
        StatsTimeframe timeframe = StatsTimeframe.DAYS;
        if (position == 1)
            timeframe = StatsTimeframe.WEEKS;
        else if (position == 2) 
            timeframe = StatsTimeframe.MONTHS;
        return InnerFragment.newInstance(timeframe);
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

    public static class InnerFragment extends Fragment implements LoaderManager.LoaderCallbacks<Cursor> {

        private static final String ARGS_TIMEFRAME = "ARGS_TIMEFRAME";
        private LinearLayout mBarGraphLayout;
        private GraphView graphView;
        private GraphViewSeries viewsSeries;
        private GraphViewSeries visitorsSeries;
        
        private TextView mVisitorsToday;
        private TextView mViewsToday;
        private TextView mVisitorsBestEver;
        private TextView mViewsAllTime;
        private TextView mCommentsAllTime;
        private ContentObserver mContentObserver = new MyObserver(new Handler());
        
        public static InnerFragment newInstance(StatsTimeframe timeframe) {
            
            InnerFragment fragment = new InnerFragment();
            
            Bundle args = new Bundle();
            args.putInt(ARGS_TIMEFRAME, timeframe.ordinal());
            fragment.setArguments(args);
            
            return fragment;
        }
        
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
            refreshChartsFromServer();
            getActivity().getContentResolver().registerContentObserver(getUri(), true, mContentObserver);
        }


        @Override
        public void onPause() {
            super.onPause();
            getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
        }
        
        private int getTimeframeOrdinal() {
            return getArguments().getInt(ARGS_TIMEFRAME);
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

        private void refreshChartsFromServer() {
            if (WordPress.getCurrentBlog() == null)
                return;
            
            final String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
            
            Listener listener = new Listener() {
                
                @Override
                public void onResponse(JSONObject response) {
                    new ParseJsonTask().execute(blogId, response, getUri());
                }
            }; 
            
            ErrorListener errorListener = new ErrorListener() {
                
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("WordPress Stats", StatsVisitorsAndViewsFragment.class.getSimpleName() + ": " + error.toString());
                }
            };
            
            int ordinal = getTimeframeOrdinal();
            if (ordinal == StatsTimeframe.DAYS.ordinal())
                WordPress.restClient.getStatsBarChartDays(blogId, listener, errorListener);
            else if (ordinal == StatsTimeframe.WEEKS.ordinal())
                WordPress.restClient.getStatsBarChartWeeks(blogId, listener, errorListener);
            else if (ordinal == StatsTimeframe.MONTHS.ordinal())
                WordPress.restClient.getStatsBarChartMonths(blogId, listener, errorListener);
                    
        }
        
        private class ParseJsonTask extends AsyncTask<Object, Void, Void> {

            @Override
            protected Void doInBackground(Object... params) {
                String blogId = (String) params[0];
                JSONObject response = (JSONObject) params[1];
                Uri uri = (Uri) params[2];
                
                Context context = WordPress.getContext();
                
                if (response != null && response.has("result")) {
                    try {
                        JSONArray results = response.getJSONArray("result");
                        
                        int count = results.length();
                        
                        // delete old stats and insert new ones
                        if (count > 0)
                            context.getContentResolver().delete(uri, "blogId=?", new String[] { blogId });
                        
                        for (int i = 0; i < count; i++ ) {
                            JSONObject result = results.getJSONObject(i);
                            
                            ContentValues values = null;
                            
                            int ordinal = getTimeframeOrdinal();
                            if (ordinal == StatsTimeframe.DAYS.ordinal()) {
                                StatsBarChartDay stat = new StatsBarChartDay(blogId, result);
                                values = StatsBarChartDaysTable.getContentValues(stat);
                            } else if (ordinal == StatsTimeframe.WEEKS.ordinal()) {
                                StatsBarChartWeek stat = new StatsBarChartWeek(blogId, result);
                                values = StatsBarChartWeeksTable.getContentValues(stat);
                            } else if (ordinal == StatsTimeframe.MONTHS.ordinal()) {
                                StatsBarChartMonth stat = new StatsBarChartMonth(blogId, result);
                                values = StatsBarChartMonthsTable.getContentValues(stat);
                            }
                            
                            if (values != null && uri != null) {
                                context.getContentResolver().insert(uri, values);
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    
                }
                return null;
            }        
        }

        
        
        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            getLoaderManager().restartLoader(getTimeframeOrdinal(), null, this);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
            return new CursorLoader(getActivity(), getUri(), null, "blogId=?", new String[] { blogId }, null);
        }

        private Uri getUri() {
            int ordinal = getTimeframeOrdinal();
            Uri uri = null;
            
            if (ordinal == StatsTimeframe.DAYS.ordinal()) 
                uri = StatsContentProvider.STATS_BAR_CHART_DAYS_URI;
            else if (ordinal == StatsTimeframe.WEEKS.ordinal())
                uri = StatsContentProvider.STATS_BAR_CHART_WEEKS_URI;
            else if (ordinal == StatsTimeframe.MONTHS.ordinal())
                uri = StatsContentProvider.STATS_BAR_CHART_MONTHS_URI;
            return uri;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

            if (!cursor.moveToFirst())
                return;
            
            int numPoints = Math.min(30, cursor.getCount());
            
            GraphViewData[] views = new GraphViewData[numPoints];
            GraphViewData[] visitors = new GraphViewData[numPoints];
            String[] horLabels = new String[numPoints];
            
            for(int i = numPoints - 1; i >= 0; i--) {
                views[i] = new GraphViewData(i, getViews(cursor));
                visitors[i] = new GraphViewData(i, getVisitors(cursor));
                horLabels[i] = getDate(cursor);
                cursor.moveToNext();
            }
            
            viewsSeries = new GraphViewSeries(views);
            visitorsSeries = new GraphViewSeries(visitors);

            viewsSeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
            viewsSeries.getStyle().padding = Utils.dpToPx(1);
            visitorsSeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_visitors);
            visitorsSeries.getStyle().padding = Utils.dpToPx(3);
            
            graphView = new StatsBarGraph(getActivity(), "");
            graphView.addSeries(viewsSeries);
            graphView.addSeries(visitorsSeries);

            graphView.getGraphViewStyle().setNumHorizontalLabels(horLabels.length / 5);
            graphView.setHorizontalLabels(horLabels);
            
            mBarGraphLayout.removeAllViews();
            mBarGraphLayout.addView(graphView);   
        }

        private int getViews(Cursor cursor) {
            int timeframe = getTimeframeOrdinal();
            int views = 0;
            
            if (timeframe == StatsTimeframe.DAYS.ordinal()) {
                views = cursor.getInt(cursor.getColumnIndex(StatsBarChartDaysTable.Columns.VIEWS));
            } else if (timeframe == StatsTimeframe.WEEKS.ordinal()) {
                views = cursor.getInt(cursor.getColumnIndex(StatsBarChartWeeksTable.Columns.VIEWS));
            } else if (timeframe == StatsTimeframe.MONTHS.ordinal()) {
                views = cursor.getInt(cursor.getColumnIndex(StatsBarChartMonthsTable.Columns.VIEWS));
            }
            
            return views;
        }

        private int getVisitors(Cursor cursor) {
            int timeframe = getTimeframeOrdinal();
            int visitors = 0;
            
            if (timeframe == StatsTimeframe.DAYS.ordinal()) {
                visitors = cursor.getInt(cursor.getColumnIndex(StatsBarChartDaysTable.Columns.VISITORS));
            } else if (timeframe == StatsTimeframe.WEEKS.ordinal()) {
                visitors = cursor.getInt(cursor.getColumnIndex(StatsBarChartWeeksTable.Columns.VISITORS));
            } else if (timeframe == StatsTimeframe.MONTHS.ordinal()) {
                visitors = cursor.getInt(cursor.getColumnIndex(StatsBarChartMonthsTable.Columns.VISITORS));
            }
            
            return visitors;
        }

        private String getDate(Cursor cursor) {
            int timeframe = getTimeframeOrdinal();
            
            String date = "";
            
            if (timeframe == StatsTimeframe.DAYS.ordinal()) {
                String temp = cursor.getString(cursor.getColumnIndex(StatsBarChartDaysTable.Columns.DATE));
                date = StatUtils.parseDate(temp, "yyyy-MM-dd", "MMM d");
            } else if (timeframe == StatsTimeframe.WEEKS.ordinal()) {
                String temp = cursor.getString(cursor.getColumnIndex(StatsBarChartWeeksTable.Columns.DATE));
                date = StatUtils.parseDate(temp, "yyyy-'W'ww", "'Week' w");
            } else if (timeframe == StatsTimeframe.MONTHS.ordinal()) {
                String temp = cursor.getString(cursor.getColumnIndex(StatsBarChartMonthsTable.Columns.DATE));
                date = StatUtils.parseDate(temp, "yyyy-MM", "MMM yyyy");
            }
            
            return date;
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursor) {
            mBarGraphLayout.removeAllViews();
        }
        
        class MyObserver extends ContentObserver {      
           public MyObserver(Handler handler) {
              super(handler);           
           }

           @Override
           public void onChange(boolean selfChange) {
               if (isAdded())
                   getLoaderManager().restartLoader(0, null, InnerFragment.this);           
           }        
        }
    }

}
