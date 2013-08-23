package org.wordpress.android.ui.stats;

import android.content.BroadcastReceiver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
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
import android.support.v4.content.LocalBroadcastManager;
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
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.models.StatsBarChartData;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.MediaUploadService;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.Utils;

public class StatsVisitorsAndViewsFragment extends StatsAbsListViewFragment {

    private static final String[] TITLES = new String [] { StatsBarChartUnit.DAY.getLabel(), StatsBarChartUnit.WEEK.getLabel(), StatsBarChartUnit.MONTH.getLabel() };

    public static final String TAG = StatsVisitorsAndViewsFragment.class.getSimpleName();
    
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
        StatsBarChartUnit unit = StatsBarChartUnit.DAY;
        if (position == 1)
            unit = StatsBarChartUnit.WEEK;
        else if (position == 2) 
            unit = StatsBarChartUnit.MONTH;
        return InnerFragment.newInstance(unit);
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

        private static final String ARGS_BAR_CHART_UNIT = "ARGS_TIMEFRAME";
        private LinearLayout mBarGraphLayout;
        
        private TextView mVisitorsToday;
        private TextView mViewsToday;
        private TextView mViewsBestEver;
        private TextView mViewsAllTime;
        private TextView mCommentsAllTime;
        private View mLegend;
        private ContentObserver mContentObserver = new MyObserver(new Handler());
        
        private BroadcastReceiver mReceiver = new BroadcastReceiver() {
            
            @Override
            public void onReceive(Context context, Intent intent) {
                String action = intent.getAction();
                if (action.equals(StatUtils.STATS_SUMMARY_UPDATED)) {
                    refreshSummary();
                }
            }
        };
        
        public static InnerFragment newInstance(StatsBarChartUnit unit) {
            
            InnerFragment fragment = new InnerFragment();
            
            Bundle args = new Bundle();
            args.putInt(ARGS_BAR_CHART_UNIT, unit.ordinal());
            fragment.setArguments(args);
            
            return fragment;
        }
        
        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
            
