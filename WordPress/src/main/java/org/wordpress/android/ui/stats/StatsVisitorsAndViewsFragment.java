package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.greenrobot.event.EventBus;

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

    private LinearLayout mLegendContainer;
    private CheckedTextView mLegendLabel;
    private LinearLayout mVisitorsCheckboxContainer;
    private CheckBox mVisitorsCheckbox;
    private boolean mIsCheckboxChecked;

    private OnDateChangeListener mListener;
    private OnOverviewItemChangeListener mOverviewItemChangeListener;

    private final OverviewLabel[] overviewItems = {OverviewLabel.VIEWS, OverviewLabel.VISITORS, OverviewLabel.LIKES,
            OverviewLabel.COMMENTS};

    // Restore the following variables on restart
    private Serializable mVisitsData; //VisitModel or VolleyError
    private int mSelectedOverviewItemIndex = 0;
    private int mSelectedBarGraphBarIndex = -1;

    // Container Activity must implement this interface
    public interface OnDateChangeListener {
        void onDateChanged(String blogID, StatsTimeframe timeframe, String newDate);
    }

    // Container Activity must implement this interface
    public interface OnOverviewItemChangeListener {
        void onOverviewItemChanged(OverviewLabel newItem);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mListener = (OnDateChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnDateChangeListener");
        }
        try {
            mOverviewItemChangeListener = (OnOverviewItemChangeListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnOverviewItemChangeListener");
        }
    }

    void setSelectedOverviewItem(OverviewLabel itemToSelect) {
        for (int i = 0; i < overviewItems.length; i++) {
            if (overviewItems[i] == itemToSelect) {
                mSelectedOverviewItemIndex = i;
                return;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);

        mDateTextView = (TextView) view.findViewById(R.id.stats_summary_date);
        mGraphContainer = (LinearLayout) view.findViewById(R.id.stats_bar_chart_fragment_container);
        mModuleButtonsContainer = (LinearLayout) view.findViewById(R.id.stats_pager_tabs);

        mLegendContainer = (LinearLayout) view.findViewById(R.id.stats_legend_container);
        mLegendLabel = (CheckedTextView) view.findViewById(R.id.stats_legend_label);
        mLegendLabel.setCheckMarkDrawable(null); // Make sure to set a null drawable here. Otherwise the touching area is the same of a TextView
        mVisitorsCheckboxContainer = (LinearLayout) view.findViewById(R.id.stats_checkbox_visitors_container);
        mVisitorsCheckbox = (CheckBox) view.findViewById(R.id.stats_checkbox_visitors);
        mVisitorsCheckbox.setOnClickListener(onCheckboxClicked);

        // Fix an issue on devices with 4.1 or lower, where the Checkbox already uses padding by default internally and overriding it with paddingLeft
        // causes the issue report here https://github.com/wordpress-mobile/WordPress-Android/pull/2377#issuecomment-77067993
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            mVisitorsCheckbox.setPadding(getResources().getDimensionPixelSize(R.dimen.margin_medium), 0, 0, 0);
        }

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
        final LinearLayout tab;
        final LinearLayout innerContainer;
        final TextView label;
        final TextView value;
        final ImageView icon;
        final OverviewLabel labelItem;
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
                label.setTextColor(getResources().getColor(R.color.grey_dark));
                value.setTextColor(getResources().getColor(R.color.orange_jazzy));
            } else {
                label.setTextColor(getResources().getColor(R.color.grey_darken_20));
                value.setTextColor(getResources().getColor(R.color.blue_wordpress));
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

    private final View.OnClickListener TopButtonsOnClickListener = new View.OnClickListener() {
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

            mSelectedOverviewItemIndex = checkedId;
            if (mOverviewItemChangeListener != null) {
                mOverviewItemChangeListener.onOverviewItemChanged(
                        overviewItems[mSelectedOverviewItemIndex]
                );
            }
            updateUI();
        }
    };


    private final View.OnClickListener onCheckboxClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Is the view now checked?
            mIsCheckboxChecked = ((CheckBox) view).isChecked();
            updateUI();
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
                mVisitsData = savedInstanceState.getSerializable(ARG_REST_RESPONSE);
            }
            if (savedInstanceState.containsKey(ARG_SELECTED_OVERVIEW_ITEM)) {
                mSelectedOverviewItemIndex = savedInstanceState.getInt(ARG_SELECTED_OVERVIEW_ITEM, 0);
            }
            if (savedInstanceState.containsKey(ARG_SELECTED_GRAPH_BAR)) {
                mSelectedBarGraphBarIndex = savedInstanceState.getInt(ARG_SELECTED_GRAPH_BAR, -1);
            }

            mIsCheckboxChecked = savedInstanceState.getBoolean(ARG_CHECKBOX_SELECTED, true);
        } else {
            mIsCheckboxChecked = true;
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
        outState.putBoolean(ARG_CHECKBOX_SELECTED, mVisitorsCheckbox.isChecked());

        super.onSaveInstanceState(outState);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mVisitsData != null) {
            updateUI();
        } else {
            if (NetworkUtils.isNetworkAvailable(getActivity())) {
                setupNoResultsUI(true);
                refreshStats();
            } else {
                setupNoResultsUI(false);
            }
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

        // Update the Legend and enable/disable the visitors checkboxes
        mLegendContainer.setVisibility(View.VISIBLE);
        mLegendLabel.setText(StringUtils.capitalize(selectedStatsType.getLabel().toLowerCase()));
        switch(selectedStatsType) {
            case VIEWS:
                mVisitorsCheckboxContainer.setVisibility(View.VISIBLE);
                mVisitorsCheckbox.setEnabled(true);
                mVisitorsCheckbox.setChecked(mIsCheckboxChecked);
                break;
            default:
                mVisitorsCheckboxContainer.setVisibility(View.GONE);
                break;
        }

        // Setting Up labels and prepare variables that hold series
        final String[] horLabels = new String[dataToShowOnGraph.length];
        mStatsDate = new String[dataToShowOnGraph.length];
        GraphView.GraphViewData[] mainSeriesItems = new GraphView.GraphViewData[dataToShowOnGraph.length];

        GraphView.GraphViewData[] secondarySeriesItems = null;
        if (mIsCheckboxChecked && selectedStatsType == OverviewLabel.VIEWS) {
            secondarySeriesItems = new GraphView.GraphViewData[dataToShowOnGraph.length];
        }

        // Fill series variables with data
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
                secondarySeriesItems[i] = new GraphView.GraphViewData(i, dataToShowOnGraph[i].getVisitors());
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
        mainSeriesOnScreen.getStyle().color = getResources().getColor(R.color.stats_bar_graph_main_series);
        mainSeriesOnScreen.getStyle().highlightColor = getResources().getColor(R.color.stats_bar_graph_main_series_highlight);
        mainSeriesOnScreen.getStyle().outerhighlightColor = getResources().getColor(R.color.stats_bar_graph_outer_highlight);
        mainSeriesOnScreen.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 5);
        mGraphView.addSeries(mainSeriesOnScreen);

        // Add the Visitors series if it's checked in the legend
        if (mIsCheckboxChecked && secondarySeriesItems != null && selectedStatsType == OverviewLabel.VIEWS) {
            GraphViewSeries secondarySeries = new GraphViewSeries(secondarySeriesItems);
            secondarySeries.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 10);
            secondarySeries.getStyle().color = getResources().getColor(R.color.stats_bar_graph_secondary_series);
            secondarySeries.getStyle().highlightColor = getResources().getColor(R.color.orange_fire);
            mGraphView.addSeries(secondarySeries);
        }

        // Setup the Y-axis on Visitors and Views Tabs.
        // Views and Visitors tabs have the exact same Y-axis as shifting from one Y-axis to another defeats
        // the purpose of making these bars visually easily to compare.
        switch(selectedStatsType) {
            case VISITORS:
                double maxYValue = getMaxYValueForVisitorsAndView(dataToShowOnGraph);
                mGraphView.setManualYAxisBounds(maxYValue, 0d);
                break;
            default:
                mGraphView.setManualYAxis(false);
                break;
        }

        // Set the Graph Style
        mGraphView.getGraphViewStyle().setNumHorizontalLabels(dataToShowOnGraph.length);
        // Set the maximum size a column can get on the screen in PX
        mGraphView.getGraphViewStyle().setMaxColumnWidth(
                DisplayUtils.dpToPx(getActivity(), StatsConstants.STATS_GRAPH_BAR_MAX_COLUMN_WIDTH_DP)
        );
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

    // Find the max value in Visitors and Views data.
    // Only checks the Views data, since Visitors is for sure less-equals than Views.
    private double getMaxYValueForVisitorsAndView(final VisitModel[] dataToShowOnGraph) {
        if (dataToShowOnGraph == null || dataToShowOnGraph.length == 0) {
            return 0d;
        }
        double largest = Integer.MIN_VALUE;

        for (VisitModel aDataToShowOnGraph : dataToShowOnGraph) {
            int currentItemValue = aDataToShowOnGraph.getViews();
            if (currentItemValue > largest) {
                largest = currentItemValue;
            }
        }
        return largest;
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
                    c.setFirstDayOfWeek(Calendar.MONDAY);
                    c.setTime(parsedDate);
                    // first day of this week
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

        // Hide the legend
        mLegendContainer.setVisibility(View.GONE);
        mVisitorsCheckboxContainer.setVisibility(View.GONE);

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

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdated event) {
        if (!isAdded()) {
            return;
        }

        if (!getDate().equals(event.mDate)) {
            return;
        }

        if (!event.mRequestBlogId.equals(StatsUtils.getBlogId(getLocalTableBlogID()))) {
            return;
        }

        if (event.mTimeframe != getTimeframe()) {
            return;
        }

        StatsService.StatsEndpointsEnum sectionToUpdate = event.mEndPointName;
        if (sectionToUpdate != StatsService.StatsEndpointsEnum.VISITS) {
            return;
        }

        Serializable dataObj = event.mResponseObjectModel;

        mVisitsData = (dataObj == null || dataObj instanceof VolleyError) ? null : (VisitsModel) dataObj;
        mSelectedBarGraphBarIndex = -1;

        // Reset the bar to highlight
        if (mGraphView != null) {
            mGraphView.resetHighlightBar();
        }

        updateUI();
    }

    @Override
    public void onBarTapped(int tappedBar) {
        if (!isAdded()) {
            return;
        }
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

    public enum OverviewLabel {
        VIEWS(R.string.stats_views),
        VISITORS(R.string.stats_visitors),
        LIKES(R.string.stats_likes),
        COMMENTS(R.string.stats_comments),
        ;

        private final int mLabelResId;

        OverviewLabel(int labelResId) {
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
    protected StatsService.StatsEndpointsEnum[] getSectionsToUpdate() {
        return new StatsService.StatsEndpointsEnum[]{
                StatsService.StatsEndpointsEnum.VISITS
        };
    }
}
