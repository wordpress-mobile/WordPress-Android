package org.wordpress.android.ui.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

import org.json.JSONArray;
import org.json.JSONException;
import org.wordpress.android.R;
import org.wordpress.android.ui.stats.model.SummaryModel;
import org.wordpress.android.ui.stats.model.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.TypefaceCache;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;


public class StatsVisitorsAndViewsFragment extends StatsAbstractFragment implements RadioGroup.OnCheckedChangeListener  {

    public static final String TAG = StatsVisitorsAndViewsFragment.class.getSimpleName();

    private LinearLayout mGraphContainer;
    private StatsBarGraph mGraphView;
    private GraphViewSeries mCurrentSeriesOnScreen;
    private VisitsModel mVisitsData;
    private String[] mStatsDate;

    private RadioGroup mRadioGroup;
    private int mSelectedButtonIndex = 0;
    String[] titles = {"Views", "Visitors", "Likes", "Reblogs", "Comments" };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);
        setRetainInstance(true);

        TextView titleTextView = (TextView) view.findViewById(R.id.stats_pager_title);
        titleTextView.setText(getTitle().toUpperCase(Locale.getDefault()));

        mGraphContainer = (LinearLayout) view.findViewById(R.id.stats_bar_chart_fragment_container);
        setupEmptyGraph();

        mRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);

        int dp8 = DisplayUtils.dpToPx(view.getContext(), 8);
        int dp80 = DisplayUtils.dpToPx(view.getContext(), 80);

        for (int i = 0; i < titles.length; i++) {
            RadioButton rb = (RadioButton) inflater.inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);
            rb.setTypeface((TypefaceCache.getTypeface(view.getContext())));

            params.setMargins(dp8, 0, 0, 0);
            rb.setMinimumWidth(dp80);
            rb.setGravity(Gravity.CENTER);
            rb.setLayoutParams(params);
            rb.setText(titles[i]);
            mRadioGroup.addView(rb);

            if (i == 0) {
                rb.setChecked(true);
            }
        }

        mRadioGroup.setVisibility(View.VISIBLE);
        mRadioGroup.setOnCheckedChangeListener(this);

        return view;
    }

    @Override
    public void onPause() {
        super.onPause();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        super.onResume();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_STATS_UPDATED));
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        // checkedId will be -1 when the selection is cleared
        if (checkedId == -1)
            return;

        int index = group.indexOfChild(group.findViewById(checkedId));
        if (index == -1)
            return;

        mSelectedButtonIndex = index;
        updateGraph(mVisitsData);
    }

    private HashMap<String, Integer> getDataModelColumnIndexes(VisitsModel visitsData) {
        HashMap<String, Integer> mapping = new HashMap<String, Integer>(6);
        final JSONArray fieldsJSON = visitsData.getFieldsJSON();
        if (visitsData == null || fieldsJSON == null) {
            return mapping;
        }
        try {
            for (int i = 0; i < fieldsJSON.length(); i++) {
                final String field = fieldsJSON.getString(i);
                mapping.put(field, new Integer(i));
            }
        } catch (JSONException e) {
            AppLog.e(AppLog.T.STATS, this.getClass().getName() + "JSON error", e);
        }
        return mapping;
    }


    private void updateGraph(VisitsModel visitsData) {
        mVisitsData = visitsData;
        final JSONArray dataJSON = visitsData.getDataJSON();
        if (dataJSON == null || dataJSON.length() == 0) {
            setupEmptyGraph();
            return;
        }

        // keep a reference to the bar to hightlight in the graph.
        int barToHighlight = -1;
        if (mGraphView != null){
            barToHighlight = mGraphView.getHighlightBar();
        }

        String currentField = titles[mSelectedButtonIndex].toLowerCase();
        HashMap<String, Integer> columnsMapping = getDataModelColumnIndexes(visitsData);
        int numPoints = Math.min(getNumOfPoints(), dataJSON.length());
        final String[] horLabels = new String[numPoints];
        mStatsDate = new String[numPoints];
        GraphView.GraphViewData[] views = new GraphView.GraphViewData[numPoints];

        int currentPointIndex = numPoints - 1;
        for (int i = dataJSON.length() -1; i >= 0 && currentPointIndex >= 0; i--) {
            int currentItemValue = 0;
            try {
                JSONArray currentDayData = dataJSON.getJSONArray(i);
                currentItemValue = currentDayData.getInt(columnsMapping.get(currentField).intValue());
                views[currentPointIndex] = new GraphView.GraphViewData(currentPointIndex, currentItemValue);
                String currentItemStatsDate = currentDayData.getString(columnsMapping.get("period").intValue());
                horLabels[currentPointIndex] = getDateLabel(currentItemStatsDate);
                mStatsDate[currentPointIndex] = currentItemStatsDate;
            } catch (JSONException e) {
                AppLog.e(AppLog.T.STATS, this.getClass().getName() + "JSON error", e);
            }
            currentPointIndex--;
        }

        mCurrentSeriesOnScreen = new GraphViewSeries(views);
        mCurrentSeriesOnScreen.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
        mCurrentSeriesOnScreen.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 1);

        if (mGraphContainer.getChildCount() >= 1 && mGraphContainer.getChildAt(0) instanceof GraphView) {
            mGraphView = (StatsBarGraph) mGraphContainer.getChildAt(0);
        } else {
            mGraphContainer.removeAllViews();
            mGraphView = new StatsBarGraph(getActivity());
            mGraphContainer.addView(mGraphView);
        }

        if (mGraphView != null) {
            mGraphView.removeAllSeries();
            mGraphView.addSeries(mCurrentSeriesOnScreen);
            mGraphView.getGraphViewStyle().setNumHorizontalLabels(getNumOfHorizontalLabels(numPoints));
            mGraphView.setHorizontalLabels(horLabels);
        }

        if (barToHighlight != -1) {
            mGraphView.highlightBar(barToHighlight);
        } else {
            mGraphView.highlightBar(numPoints - 1);
        }
    }

    private String getDateLabel(String dateToFormat) {
        switch (getTimeframe()) {
            case DAY:
                return StatsUtils.parseDate(dateToFormat, "yyyy-MM-dd", "MMM d");
            case WEEK:
                // first four digits are the year
                // followed by Wxx where xx is the month
                // followed by Wxx where xx is the day of the month
                // ex: 2013W07W22 = July 22, 2013
                return StatsUtils.parseDate(dateToFormat, "yyyy'W'MM'W'dd", "MMM d");
            case MONTH:
                return StatsUtils.parseDate(dateToFormat, "yyyy-MM", "MMM yyyy");
            case YEAR:
                return StatsUtils.parseDate(dateToFormat, "yyyy-MM-dd", "yyyy");
            default:
                return dateToFormat;
        }
    }

    private void setupEmptyGraph() {
        Context context = mGraphContainer.getContext();
        if (context != null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View emptyBarGraphView = inflater.inflate(R.layout.stats_bar_graph_empty, mGraphContainer, false);
            if (emptyBarGraphView != null) {
                mGraphContainer.removeAllViews();
                mGraphContainer.addView(emptyBarGraphView);
            }
        }
        return;
    }

    private int getNumOfPoints() {
        if (getTimeframe() == StatsTimeframe.DAY) {
            return 7;
        } else {
            return 12;
        }
    }

    private int getNumOfHorizontalLabels(int numPoints) {
        if (getTimeframe() == StatsTimeframe.DAY) {
            return numPoints / 2;
        } else {
            return numPoints / 3;
        }
    }