            View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);
            
            mBarGraphLayout = (LinearLayout) view.findViewById(R.id.stats_visitors_and_views_bar_graph_layout);
            
            mVisitorsToday = (TextView) view.findViewById(R.id.stats_visitors_and_views_today_visitors_count);
            mViewsToday = (TextView) view.findViewById(R.id.stats_visitors_and_views_today_views_count);
            mViewsBestEver = (TextView) view.findViewById(R.id.stats_visitors_and_views_best_ever_views_count);
            mViewsAllTime = (TextView) view.findViewById(R.id.stats_visitors_and_views_all_time_view_count);
            mCommentsAllTime = (TextView) view.findViewById(R.id.stats_visitors_and_views_all_time_comment_count);
            mLegend = view.findViewById(R.id.stats_bar_graph_legend);
            
            return view;
        }
        

        @Override
        public void onResume() {
            super.onResume();
            refreshSummary();
            refreshChartsFromServer();
            getActivity().getContentResolver().registerContentObserver(getUri(), true, mContentObserver);
            
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
            lbm.registerReceiver(mReceiver, new IntentFilter(MediaUploadService.MEDIA_UPLOAD_INTENT_NOTIFICATION));
        }


        @Override
        public void onPause() {
            super.onPause();
            getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
            
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
            lbm.unregisterReceiver(mReceiver);
        }
        
        private StatsBarChartUnit getBarChartUnit() {
            int ordinal = getArguments().getInt(ARGS_BAR_CHART_UNIT);
            return StatsBarChartUnit.values()[ordinal];
        }

        private void refreshSummary() {
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
            }.execute(blogId);
        }

        protected void refreshViews(StatsSummary result) {
            int visitorsToday = 0;
            int viewsToday = 0;
            int visitorsBestEver = 0;
            int viewsAllTime = 0;
            int commentsAllTime = 0;
            
            if (result != null) {
                visitorsToday = result.getVisitorsToday();
                viewsToday = result.getViewsToday();
                visitorsBestEver = result.getViewsBestDayTotal();
                viewsAllTime = result.getViewsAllTime();
                commentsAllTime = result.getCommentsAllTime();
            }

            mVisitorsToday.setText(visitorsToday + "");
            mViewsToday.setText(viewsToday + "");
            mViewsBestEver.setText(visitorsBestEver + "");
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
            
            WordPress.restClient.getStatsBarChartData(blogId, getBarChartUnit(), getNumOfPoints(), listener, errorListener);
        }
        
        private class ParseJsonTask extends AsyncTask<Object, Void, Void> {

            @Override
            protected Void doInBackground(Object... params) {
                String blogId = (String) params[0];
                JSONObject response = (JSONObject) params[1];
                Uri uri = (Uri) params[2];
                
                Context context = WordPress.getContext();
                
                if (response != null && response.has("data")) {
                    try {
                        JSONArray results = response.getJSONArray("data");
                        
                        int count = results.length();

                        StatsBarChartUnit unit = getBarChartUnit();
                                                
                        // delete old stats and insert new ones
                        if (count > 0)
                            context.getContentResolver().delete(uri, "blogId=? AND unit=?", new String[] { blogId, unit.name() });

                        for (int i = 0; i < count; i++ ) {
                            JSONArray result = results.getJSONArray(i);
                            StatsBarChartData stat = new StatsBarChartData(blogId, unit, result);
                            ContentValues values = StatsBarChartDataTable.getContentValues(stat);
                            
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
            getLoaderManager().restartLoader(getBarChartUnit().ordinal(), null, this);
        }

        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
            if (WordPress.getCurrentBlog() == null)
                return null;
            
            String blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
            StatsBarChartUnit unit = getBarChartUnit();
            return new CursorLoader(getActivity(), getUri(), null, "blogId=? AND unit=?", new String[] { blogId, unit.name() }, null);
        }

        private Uri getUri() {
            return StatsContentProvider.STATS_BAR_CHART_DATA_URI;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {

            if (getActivity() == null)
                return;
            
            if (!cursor.moveToFirst() || cursor.getCount() == 0) {
                Context context = mBarGraphLayout.getContext();
                LayoutInflater inflater = LayoutInflater.from(context);
                mBarGraphLayout.addView(inflater.inflate(R.layout.stats_bar_graph_empty, mBarGraphLayout, false));
                
                return;
            }
            
            int numPoints = Math.min(getNumOfPoints(), cursor.getCount());
            
            GraphViewData[] views = new GraphViewData[numPoints];
            GraphViewData[] visitors = new GraphViewData[numPoints];
            String[] horLabels = new String[numPoints];
            
            for(int i = numPoints - 1; i >= 0; i--) {
                views[i] = new GraphViewData(i, getViews(cursor));
                visitors[i] = new GraphViewData(i, getVisitors(cursor));
                horLabels[i] = getDate(cursor);
                cursor.moveToNext();
            }
            
            GraphViewSeries viewsSeries = new GraphViewSeries(views);
            GraphViewSeries visitorsSeries = new GraphViewSeries(visitors);

            viewsSeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
            viewsSeries.getStyle().padding = Utils.dpToPx(1);
            visitorsSeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_visitors);
            visitorsSeries.getStyle().padding = Utils.dpToPx(3);
            
            GraphView graphView = new StatsBarGraph(getActivity(), "");
            graphView.addSeries(viewsSeries);
            graphView.addSeries(visitorsSeries);

            graphView.getGraphViewStyle().setNumHorizontalLabels(getNumOfHorizontalLabels(numPoints));
            graphView.setHorizontalLabels(horLabels);
            
            mBarGraphLayout.removeAllViews();
            mBarGraphLayout.addView(graphView);   
            mLegend.setVisibility(View.VISIBLE);
        }

        private int getNumOfPoints() {
            if (Utils.isTablet()) {
                return 30; 
            } 
            
            if (getBarChartUnit() == StatsBarChartUnit.DAY) 
                return 7;
            else
                return 12;
        }
        
        private int getNumOfHorizontalLabels(int numPoints) {
            if (Utils.isTablet()) {
                return numPoints / 5;
            }

            if (getBarChartUnit() == StatsBarChartUnit.DAY)
                return numPoints / 2;
            else
                return numPoints / 3;
        }

        private int getViews(Cursor cursor) {
            return cursor.getInt(cursor.getColumnIndex(StatsBarChartDataTable.Columns.VIEWS));
        }

        private int getVisitors(Cursor cursor) {
            return cursor.getInt(cursor.getColumnIndex(StatsBarChartDataTable.Columns.VISITORS));
        }

        private String getDate(Cursor cursor) {
            StatsBarChartUnit unit = getBarChartUnit();
            
            String date = "";

            String temp = cursor.getString(cursor.getColumnIndex(StatsBarChartDataTable.Columns.DATE));
            
            if (unit == StatsBarChartUnit.DAY) {
                date = StatUtils.parseDate(temp, "yyyy-MM-dd", "MMM d");
            } else if (unit == StatsBarChartUnit.WEEK) {
                date = StatUtils.parseDate(temp, "yyyy'W'ww", "'Week' w");
            } else if (unit == StatsBarChartUnit.MONTH) {
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
