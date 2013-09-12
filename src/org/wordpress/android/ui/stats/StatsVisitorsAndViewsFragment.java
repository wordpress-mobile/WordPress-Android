package org.wordpress.android.ui.stats;

import java.text.DecimalFormat;
import java.util.Locale;

import android.content.BroadcastReceiver;
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
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphView.GraphViewData;
import com.jjoe64.graphview.GraphViewSeries;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.datasets.StatsBarChartDataTable;
import org.wordpress.android.models.StatsSummary;
import org.wordpress.android.providers.StatsContentProvider;
import org.wordpress.android.util.StatUtils;
import org.wordpress.android.util.Utils;

/**
 * Fragment for visitors and views stats. Has three pages, for DAY, WEEK and MONTH stats.
 * A summary of the blog's stats are also shown on each page.
 */ 
public class StatsVisitorsAndViewsFragment extends StatsAbsPagedViewFragment {

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
                    StatsSummary summary = (StatsSummary) intent.getSerializableExtra(StatUtils.STATS_SUMMARY_UPDATED_EXTRA);
                    refreshViews(summary);
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
            getActivity().getContentResolver().registerContentObserver(getUri(), true, mContentObserver);
            
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
            getActivity().getContentResolver().unregisterContentObserver(mContentObserver);
            
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
            lbm.unregisterReceiver(mReceiver);
        }
        
        private StatsBarChartUnit getBarChartUnit() {
            int ordinal = getArguments().getInt(ARGS_BAR_CHART_UNIT);
            return StatsBarChartUnit.values()[ordinal];
        }

        protected void refreshViews(StatsSummary stats) {
            int visitorsToday = 0;
            int viewsToday = 0;
            int visitorsBestEver = 0;
            int viewsAllTime = 0;
            int commentsAllTime = 0;
            
            if (stats != null) {
                visitorsToday = stats.getVisitorsToday();
                viewsToday = stats.getViewsToday();
                visitorsBestEver = stats.getViewsBestDayTotal();
                viewsAllTime = stats.getViewsAllTime();
                commentsAllTime = stats.getCommentsAllTime();
            }

            DecimalFormat formatter = (DecimalFormat) DecimalFormat.getInstance(Locale.getDefault());

            mVisitorsToday.setText(formatter.format(visitorsToday));
            mViewsToday.setText(formatter.format(viewsToday));
            mViewsBestEver.setText(formatter.format(visitorsBestEver));
            mViewsAllTime.setText(formatter.format(viewsAllTime));
            mCommentsAllTime.setText(formatter.format(commentsAllTime));
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