/*

    private void refreshSummary() {
        if (WordPress.getBlog(getLocalTableBlogID()) == null) {
            return;
        }

        final Handler handler = new Handler();
        new Thread() {
            @Override
            public void run() {
                String blogId = WordPress.getBlog(getLocalTableBlogID()).getDotComBlogId();
                if (TextUtils.isEmpty(blogId)) {
                    blogId = "0";
                }
                final StatsSummary summary = StatsUtils.getSummary(blogId);
                handler.post(new Runnable() {
                    public void run() {
                        refreshSummary(summary);
                    }
                });
            }
        } .start();
    }

    private void refreshSummary(final StatsSummary stats) {
        if (getActivity() == null) {
            return;
        }

        String timezone = StatsUtils.getBlogTimezone(WordPress.getBlog(getLocalTableBlogID()));
        long currentDate = timezone != null ? StatsUtils.getCurrentDateMsTZ(timezone) : StatsUtils.getCurrentDateMs();

        if (stats != null
                && stats.getDay() != null
                && StatsUtils.toMs(stats.getDay()) != currentDate
                ) {
            mVisitorsTodayLabel.setText(StatsUtils.parseDate(stats.getDay(), "yyyy-MM-dd", "MMM d"));
        } else {
            // set the default "Today" label
            mVisitorsTodayLabel.setText(R.string.stats_visitors_and_views_header_today);
        }

        if (stats == null) {
            mVisitorsToday.setText("0");
            mViewsToday.setText("0");
            mViewsBestEver.setText("0");
            mViewsAllTime.setText("0");
            mCommentsAllTime.setText("0");
        } else {
            mVisitorsToday.setText(FormatUtils.formatDecimal(stats.getVisitorsToday()));
            mViewsToday.setText(FormatUtils.formatDecimal(stats.getViewsToday()));
            mViewsBestEver.setText(FormatUtils.formatDecimal(stats.getViewsBestDayTotal()));
            mViewsAllTime.setText(FormatUtils.formatDecimal(stats.getViewsAllTime()));
            mCommentsAllTime.setText(FormatUtils.formatDecimal(stats.getCommentsAllTime()));
        }
    }
*/
    @Override
    protected String getTitle() {
        return getString(R.string.stats_view_visitors_and_views);
    }

    /*
     * receives broadcast when data has been updated
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = StringUtils.notNullStr(intent.getAction());

            if (!action.equals(StatsService.ACTION_STATS_UPDATED) || !intent.hasExtra(StatsService.EXTRA_UPDATED_SECTION_NAME)) {
                return;
            }

            StatsService.StatsSectionEnum sectionToUpdate = (StatsService.StatsSectionEnum) intent.getSerializableExtra(StatsService.EXTRA_UPDATED_SECTION_NAME);
            if (sectionToUpdate != StatsService.StatsSectionEnum.SUMMARY && sectionToUpdate != StatsService.StatsSectionEnum.VISITS) {
                return;
            }

            Serializable dataObj = intent.getSerializableExtra(StatsService.EXTRA_UPDATED_SECTION_DATA);
            if ( dataObj == null || dataObj instanceof VolleyError) {
                //TODO: show the error on the section ???
                return;
            }
            //TODO: check period and blogID
            final String blogId = StatsUtils.getBlogId(getLocalTableBlogID());

            if (sectionToUpdate == StatsService.StatsSectionEnum.SUMMARY) {

            }

            if (sectionToUpdate == StatsService.StatsSectionEnum.VISITS) {
                updateGraph((VisitsModel)dataObj);
            }
            return;
        }
    };
}
