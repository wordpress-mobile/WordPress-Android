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

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.stats.model.VisitModel;
import org.wordpress.android.ui.stats.model.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.TypefaceCache;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;


public class StatsVisitorsAndViewsFragment extends StatsAbstractFragment
        implements RadioGroup.OnCheckedChangeListener, StatsBarGraph.OnGestureListener  {

    public static final String TAG = StatsVisitorsAndViewsFragment.class.getSimpleName();
    private static final String ARG_SELECTED_GRAPH_BAR = "ARG_SELECTED_GRAPH_BAR";
    private static final String ARG_SELECTED_OVERVIEW_ITEM = "ARG_SELECTED_OVERVIEW_ITEM";

    private LinearLayout mGraphContainer;
    private StatsBarGraph mGraphView;
    private GraphViewSeries mCurrentSeriesOnScreen;
    private RadioGroup mRadioGroup;
    private TextView mDateTextView;
    private String[] mStatsDate;

    OverviewLabel[] overviewItems = {OverviewLabel.VIEWS, OverviewLabel.VISITORS, OverviewLabel.LIKES,
            OverviewLabel.REBLOGS, OverviewLabel.COMMENTS};

    // Restore the following variables on restart
    private VisitsModel mVisitsData;
    private int mSelectedOverviewItemIndex = 0;
    private int mSelectedBarGraphBarIndex = -1;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);

        mDateTextView = (TextView) view.findViewById(R.id.stats_summary_date);
        mGraphContainer = (LinearLayout) view.findViewById(R.id.stats_bar_chart_fragment_container);
        mRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);

        int dp8 = DisplayUtils.dpToPx(view.getContext(), 8);
        int dp80 = DisplayUtils.dpToPx(view.getContext(), 80);

        for (int i = 0; i < overviewItems.length; i++) {
            RadioButton rb = (RadioButton) inflater.inflate(R.layout.stats_radio_button, null, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.WRAP_CONTENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);
            rb.setTypeface((TypefaceCache.getTypeface(view.getContext())));

            params.setMargins(0, dp8, 0, 0);
            rb.setMinimumWidth(dp80);
            rb.setGravity(Gravity.CENTER);
            rb.setLayoutParams(params);
            rb.setText(overviewItems[i].getLabel());
            rb.setTag(overviewItems[i]);
            mRadioGroup.addView(rb);

            if (i == mSelectedOverviewItemIndex) {
                rb.setChecked(true);
            }
        }

        mRadioGroup.setVisibility(View.VISIBLE);
        updateUI();
        mRadioGroup.setOnCheckedChangeListener(this);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            AppLog.d(T.STATS, "StatsVisitorsAndViewsFragment > restoring instance state");
            if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
                mVisitsData = (VisitsModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
            }
            if (savedInstanceState.containsKey(ARG_SELECTED_OVERVIEW_ITEM)) {
                mSelectedOverviewItemIndex = savedInstanceState.getInt(ARG_SELECTED_OVERVIEW_ITEM, 0);
            }
            if (savedInstanceState.containsKey(ARG_SELECTED_GRAPH_BAR)) {
                mSelectedBarGraphBarIndex = savedInstanceState.getInt(ARG_SELECTED_GRAPH_BAR, -1);
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        AppLog.d(T.STATS, "StatsVisitorsAndViewsFragment > saving instance state");

        outState.putSerializable(ARG_REST_RESPONSE, mVisitsData);
        outState.putInt(ARG_SELECTED_GRAPH_BAR, mSelectedBarGraphBarIndex);
        outState.putInt(ARG_SELECTED_OVERVIEW_ITEM, mSelectedOverviewItemIndex);

        super.onSaveInstanceState(outState);
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

        mSelectedOverviewItemIndex = index;
        updateUI();
    }

    private VisitModel[] getDataToShowOnGraph(VisitsModel visitsData) {
        VisitModel[] visitModels = visitsData.getVisits();
        int numPoints = Math.min(getNumOfPoints(), visitModels.length);
        int currentPointIndex = numPoints - 1;
        VisitModel[] visitModelsToShow = new VisitModel[numPoints];

        for (int i = visitModels.length -1; i >= 0 && currentPointIndex >= 0; i--) {
            VisitModel currentVisitModel = visitModels[i];
            visitModelsToShow[currentPointIndex] = currentVisitModel;
            currentPointIndex--;
        }
        return visitModelsToShow;
    }

    private void updateUI() {
        if (mVisitsData == null) {
            setupEmptyGraph();
            return;
        }

        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph(mVisitsData);
        if (dataToShowOnGraph == null || dataToShowOnGraph.length == 0) {
            setupEmptyGraph();
            return;
        }

        int barSelectedOnGraph = mSelectedBarGraphBarIndex != -1 ? mSelectedBarGraphBarIndex : dataToShowOnGraph.length - 1;
        final String[] horLabels = new String[dataToShowOnGraph.length];
        mStatsDate = new String[dataToShowOnGraph.length];
        GraphView.GraphViewData[] views = new GraphView.GraphViewData[dataToShowOnGraph.length];

        boolean isEmptyGraph = true;
        OverviewLabel selectedStatsType = overviewItems[mSelectedOverviewItemIndex];

        for (int i = 0; i < dataToShowOnGraph.length; i++) {
            int currentItemValue = 0;
            switch(selectedStatsType) {
                case VIEWS:
                    currentItemValue = dataToShowOnGraph[i].getViews();
                    break;
                case VISITORS:
                    currentItemValue = dataToShowOnGraph[i].getVisitors();
                    break;
                case REBLOGS:
                    currentItemValue = dataToShowOnGraph[i].getReblogs();
                    break;
                case LIKES:
                    currentItemValue = dataToShowOnGraph[i].getLikes();
                    break;
                case COMMENTS:
                    currentItemValue = dataToShowOnGraph[i].getComments();
                    break;
            }
            views[i] = new GraphView.GraphViewData(i, currentItemValue);

            String currentItemStatsDate = dataToShowOnGraph[i].getPeriod();
            horLabels[i] = getDateLabel(currentItemStatsDate);
            mStatsDate[i] = currentItemStatsDate;
            if (currentItemValue > 0) {
                isEmptyGraph = false;
            }
        }

        mCurrentSeriesOnScreen = new GraphViewSeries(views);
        mCurrentSeriesOnScreen.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
        mCurrentSeriesOnScreen.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 1);

        if (isEmptyGraph) {
            setupEmptyGraph();
        } else {
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
               //mGraphView.getGraphViewStyle().setNumHorizontalLabels(getNumOfHorizontalLabels(dataToShowOnGraph.length));
                mGraphView.getGraphViewStyle().setNumHorizontalLabels(dataToShowOnGraph.length);
                mGraphView.setHorizontalLabels(horLabels);
                mGraphView.setGestureListener(this);
                mGraphView.highlightBar(barSelectedOnGraph);
            }
        }

        updateUIBelowTheGraph(barSelectedOnGraph);
    }

    //update the area right below the graph
    private void updateUIBelowTheGraph(int itemPosition) {
        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph(mVisitsData);

        String date =  mStatsDate[itemPosition];
        if (date == null) {
            AppLog.w(AppLog.T.STATS, "Cannot update the area below the graph if a null date is passed!!");
            return;
        }

        switch (getTimeframe()) {
            case DAY:
                mDateTextView.setText(StatsUtils.parseDate(date, "yyyy-MM-dd", "MMMM d"));
                break;
            case WEEK:
                try {
                    SimpleDateFormat sdf;
                    Calendar c;
                    final Date parsedDate;
                    // first four digits are the year
                    // followed by Wxx where xx is the month
                    // followed by Wxx where xx is the day of the month
                    // ex: 2013W07W22 = July 22, 2013
                    sdf = new SimpleDateFormat("yyyy'W'MM'W'dd");
                    //Calculate the end of the week
                    parsedDate = sdf.parse(date);
                    c = Calendar.getInstance();
                    c.setTime(parsedDate);
                    // first day of this week
                    c.setFirstDayOfWeek(Calendar.MONDAY);
                    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY );
                    String startDateLabel = StatsUtils.msToString(c.getTimeInMillis(), "MMMM dd");
                    // last day of this week
                    c.add(Calendar.DAY_OF_WEEK, + 6);
                    String endDateLabel = StatsUtils.msToString(c.getTimeInMillis(), "MMMM dd");
                    mDateTextView.setText(startDateLabel + " - " + endDateLabel);
                } catch (ParseException e) {
                    AppLog.e(AppLog.T.UTILS, e);
                    mDateTextView.setText("");
                }
                break;
            case MONTH:
                mDateTextView.setText(
                        StatsUtils.msToString(StatsUtils.toMs(date), "MMMM")
                );
                break;
            case YEAR:
                mDateTextView.setText(StatsUtils.parseDate(date, "yyyy-MM-dd", "yyyy"));
                break;
        }

        VisitModel modelTapped = dataToShowOnGraph[itemPosition];
        for (int i=0 ; i < mRadioGroup.getChildCount(); i++) {
            View o = mRadioGroup.getChildAt(i);
            if (o instanceof RadioButton) {
                RadioButton currentBtm = (RadioButton)o;
                if (i == mSelectedOverviewItemIndex) {
                   currentBtm.setChecked(true);
                }
                OverviewLabel overviewItem = (OverviewLabel)currentBtm.getTag();
                switch (overviewItem) {
                    case VIEWS:
                        currentBtm.setText(overviewItem.getLabel() + " - " +  modelTapped.getViews());
                        break;
                    case VISITORS:
                        currentBtm.setText(overviewItem.getLabel() + " - " +  modelTapped.getVisitors());
                        break;
                    case REBLOGS:
                        currentBtm.setText(overviewItem.getLabel() + " - " +  modelTapped.getReblogs());
                        break;
                    case LIKES:
                        currentBtm.setText(overviewItem.getLabel() + " - " +  modelTapped.getLikes());
                        break;
                    case COMMENTS:
                        currentBtm.setText(overviewItem.getLabel() + " - " +  modelTapped.getComments());
                        break;
                }
            }
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
        return 7;
        /*
        if (getTimeframe() == StatsTimeframe.DAY) {
            return 7;
        } else {
            return 12;
        }
        */
    }

   /* private int getNumOfHorizontalLabels(int numPoints) {
        if (getTimeframe() == StatsTimeframe.DAY) {
            return numPoints / 2;
        } else {
            return numPoints / 3;
        }
    }*/

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
            if (/*sectionToUpdate != StatsService.StatsSectionEnum.SUMMARY && */ sectionToUpdate != StatsService.StatsSectionEnum.VISITS) {
                return;
            }

            Serializable dataObj = intent.getSerializableExtra(StatsService.EXTRA_UPDATED_SECTION_DATA);
            if ( dataObj == null || dataObj instanceof VolleyError) {
                //TODO: show the error on the section ???
                return;
            }

            // Reset the bar to highlight
            if (mGraphView != null) {
                mGraphView.resetHighlightBar();
            }

            mVisitsData = (VisitsModel)dataObj;
            mSelectedBarGraphBarIndex = -1;
            mSelectedOverviewItemIndex = 0;
            updateUI();
            return;
        }
    };




    @Override
    public void onBarTapped(int tappedBar) {
        AppLog.d(AppLog.T.STATS, " Tapped bar date " + mStatsDate[tappedBar]);
        mSelectedBarGraphBarIndex = tappedBar;
        updateUIBelowTheGraph(tappedBar);

        //TODO: move this code in utility stats. See activity that has similar code.
        if (!NetworkUtils.isNetworkAvailable(getActivity())) {
            return;
        }

        // Update Stats here
        String date =  mStatsDate[tappedBar];
        if (date == null) {
            AppLog.w(AppLog.T.STATS, "A call to update stats is made but a null date is received!!");
            return;
        }

        //Calculate the correct end date for the selected period
        String calculatedDate = null;

        try {
            SimpleDateFormat sdf;
            Calendar c = Calendar.getInstance();
            c.setFirstDayOfWeek(Calendar.MONDAY);
            final Date parsedDate;
            switch (getTimeframe()) {
                case DAY:
                    calculatedDate = date;
                    break;
                case WEEK:
                    // first four digits are the year
                    // followed by Wxx where xx is the month
                    // followed by Wxx where xx is the day of the month
                    // ex: 2013W07W22 = July 22, 2013
                    sdf = new SimpleDateFormat("yyyy'W'MM'W'dd");
                    //Calculate the end of the week
                    parsedDate = sdf.parse(date);
                    c.setTime(parsedDate);
                    // first day of this week
                    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    // last day of this week
                    c.add(Calendar.DAY_OF_WEEK, +6);
                    calculatedDate = StatsUtils.msToString(c.getTimeInMillis(), "yyyy-MM-dd");
                    break;
                case MONTH:
                    sdf = new SimpleDateFormat("yyyy-MM");
                    //Calculate the end of the month
                    parsedDate = sdf.parse(date);
                    c.setTime(parsedDate);
                    // last day of this month
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
                    calculatedDate = StatsUtils.msToString(c.getTimeInMillis(), "yyyy-MM-dd");
                    break;
                case YEAR:
                    sdf = new SimpleDateFormat("yyyy-MM-dd");
                    //Calculate the end of the week
                    parsedDate = sdf.parse(date);
                    c.setTime(parsedDate);
                    c.set(Calendar.MONTH, Calendar.DECEMBER);
                    c.set(Calendar.DAY_OF_MONTH, 31);
                    calculatedDate = StatsUtils.msToString(c.getTimeInMillis(), "yyyy-MM-dd");
                    break;
            }
        } catch (ParseException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        if (calculatedDate == null) {
            AppLog.w(AppLog.T.STATS, "A call to request new stats stats is made but date received cannot be parsed!! " + date);
            return;
        }

        final String blogId = StatsUtils.getBlogId(getLocalTableBlogID());
        // start service to get stats
        Intent intent = new Intent(getActivity(), StatsService.class);
        intent.putExtra(StatsService.ARG_BLOG_ID, blogId);
        intent.putExtra(StatsService.ARG_PERIOD, getTimeframe());
        intent.putExtra(StatsService.ARG_DATE, calculatedDate);
        intent.putExtra(StatsService.ARG_UPDATE_GRAPH, false);
        getActivity().startService(intent);
    }


    private enum OverviewLabel {
        VIEWS(R.string.stats_views),
        VISITORS(R.string.stats_visitors),
        LIKES(R.string.stats_likes),
        REBLOGS(R.string.stats_reblogs),
        COMMENTS(R.string.stats_comments),
        ;

        private final int mLabelResId;

        private OverviewLabel(int labelResId) {
            mLabelResId = labelResId;
        }

        public String getLabel() {
            return WordPress.getContext().getString(mLabelResId);
        }

        public static String[] toStringArray(OverviewLabel[] timeframes) {
            String[] titles = new String[timeframes.length];

            for (int i = 0; i < timeframes.length; i++) {
                titles[i] = timeframes[i].getLabel();
            }

            return titles;
        }

        // Field name as returned from the REST API
       /* public String getRestApiFieldName() {
            switch (this) {
                case VIEWS:
                    return "views";
                case VISITORS:
                    return "visitors";
                case LIKES:
                    return "likes";
                case REBLOGS:
                    return "reblogs";
                case COMMENTS:
                    return "comments";
            }
            return "";
        } */
    }
}
