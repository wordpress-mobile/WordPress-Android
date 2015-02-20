package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.stats.exceptions.StatsError;
import org.wordpress.android.ui.stats.models.VisitModel;
import org.wordpress.android.ui.stats.models.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.widgets.TypefaceCache;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;


public class StatsVisitorsAndViewsFragment extends StatsAbstractFragment
        implements StatsBarGraph.OnGestureListener {

    public static final String TAG = StatsVisitorsAndViewsFragment.class.getSimpleName();
    private static final String ARG_SELECTED_GRAPH_BAR = "ARG_SELECTED_GRAPH_BAR";
    private static final String ARG_SELECTED_OVERVIEW_ITEM = "ARG_SELECTED_OVERVIEW_ITEM";
    private static final String ARG_CHECKBOX_SELECTED = "ARG_CHECKBOX_SELECTED";


    private LinearLayout mGraphContainer;
    private StatsBarGraph mGraphView;
    private LinearLayout mModuleButtonsContainer;
    private TextView mDateTextView;
    private String[] mStatsDate;

    private LinearLayout mVisitorsAndViewsCheckBoxesContainer;
    private CheckBox mViewsCheckbox;
    private CheckBox mVisitorsCheckbox;
    private boolean mIsCheckboxChecked;
    private ImageView mLegendViews;
    private ImageView mLegendVisitors;

    private OnDateChangeListener mListener;

    final OverviewLabel[] overviewItems = {OverviewLabel.VIEWS, OverviewLabel.VISITORS, OverviewLabel.LIKES,
            OverviewLabel.COMMENTS};

    // Restore the following variables on restart
    private Serializable mVisitsData; //VisitModel or VolleyError
    private int mSelectedOverviewItemIndex = 0;
    private int mSelectedBarGraphBarIndex = -1;

    // Container Activity must implement this interface
    public interface OnDateChangeListener {
        public void onDateChanged(String blogID, StatsTimeframe timeframe, String newDate);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnDateChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnDateChangeListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);

        mDateTextView = (TextView) view.findViewById(R.id.stats_summary_date);
        mGraphContainer = (LinearLayout) view.findViewById(R.id.stats_bar_chart_fragment_container);
        mModuleButtonsContainer = (LinearLayout) view.findViewById(R.id.stats_pager_tabs);
        mVisitorsAndViewsCheckBoxesContainer = (LinearLayout) view.findViewById(R.id.stats_visitors_and_views_checkboxes_container);
        mViewsCheckbox = (CheckBox) view.findViewById(R.id.stats_checkbox_views);
        mViewsCheckbox.setOnClickListener(onCheckboxClicked);
        mVisitorsCheckbox = (CheckBox) view.findViewById(R.id.stats_checkbox_visitors);
        mVisitorsCheckbox.setOnClickListener(onCheckboxClicked);
        mLegendViews = (ImageView) mVisitorsAndViewsCheckBoxesContainer.findViewById(R.id.stats_label_views);
        mLegendVisitors = (ImageView) mVisitorsAndViewsCheckBoxesContainer.findViewById(R.id.stats_label_visitors);

        // Make sure we've all the info to build the tab correctly. This is ALWAYS true
        if (mModuleButtonsContainer.getChildCount() == overviewItems.length) {
            for (int i = 0; i < mModuleButtonsContainer.getChildCount(); i++) {
                LinearLayout currentTab = (LinearLayout) mModuleButtonsContainer.getChildAt(i);
                boolean isLastItem = i == (overviewItems.length - 1);
                boolean isChecked = i == mSelectedOverviewItemIndex;
                TabViewHolder currentTabViewHolder = new TabViewHolder(currentTab, overviewItems[i], isChecked, isLastItem);
                currentTab.setOnClickListener(TopButtonsOnClickListener);
                currentTab.setTag(currentTabViewHolder);
            }
            mModuleButtonsContainer.setVisibility(View.VISIBLE);
        }
        return view;
    }

    private class TabViewHolder {
        LinearLayout tab;
        LinearLayout innerContainer;
        TextView label;
        TextView value;
        ImageView icon;
        OverviewLabel labelItem;
        boolean isChecked = false;
        boolean isLastItem = false;

        public TabViewHolder(LinearLayout currentTab, OverviewLabel labelItem, boolean checked, boolean isLastItem) {
            tab = currentTab;
            innerContainer = (LinearLayout) currentTab.findViewById(R.id.stats_visitors_and_views_tab_inner_container);
            label = (TextView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_label);
            label.setText(labelItem.getLabel());
            value = (TextView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_value);
            icon = (ImageView) currentTab.findViewById(R.id.stats_visitors_and_views_tab_icon);
            this.labelItem = labelItem;
            this.isChecked = checked;
            this.isLastItem = isLastItem;
            updateBackGroundAndIcon();
        }

        private Drawable getTabIcon() {
            switch (labelItem) {
                case VISITORS:
                    return isChecked ? getResources().getDrawable(R.drawable.stats_icon_visitors_active) :
                            getResources().getDrawable(R.drawable.stats_icon_visitors);
                case COMMENTS:
                    return isChecked ? getResources().getDrawable(R.drawable.stats_icon_comments_active) :
                            getResources().getDrawable(R.drawable.stats_icon_comments);
                case LIKES:
                    return isChecked ? getResources().getDrawable(R.drawable.stats_icon_likes_active) :
                            getResources().getDrawable(R.drawable.stats_icon_likes);
                default:
                    // Views and when no prev match
                    return isChecked ? getResources().getDrawable(R.drawable.stats_icon_views_active) :
                            getResources().getDrawable(R.drawable.stats_icon_views);
            }
        }

        public void updateBackGroundAndIcon() {
            if (isChecked) {
                value.setTextColor(getResources().getColor(R.color.calypso_orange_dark));
            } else {
                value.setTextColor(getResources().getColor(R.color.calypso_blue_unread));
            }

            icon.setImageDrawable(getTabIcon());

            if (isLastItem) {
                if (isChecked) {
                    tab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_latest_white);
                } else {
                    tab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_latest_blue_light);
                }
            } else {
                if (isChecked) {
                    tab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_white);
                } else {
                    tab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_blue_light);
                }
            }
        }

        public void setChecked(boolean checked) {
            this.isChecked = checked;
        }
    }

    private View.OnClickListener TopButtonsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isAdded()) {
                return;
            }

            //LinearLayout tab = (LinearLayout) v;
            TabViewHolder tabViewHolder = (TabViewHolder) v.getTag();

            if (tabViewHolder.isChecked) {
                // already checked. Do nothing
                return;
            }

            int numberOfTabs = mModuleButtonsContainer.getChildCount();
            int checkedId = -1;
            for (int i = 0; i < numberOfTabs; i++) {
                LinearLayout currentTab = (LinearLayout) mModuleButtonsContainer.getChildAt(i);
                TabViewHolder currentTabViewHolder = (TabViewHolder) currentTab.getTag();
                if (tabViewHolder == currentTab.getTag()) {
                    checkedId = i;
                    currentTabViewHolder.setChecked(true);
                } else {
                    currentTabViewHolder.setChecked(false);
                }
            }

            if (checkedId == -1)
                return;

            mIsCheckboxChecked = false;
            mSelectedOverviewItemIndex = checkedId;
            updateUI();
        }
    };


    View.OnClickListener onCheckboxClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Is the view now checked?
            mIsCheckboxChecked = ((CheckBox) view).isChecked();
            // do not need to uncheck the other checkbox here.
            updateUI();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            AppLog.d(T.STATS, "StatsVisitorsAndViewsFragment > restoring instance state");
            if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
                mVisitsData = savedInstanceState.getSerializable(ARG_REST_RESPONSE);
            }
            if (savedInstanceState.containsKey(ARG_SELECTED_OVERVIEW_ITEM)) {
                mSelectedOverviewItemIndex = savedInstanceState.getInt(ARG_SELECTED_OVERVIEW_ITEM, 0);
            }
            if (savedInstanceState.containsKey(ARG_SELECTED_GRAPH_BAR)) {
                mSelectedBarGraphBarIndex = savedInstanceState.getInt(ARG_SELECTED_GRAPH_BAR, -1);
            }

            mIsCheckboxChecked = savedInstanceState.getBoolean(ARG_CHECKBOX_SELECTED, false);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //AppLog.d(T.STATS, "StatsVisitorsAndViewsFragment > saving instance state");

        // FIX for https://github.com/wordpress-mobile/WordPress-Android/issues/2228
        if (mVisitsData != null && mVisitsData instanceof VolleyError) {
            VolleyError currentVolleyError = (VolleyError) mVisitsData;
            mVisitsData = StatsUtils.rewriteVolleyError(currentVolleyError, getString(R.string.error_refresh_stats));
        }
        outState.putSerializable(ARG_REST_RESPONSE, mVisitsData);
        outState.putInt(ARG_SELECTED_GRAPH_BAR, mSelectedBarGraphBarIndex);
        outState.putInt(ARG_SELECTED_OVERVIEW_ITEM, mSelectedOverviewItemIndex);
        outState.putBoolean(ARG_CHECKBOX_SELECTED, mViewsCheckbox.isChecked() || mVisitorsCheckbox.isChecked());

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
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_STATS_SECTION_UPDATED));

        if (mVisitsData != null) {
            updateUI();
        } else {
            setupNoResultsUI(true);
            mMoreDataListener.onRefreshRequested(new StatsService.StatsEndpointsEnum[]{StatsService.StatsEndpointsEnum.VISITS});
        }
    }

    private VisitModel[] getDataToShowOnGraph(VisitsModel visitsData) {
        List<VisitModel> visitModels = visitsData.getVisits();
        int numPoints = Math.min(StatsUIHelper.getNumOfBarsToShow(), visitModels.size());
        int currentPointIndex = numPoints - 1;
        VisitModel[] visitModelsToShow = new VisitModel[numPoints];

        for (int i = visitModels.size() -1; i >= 0 && currentPointIndex >= 0; i--) {
            VisitModel currentVisitModel = visitModels.get(i);
            visitModelsToShow[currentPointIndex] = currentVisitModel;
            currentPointIndex--;
        }
        return visitModelsToShow;
    }

    private void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (mVisitsData == null) {
            setupNoResultsUI(false);
            return;
        }

        if (mVisitsData instanceof VolleyError || mVisitsData instanceof StatsError) {
            setupNoResultsUI(false);
            return;
        }

        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph((VisitsModel)mVisitsData);
        if (dataToShowOnGraph == null || dataToShowOnGraph.length == 0) {
            setupNoResultsUI(false);
            return;
        }

        // Read the selected Tab in the UI
        OverviewLabel selectedStatsType = overviewItems[mSelectedOverviewItemIndex];

        // Enable disable the views/visitors checkboxes and update the legend
        switch(selectedStatsType) {
            case VIEWS:
                mVisitorsAndViewsCheckBoxesContainer.setVisibility(View.VISIBLE);
                // Views tab enabled
                mViewsCheckbox.setEnabled(false);
                mViewsCheckbox.setChecked(false);
                mVisitorsCheckbox.setEnabled(true);
                mVisitorsCheckbox.setChecked(mIsCheckboxChecked);
                mLegendViews.setBackgroundResource(R.drawable.stats_visitors_and_views_legend_background_primary);
                mLegendVisitors.setBackgroundResource(R.drawable.stats_visitors_and_views_legend_background_secondary);
                break;
            case VISITORS:
                mVisitorsAndViewsCheckBoxesContainer.setVisibility(View.VISIBLE);
                // Visitors tab enabled
                mViewsCheckbox.setEnabled(true);
                mViewsCheckbox.setChecked(mIsCheckboxChecked);
                mVisitorsCheckbox.setEnabled(false);
                mVisitorsCheckbox.setChecked(false);
                mLegendViews.setBackgroundResource(R.drawable.stats_visitors_and_views_legend_background_secondary);
                mLegendVisitors.setBackgroundResource(R.drawable.stats_visitors_and_views_legend_background_primary);
                break;
            default:
                // Likes or Comments
                mVisitorsAndViewsCheckBoxesContainer.setVisibility(View.GONE);
                break;
        }

        final String[] horLabels = new String[dataToShowOnGraph.length];
        mStatsDate = new String[dataToShowOnGraph.length];
        GraphView.GraphViewData[] mainSeriesItems = new GraphView.GraphViewData[dataToShowOnGraph.length];

        GraphView.GraphViewData[] secondarySeriesItems = null;
        if (mIsCheckboxChecked) {
            secondarySeriesItems = new GraphView.GraphViewData[dataToShowOnGraph.length];
        }

        for (int i = 0; i < dataToShowOnGraph.length; i++) {
            int currentItemValue = 0;
            switch(selectedStatsType) {
                case VIEWS:
                    currentItemValue = dataToShowOnGraph[i].getViews();
                    break;
                case VISITORS:
                    currentItemValue = dataToShowOnGraph[i].getVisitors();
                    break;
                case LIKES:
                    currentItemValue = dataToShowOnGraph[i].getLikes();
                    break;
                case COMMENTS:
                    currentItemValue = dataToShowOnGraph[i].getComments();
                    break;
            }
            mainSeriesItems[i] = new GraphView.GraphViewData(i, currentItemValue);

            if (mIsCheckboxChecked && secondarySeriesItems != null) {
                switch(selectedStatsType) {
                    case VIEWS:
                        secondarySeriesItems[i] = new GraphView.GraphViewData(i, dataToShowOnGraph[i].getVisitors());
                        break;
                    case VISITORS:
                        secondarySeriesItems[i] = new GraphView.GraphViewData(i,dataToShowOnGraph[i].getViews());
                        break;
                }
            }

            String currentItemStatsDate = dataToShowOnGraph[i].getPeriod();
            horLabels[i] = getDateLabelForBarInGraph(currentItemStatsDate);
            mStatsDate[i] = currentItemStatsDate;
        }

        if (mGraphContainer.getChildCount() >= 1 && mGraphContainer.getChildAt(0) instanceof GraphView) {
            mGraphView = (StatsBarGraph) mGraphContainer.getChildAt(0);
        } else {
            mGraphContainer.removeAllViews();
            mGraphView = new StatsBarGraph(getActivity());
            mGraphContainer.addView(mGraphView);
        }

        mGraphView.removeAllSeries();

        GraphViewSeries mainSeriesOnScreen = new GraphViewSeries(mainSeriesItems);

        if (secondarySeriesItems!= null) {
            // We have 2 series on the screen now. Need to check which one should be drawn first.
            GraphViewSeries secondarySeries = new GraphViewSeries(secondarySeriesItems);

            // Need to check the order now. Views always > Visitors.
            if (selectedStatsType == OverviewLabel.VIEWS) {
                mainSeriesOnScreen.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 5);
                mainSeriesOnScreen.getStyle().highlightColor = getResources().getColor(R.color.calypso_orange_dark);
                mainSeriesOnScreen.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
                secondarySeries.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 8);
                secondarySeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views_inner);
                secondarySeries.getStyle().highlightColor = getResources().getColor(R.color.stats_bar_graph_views_inner_highlight);
                mGraphView.addSeries(mainSeriesOnScreen);
                mGraphView.addSeries(secondarySeries);
            } else {
                secondarySeries.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 5);
                secondarySeries.getStyle().highlightColor = getResources().getColor(R.color.stats_bar_graph_views_inner_highlight);
                secondarySeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views_inner);
                mainSeriesOnScreen.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 8);
                mainSeriesOnScreen.getStyle().highlightColor = getResources().getColor(R.color.calypso_orange_dark);
                mainSeriesOnScreen.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
                mGraphView.addSeries(secondarySeries);
                mGraphView.addSeries(mainSeriesOnScreen);
            }
        } else {
            // add only the main series to the graph
            mainSeriesOnScreen.getStyle().color = getResources().getColor(R.color.stats_bar_graph_views);
            mainSeriesOnScreen.getStyle().highlightColor = getResources().getColor(R.color.calypso_orange_dark);
            mainSeriesOnScreen.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 5);
            mGraphView.addSeries(mainSeriesOnScreen);
        }

       //mGraphView.getGraphViewStyle().setNumHorizontalLabels(getNumOfHorizontalLabels(dataToShowOnGraph.length));
        mGraphView.getGraphViewStyle().setNumHorizontalLabels(dataToShowOnGraph.length);
        mGraphView.setHorizontalLabels(horLabels);
        mGraphView.setGestureListener(this);

        int barSelectedOnGraph;
        if (mSelectedBarGraphBarIndex == -1) {
            // No previous bar was highlighted, highlight the most recent one
            barSelectedOnGraph = dataToShowOnGraph.length - 1;
        } else if (mSelectedBarGraphBarIndex < dataToShowOnGraph.length) {
            barSelectedOnGraph = mSelectedBarGraphBarIndex;
        } else {
            // A previous bar was highlighted, but it's out of the screen now. Device Rotated.
            // This cannot happen now, since we've fixed number of bars on a device. # of bars doesn't change with device rotation.
            barSelectedOnGraph = dataToShowOnGraph.length - 1;
            mSelectedBarGraphBarIndex = barSelectedOnGraph;
            // TODO: make sure to handle this case in the modules below, otherwise the graph is updated but not other fragments
            // that are still pointing to the old selected date.
        }

        updateUIBelowTheGraph(barSelectedOnGraph);
        mGraphView.highlightBar(barSelectedOnGraph);
    }

    //update the area right below the graph
    private void updateUIBelowTheGraph(int itemPosition) {
        if (!isAdded()) {
            return;
        }

        if (mVisitsData == null) {
            setupNoResultsUI(false);
            return;
        }

        if (mVisitsData instanceof VolleyError) {
            setupNoResultsUI(false);
            return;
        }

        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph((VisitsModel)mVisitsData);

        // Make sure we've data to show on the screen
        if (dataToShowOnGraph.length == 0) {
            return;
        }

        // This check should never be true, since we put a check on the index in the calling function updateUI()
        if (dataToShowOnGraph.length <= itemPosition || itemPosition == -1) {
            // Make sure we're not highlighting
            itemPosition = dataToShowOnGraph.length -1;
        }

        String date =  mStatsDate[itemPosition];
        if (date == null) {
            AppLog.w(AppLog.T.STATS, "Cannot update the area below the graph if a null date is passed!!");
            return;
        }

        mDateTextView.setText(getDateForDisplayInLabels(date, getTimeframe()));

        VisitModel modelTapped = dataToShowOnGraph[itemPosition];
        for (int i=0 ; i < mModuleButtonsContainer.getChildCount(); i++) {
            View o = mModuleButtonsContainer.getChildAt(i);
            if (o instanceof LinearLayout && o.getTag() instanceof  TabViewHolder) {
                TabViewHolder tabViewHolder = (TabViewHolder)o.getTag();
                tabViewHolder.updateBackGroundAndIcon();
                switch (tabViewHolder.labelItem) {
                    case VIEWS:
                        tabViewHolder.value.setText(FormatUtils.formatDecimal(modelTapped.getViews()));
                        break;
                    case VISITORS:
                        tabViewHolder.value.setText(FormatUtils.formatDecimal(modelTapped.getVisitors()));
                        break;
                    case LIKES:
                        tabViewHolder.value.setText(FormatUtils.formatDecimal(modelTapped.getLikes()));
                        break;
                    case COMMENTS:
                        tabViewHolder.value.setText(FormatUtils.formatDecimal(modelTapped.getComments()));
                        break;
                }
            }
        }
    }

    private String getDateForDisplayInLabels(String date, StatsTimeframe timeframe) {
        String prefix = getString(R.string.stats_for);
        switch (timeframe) {
            case DAY:
                return String.format(prefix, StatsUtils.parseDate(date, StatsConstants.STATS_INPUT_DATE_FORMAT, StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_SHORT_FORMAT));
            case WEEK:
                try {
                    SimpleDateFormat sdf;
                    Calendar c;
                    final Date parsedDate;
                    // Used in bar graph
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
                    String startDateLabel = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_LONG_FORMAT);
                    // last day of this week
                    c.add(Calendar.DAY_OF_WEEK, + 6);
                    String endDateLabel = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_LONG_FORMAT);
                    return String.format(prefix, startDateLabel + " - " + endDateLabel);
                } catch (ParseException e) {
                    AppLog.e(AppLog.T.UTILS, e);
                    return "";
                }
            case MONTH:
                return String.format(prefix, StatsUtils.parseDate(date, StatsConstants.STATS_INPUT_DATE_FORMAT, StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_FORMAT));
            case YEAR:
                return String.format(prefix, StatsUtils.parseDate(date, StatsConstants.STATS_INPUT_DATE_FORMAT, StatsConstants.STATS_OUTPUT_DATE_YEAR_FORMAT));
        }
        return "";
    }


    /**
     * Return the date string that is displayed under each bar in the graph
     */
    private String getDateLabelForBarInGraph(String dateToFormat) {
        switch (getTimeframe()) {
            case DAY:
                return StatsUtils.parseDate(
                        dateToFormat,
                        StatsConstants.STATS_INPUT_DATE_FORMAT,
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_SHORT_DAY_SHORT_FORMAT
                );
            case WEEK:
                // first four digits are the year
                // followed by Wxx where xx is the month
                // followed by Wxx where xx is the day of the month
                // ex: 2013W07W22 = July 22, 2013
                return StatsUtils.parseDate(dateToFormat, "yyyy'W'MM'W'dd", StatsConstants.STATS_OUTPUT_DATE_MONTH_SHORT_DAY_SHORT_FORMAT);
            case MONTH:
                return StatsUtils.parseDate(dateToFormat, "yyyy-MM", "MMM");
            case YEAR:
                return StatsUtils.parseDate(dateToFormat, StatsConstants.STATS_INPUT_DATE_FORMAT, StatsConstants.STATS_OUTPUT_DATE_YEAR_FORMAT);
            default:
                return dateToFormat;
        }
    }

    private void setupNoResultsUI(boolean isLoading) {
        if (!isAdded()) {
            return;
        }
        mVisitorsAndViewsCheckBoxesContainer.setVisibility(View.GONE);
        mIsCheckboxChecked = false;
        mSelectedBarGraphBarIndex = -1;
        Context context = mGraphContainer.getContext();
        if (context != null) {
            LayoutInflater inflater = LayoutInflater.from(context);
            View emptyBarGraphView = inflater.inflate(R.layout.stats_bar_graph_empty, mGraphContainer, false);

          // We could show loading indicator here
            if (isLoading) {
                final TextView emptyLabel = (TextView) emptyBarGraphView.findViewById(R.id.stats_bar_graph_empty_label);
                emptyLabel.setText("");
            }

            if (emptyBarGraphView != null) {
                mGraphContainer.removeAllViews();
                mGraphContainer.addView(emptyBarGraphView);
            }
        }
        mDateTextView.setText("");

        for (int i=0 ; i < mModuleButtonsContainer.getChildCount(); i++) {
            View o = mModuleButtonsContainer.getChildAt(i);
            if (o instanceof CheckedTextView) {
                CheckedTextView currentBtm = (CheckedTextView)o;
                OverviewLabel overviewItem = (OverviewLabel)currentBtm.getTag();
                String labelPrefix = overviewItem.getLabel() + "\n 0" ;
                currentBtm.setText(labelPrefix);
            }
        }
    }

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

            if (!isAdded()) {
                return;
            }

            if (!action.equals(StatsService.ACTION_STATS_SECTION_UPDATED)) {
                return;
            }

            if (!intent.hasExtra(StatsService.EXTRA_ENDPOINT_NAME)) {
                return;
            }

            StatsService.StatsEndpointsEnum sectionToUpdate = (StatsService.StatsEndpointsEnum) intent.getSerializableExtra(StatsService.EXTRA_ENDPOINT_NAME);
            if (sectionToUpdate != StatsService.StatsEndpointsEnum.VISITS) {
                return;
            }

            if (action.equals(StatsService.ACTION_STATS_SECTION_UPDATED)) {
                Serializable dataObj = intent.getSerializableExtra(StatsService.EXTRA_ENDPOINT_DATA);

                mVisitsData = (dataObj == null || dataObj instanceof VolleyError) ? null : (VisitsModel) dataObj;
                mSelectedBarGraphBarIndex = -1;
                mSelectedOverviewItemIndex = 0;

                // Reset the bar to highlight
                if (mGraphView != null) {
                    mGraphView.resetHighlightBar();
                }

                updateUI();
            }
        }
    };

    @Override
    public void onBarTapped(int tappedBar) {
        //AppLog.d(AppLog.T.STATS, " Tapped bar date " + mStatsDate[tappedBar]);
        mSelectedBarGraphBarIndex = tappedBar;
        updateUIBelowTheGraph(tappedBar);

        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        // Update Stats here
        String date =  mStatsDate[tappedBar];
        if (date == null) {
            AppLog.w(AppLog.T.STATS, "A bar was tapped but a null date is received!!");
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
                    calculatedDate = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
                case MONTH:
                    sdf = new SimpleDateFormat("yyyy-MM");
                    //Calculate the end of the month
                    parsedDate = sdf.parse(date);
                    c.setTime(parsedDate);
                    // last day of this month
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
                    calculatedDate = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
                case YEAR:
                    sdf = new SimpleDateFormat(StatsConstants.STATS_INPUT_DATE_FORMAT);
                    //Calculate the end of the week
                    parsedDate = sdf.parse(date);
                    c.setTime(parsedDate);
                    c.set(Calendar.MONTH, Calendar.DECEMBER);
                    c.set(Calendar.DAY_OF_MONTH, 31);
                    calculatedDate = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
            }
        } catch (ParseException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        if (calculatedDate == null) {
            AppLog.w(AppLog.T.STATS, "A call to request new stats stats is made but date received cannot be parsed!! " + date);
            return;
        }

        // Update the data below the graph
        if (mListener!= null) {
            // Should never be null
            final String blogId = StatsUtils.getBlogId(getLocalTableBlogID());
            mListener.onDateChanged(blogId, getTimeframe(), calculatedDate);
        }

        AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_TAPPED_BAR_CHART);
    }

    private enum OverviewLabel {
        VIEWS(R.string.stats_views),
        VISITORS(R.string.stats_visitors),
        LIKES(R.string.stats_likes),
        COMMENTS(R.string.stats_comments),
        ;

        private final int mLabelResId;

        private OverviewLabel(int labelResId) {
            mLabelResId = labelResId;
        }

        public String getLabel() {
            return WordPress.getContext().getString(mLabelResId).toUpperCase();
        }

        public static String[] toStringArray(OverviewLabel[] timeframes) {
            String[] titles = new String[timeframes.length];

            for (int i = 0; i < timeframes.length; i++) {
                titles[i] = timeframes[i].getLabel();
            }

            return titles;
        }
    }

    @Override
    protected void resetDataModel() {
        mVisitsData = null;
    }
}
