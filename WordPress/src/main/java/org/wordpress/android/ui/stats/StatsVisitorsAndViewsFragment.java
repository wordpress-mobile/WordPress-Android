package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CheckedTextView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;

import com.jjoe64.graphview.GraphView;
import com.jjoe64.graphview.GraphViewSeries;

import org.apache.commons.lang3.ArrayUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.stats.models.VisitModel;
import org.wordpress.android.ui.stats.models.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsServiceLogic;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ColorUtils;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.FormatUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.RtlUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.widgets.AutoResizeTextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class StatsVisitorsAndViewsFragment extends StatsAbstractFragment
        implements StatsBarGraph.OnGestureListener {
    public static final String TAG = StatsVisitorsAndViewsFragment.class.getSimpleName();
    private static final String ARG_SELECTED_GRAPH_BAR = "ARG_SELECTED_GRAPH_BAR";
    private static final String ARG_PREV_NUMBER_OF_BARS = "ARG_PREV_NUMBER_OF_BARS";
    private static final String ARG_SELECTED_OVERVIEW_ITEM = "ARG_SELECTED_OVERVIEW_ITEM";
    private static final String ARG_CHECKBOX_SELECTED = "ARG_CHECKBOX_SELECTED";

    private LinearLayout mGraphContainer;
    private LinearLayout mNoActivtyThisPeriodContainer;
    private StatsBarGraph mGraphView;
    private LinearLayout mModuleButtonsContainer;
    private TextView mDateTextView;
    private String[] mStatsDate;

    private LinearLayout mLegendContainer;
    private CheckedTextView mLegendLabel;
    private LinearLayout mVisitorsCheckboxContainer;
    private CheckBox mVisitorsCheckbox;
    private boolean mIsCheckboxChecked = true;

    private OnDateChangeListener mListener;
    private OnOverviewItemChangeListener mOverviewItemChangeListener;

    private final OverviewLabel[] mOverviewItems = {OverviewLabel.VIEWS, OverviewLabel.VISITORS, OverviewLabel.LIKES,
            OverviewLabel.COMMENTS};

    // Restore the following variables on restart
    private VisitsModel mVisitsData;
    private int mSelectedOverviewItemIndex = 0;
    private int mSelectedBarGraphBarIndex = -1;
    private int mPrevNumberOfBarsGraph = -1;

    // Container Activity must implement this interface
    public interface OnDateChangeListener {
        void onDateChanged(long siteId, StatsTimeframe timeframe, String newDate);
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
        for (int i = 0; i < mOverviewItems.length; i++) {
            if (mOverviewItems[i] == itemToSelect) {
                mSelectedOverviewItemIndex = i;
                return;
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.stats_visitors_and_views_fragment, container, false);

        mDateTextView = view.findViewById(R.id.stats_summary_date);
        mGraphContainer = view.findViewById(R.id.stats_bar_chart_fragment_container);
        mModuleButtonsContainer = view.findViewById(R.id.stats_pager_tabs);
        mNoActivtyThisPeriodContainer = view.findViewById(R.id.stats_bar_chart_no_activity);

        mLegendContainer = view.findViewById(R.id.stats_legend_container);
        mLegendLabel = view.findViewById(R.id.stats_legend_label);
        mLegendLabel.setCheckMarkDrawable(
                null); // Make sure to set a null drawable here. Otherwise the touching area is the same of a TextView
        mVisitorsCheckboxContainer = view.findViewById(R.id.stats_checkbox_visitors_container);
        mVisitorsCheckbox = view.findViewById(R.id.stats_checkbox_visitors);
        mVisitorsCheckbox.setOnClickListener(mOnCheckboxClicked);

        // Make sure we've all the info to build the tab correctly. This is ALWAYS true
        if (mModuleButtonsContainer.getChildCount() == mOverviewItems.length) {
            for (int i = 0; i < mModuleButtonsContainer.getChildCount(); i++) {
                LinearLayout currentTab = (LinearLayout) mModuleButtonsContainer.getChildAt(i);
                boolean isLastItem = i == (mOverviewItems.length - 1);
                boolean isChecked = i == mSelectedOverviewItemIndex;
                TabViewHolder currentTabViewHolder =
                        new TabViewHolder(currentTab, mOverviewItems[i], isChecked, isLastItem);
                currentTab.setOnClickListener(mTopButtonsOnClickListener);
                currentTab.setTag(currentTabViewHolder);
            }
            mModuleButtonsContainer.setVisibility(View.VISIBLE);
        }

        return view;
    }

    private class TabViewHolder {
        private final LinearLayout mTab;
        private final LinearLayout mInnerContainer;
        private final AutoResizeTextView mLabel;
        private final TextView mValue;
        private final ImageView mIcon;
        private final OverviewLabel mLabelItem;
        private boolean mIsChecked = false;
        private boolean mIsLastItem = false;

        TabViewHolder(LinearLayout currentTab, OverviewLabel labelItem, boolean checked, boolean isLastItem) {
            mTab = currentTab;
            mInnerContainer = currentTab.findViewById(R.id.stats_visitors_and_views_tab_inner_container);
            mLabel = currentTab.findViewById(R.id.stats_visitors_and_views_tab_label);
            mLabel.setMinTextSize(getResources().getDimensionPixelSize(R.dimen.stats_label_min_text_size));
            mLabel.setText(labelItem.getLabel());
            mValue = currentTab.findViewById(R.id.stats_visitors_and_views_tab_value);
            mIcon = currentTab.findViewById(R.id.stats_visitors_and_views_tab_icon);
            mLabelItem = labelItem;
            mIsChecked = checked;
            mIsLastItem = isLastItem;
            updateBackGroundAndIcon(0);
        }

        private @DrawableRes int getTabIcon() {
            switch (mLabelItem) {
                case VISITORS:
                    return R.drawable.ic_user_white_24dp;
                case COMMENTS:
                    return R.drawable.ic_comment_white_24dp;
                case LIKES:
                    return R.drawable.ic_star_white_24dp;
                default:
                    // Views and when no prev match
                    return R.drawable.ic_visible_on_white_24dp;
            }
        }

        public void updateBackGroundAndIcon(int currentValue) {
            if (mIsChecked) {
                mValue.setTextColor(getResources().getColor(R.color.accent));
            } else {
                if (currentValue == 0) {
                    mValue.setTextColor(getResources().getColor(R.color.neutral));
                } else {
                    mValue.setTextColor(getResources().getColor(R.color.primary_50));
                }
            }

            ColorUtils.INSTANCE.setImageResourceWithTint(mIcon, getTabIcon(), R.color.neutral_70);

            if (mIsLastItem) {
                if (mIsChecked) {
                    mTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_latest_white);
                } else {
                    mTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_latest_blue_light);
                }
            } else {
                if (mIsChecked) {
                    mTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_white);
                } else {
                    mTab.setBackgroundResource(R.drawable.stats_visitors_and_views_button_blue_light);
                }
            }
        }

        public void setChecked(boolean checked) {
            if (checked) {
                mTab.announceForAccessibility(
                        mTab.getContext().getString(R.string.stats_tab_tap_content_description, mLabelItem.getLabel()));
            }
            this.mIsChecked = checked;
        }
    }

    private final View.OnClickListener mTopButtonsOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isAdded()) {
                return;
            }

            // LinearLayout tab = (LinearLayout) v;
            TabViewHolder tabViewHolder = (TabViewHolder) v.getTag();

            if (tabViewHolder.mIsChecked) {
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

            if (checkedId == -1) {
                return;
            }

            mSelectedOverviewItemIndex = checkedId;
            if (mOverviewItemChangeListener != null) {
                mOverviewItemChangeListener.onOverviewItemChanged(
                        mOverviewItems[mSelectedOverviewItemIndex]);
            }
            updateUI();
        }
    };


    private final View.OnClickListener mOnCheckboxClicked = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            // Is the view now checked?
            mIsCheckboxChecked = ((CheckBox) view).isChecked();
            updateUI();
        }
    };


    @Override
    protected boolean hasDataAvailable() {
        return mVisitsData != null;
    }

    @Override
    protected void saveStatsData(Bundle outState) {
        if (hasDataAvailable()) {
            outState.putSerializable(ARG_REST_RESPONSE, mVisitsData);
        }
        outState.putInt(ARG_SELECTED_GRAPH_BAR, mSelectedBarGraphBarIndex);
        outState.putInt(ARG_PREV_NUMBER_OF_BARS, mPrevNumberOfBarsGraph);
        outState.putInt(ARG_SELECTED_OVERVIEW_ITEM, mSelectedOverviewItemIndex);
        outState.putBoolean(ARG_CHECKBOX_SELECTED, mVisitorsCheckbox.isChecked());
    }

    @Override
    protected void restoreStatsData(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
                mVisitsData = (VisitsModel) savedInstanceState.getSerializable(ARG_REST_RESPONSE);
            }
            if (savedInstanceState.containsKey(ARG_SELECTED_OVERVIEW_ITEM)) {
                mSelectedOverviewItemIndex = savedInstanceState.getInt(ARG_SELECTED_OVERVIEW_ITEM, 0);
            }
            if (savedInstanceState.containsKey(ARG_SELECTED_GRAPH_BAR)) {
                mSelectedBarGraphBarIndex = savedInstanceState.getInt(ARG_SELECTED_GRAPH_BAR, -1);
            }
            if (savedInstanceState.containsKey(ARG_PREV_NUMBER_OF_BARS)) {
                mPrevNumberOfBarsGraph = savedInstanceState.getInt(ARG_PREV_NUMBER_OF_BARS, -1);
            }

            mIsCheckboxChecked = savedInstanceState.getBoolean(ARG_CHECKBOX_SELECTED, true);
        }
    }

    @Override
    protected void showErrorUI(String label) {
        setupNoResultsUI(false);
    }

    @Override
    protected void showPlaceholderUI() {
        setupNoResultsUI(true);
    }

    private VisitModel[] getDataToShowOnGraph(VisitsModel visitsData) {
        List<VisitModel> visitModels = visitsData.getVisits();
        int numPoints = Math.min(StatsUIHelper.getNumOfBarsToShow(), visitModels.size());
        int currentPointIndex = numPoints - 1;
        VisitModel[] visitModelsToShow = new VisitModel[numPoints];

        for (int i = visitModels.size() - 1; i >= 0 && currentPointIndex >= 0; i--) {
            VisitModel currentVisitModel = visitModels.get(i);
            visitModelsToShow[currentPointIndex] = currentVisitModel;
            currentPointIndex--;
        }
        if (RtlUtils.isRtl(getActivity())) {
            ArrayUtils.reverse(visitModelsToShow);
        }
        return visitModelsToShow;
    }

    protected void updateUI() {
        if (!isAdded()) {
            return;
        }

        if (mVisitsData == null) {
            setupNoResultsUI(false);
            return;
        }

        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph(mVisitsData);
        if (dataToShowOnGraph == null || dataToShowOnGraph.length == 0) {
            setupNoResultsUI(false);
            return;
        }

        // Hide the "no-activity this period" message
        mNoActivtyThisPeriodContainer.setVisibility(View.GONE);

        // Read the selected Tab in the UI
        OverviewLabel selectedStatsType = mOverviewItems[mSelectedOverviewItemIndex];

        // Update the Legend and enable/disable the visitors checkboxes
        mLegendContainer.setVisibility(View.VISIBLE);
        mLegendLabel.setText(StringUtils.capitalize(selectedStatsType.getLabel().toLowerCase(Locale.getDefault())));
        switch (selectedStatsType) {
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

        // index of days that should be XXX on the graph
        final boolean[] weekendDays;
        if (getTimeframe() == StatsTimeframe.DAY) {
            weekendDays = new boolean[dataToShowOnGraph.length];
        } else {
            weekendDays = null;
        }

        // Check we have at least one result in the current section.
        boolean atLeastOneResultIsAvailable = false;

        // Fill series variables with data
        for (int i = 0; i < dataToShowOnGraph.length; i++) {
            int currentItemValue = 0;
            switch (selectedStatsType) {
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

            if (currentItemValue > 0) {
                atLeastOneResultIsAvailable = true;
            }

            if (mIsCheckboxChecked && secondarySeriesItems != null) {
                secondarySeriesItems[i] = new GraphView.GraphViewData(i, dataToShowOnGraph[i].getVisitors());
            }

            String currentItemStatsDate = dataToShowOnGraph[i].getPeriod();
            horLabels[i] = getDateLabelForBarInGraph(currentItemStatsDate);
            mStatsDate[i] = currentItemStatsDate;

            if (weekendDays != null) {
                SimpleDateFormat from = new SimpleDateFormat(StatsConstants.STATS_INPUT_DATE_FORMAT, Locale.ROOT);
                try {
                    Date date = from.parse(currentItemStatsDate);
                    Calendar c = Calendar.getInstance();
                    c.setFirstDayOfWeek(Calendar.MONDAY);
                    c.setTimeInMillis(date.getTime());
                    weekendDays[i] = c.get(Calendar.DAY_OF_WEEK) == Calendar.SUNDAY
                                     || c.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY;
                } catch (ParseException e) {
                    weekendDays[i] = false;
                    AppLog.e(AppLog.T.STATS, e);
                }
            }
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
        mainSeriesOnScreen.getStyle().color = getResources().getColor(R.color.primary);
        mainSeriesOnScreen.getStyle().outerColor = getResources().getColor(R.color.grey_lighten_30_translucent_50);
        mainSeriesOnScreen.getStyle().highlightColor =
                getResources().getColor(R.color.accent);
        mainSeriesOnScreen.getStyle().outerhighlightColor =
                getResources().getColor(R.color.accent_10);
        mainSeriesOnScreen.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 5);
        mGraphView.addSeries(mainSeriesOnScreen);

        // Add the Visitors series if it's checked in the legend
        if (mIsCheckboxChecked && secondarySeriesItems != null && selectedStatsType == OverviewLabel.VIEWS) {
            GraphViewSeries secondarySeries = new GraphViewSeries(secondarySeriesItems);
            secondarySeries.getStyle().padding = DisplayUtils.dpToPx(getActivity(), 10);
            secondarySeries.getStyle().color = getResources().getColor(R.color.primary_dark);
            secondarySeries.getStyle().highlightColor = getResources().getColor(R.color.accent_60);
            mGraphView.addSeries(secondarySeries);
        }

        // Setup the Y-axis on Visitors and Views Tabs.
        // Views and Visitors tabs have the exact same Y-axis as shifting from one Y-axis to another defeats
        // the purpose of making these bars visually easily to compare.
        switch (selectedStatsType) {
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
                DisplayUtils.dpToPx(getActivity(), StatsConstants.STATS_GRAPH_BAR_MAX_COLUMN_WIDTH_DP));
        mGraphView.getGraphViewStyle().setTextSize(
                getResources().getDimensionPixelOffset(R.dimen.stats_legent_text_size));
        mGraphView.getGraphViewStyle().setVerticalLabelsColor(getResources().getColor(R.color.neutral_60));
        mGraphView.setHorizontalLabels(horLabels);
        mGraphView.setAccessibleHorizontalLabels(makeAccessibleHorizontalLabels(horLabels,
                mainSeriesItems, selectedStatsType));
        mGraphView.setGestureListener(this);
        mGraphView.setImportantForAccessibility(atLeastOneResultIsAvailable
                ? View.IMPORTANT_FOR_ACCESSIBILITY_YES : View.IMPORTANT_FOR_ACCESSIBILITY_NO);

        // If zero results in the current section disable clicks on the graph and show the dialog.
        mNoActivtyThisPeriodContainer.setVisibility(atLeastOneResultIsAvailable ? View.GONE : View.VISIBLE);
        mGraphView.setClickable(atLeastOneResultIsAvailable);

        // Draw the background on weekend days
        mGraphView.setWeekendDays(weekendDays);

        // Reset the bar selected upon rotation of the device when the no. of bars can change with orientation.
        // Only happens on 720DP tablets
        if (mPrevNumberOfBarsGraph != -1 && mPrevNumberOfBarsGraph != dataToShowOnGraph.length) {
            mSelectedBarGraphBarIndex = -1;
            mPrevNumberOfBarsGraph = dataToShowOnGraph.length;
            onBarTapped(getDefaultBarIndex(dataToShowOnGraph));
            mGraphView.highlightBar(getDefaultBarIndex(dataToShowOnGraph));
            return;
        }

        mPrevNumberOfBarsGraph = dataToShowOnGraph.length;
        int barSelectedOnGraph;
        if (mSelectedBarGraphBarIndex == -1) {
            // No previous bar was highlighted, highlight the most recent one
            barSelectedOnGraph = getDefaultBarIndex(dataToShowOnGraph);
        } else if (mSelectedBarGraphBarIndex < dataToShowOnGraph.length) {
            barSelectedOnGraph = mSelectedBarGraphBarIndex;
        } else {
            // A previous bar was highlighted, but it's out of the screen now. This should never happen atm.
            barSelectedOnGraph = getDefaultBarIndex(dataToShowOnGraph);
            mSelectedBarGraphBarIndex = barSelectedOnGraph;
        }

        updateUIBelowTheGraph(barSelectedOnGraph);
        mGraphView.highlightBar(barSelectedOnGraph);
    }

    private String[] makeAccessibleHorizontalLabels(String[] horizontalLabels,
                                                    GraphView.GraphViewData[] dataToShowOnGraph,
                                                    OverviewLabel typeOfStats) {
        String[] accessibleLabels = new String[horizontalLabels.length];

        for (int i = 0; i < horizontalLabels.length; i++) {
            String barDate;

            if (getTimeframe() == StatsTimeframe.MONTH) {
                barDate = StatsUtils.parseDateToLocalizedFormat(
                        horizontalLabels[i],
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_SHORT_FORMAT,
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_FORMAT);
            } else if (getTimeframe() == StatsTimeframe.WEEK) {
                barDate = getString(R.string.stats_bar_week_desc, StatsUtils.parseDateToLocalizedFormat(
                        horizontalLabels[i],
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_SHORT_DAY_SHORT_FORMAT,
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_SHORT_FORMAT));
            } else {
                barDate = horizontalLabels[i];
            }

            accessibleLabels[i] = getString(R.string.stats_bar_date_value_type_desc, barDate,
                    (int) dataToShowOnGraph[i].getY(), typeOfStats.getLabel());
        }

        return accessibleLabels;
    }

    private int getDefaultBarIndex(final VisitModel[] dataToShowOnGraph) {
        return RtlUtils.isRtl(getActivity()) && dataToShowOnGraph.length > 0 ? 0 : dataToShowOnGraph.length - 1;
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

    // update the area right below the graph
    private void updateUIBelowTheGraph(int itemPosition) {
        if (!isAdded()) {
            return;
        }

        if (mVisitsData == null) {
            setupNoResultsUI(false);
            return;
        }

        final VisitModel[] dataToShowOnGraph = getDataToShowOnGraph(mVisitsData);

        // Make sure we've data to show on the screen
        if (dataToShowOnGraph.length == 0) {
            return;
        }

        // This check should never be true, since we put a check on the index in the calling function updateUI()
        if (dataToShowOnGraph.length <= itemPosition || itemPosition == -1) {
            // Make sure we're not highlighting
            itemPosition = dataToShowOnGraph.length - 1;
        }

        String date = mStatsDate[itemPosition];
        if (date == null) {
            AppLog.w(AppLog.T.STATS, "Cannot update the area below the graph if a null date is passed!!");
            return;
        }

        mDateTextView.setText(getDateForDisplayInLabels(date, getTimeframe()));

        VisitModel modelTapped = dataToShowOnGraph[itemPosition];
        for (int i = 0; i < mModuleButtonsContainer.getChildCount(); i++) {
            View o = mModuleButtonsContainer.getChildAt(i);
            if (o instanceof LinearLayout && o.getTag() instanceof TabViewHolder) {
                TabViewHolder tabViewHolder = (TabViewHolder) o.getTag();
                int currentValue = 0;
                switch (tabViewHolder.mLabelItem) {
                    case VIEWS:
                        currentValue = modelTapped.getViews();
                        break;
                    case VISITORS:
                        currentValue = modelTapped.getVisitors();
                        break;
                    case LIKES:
                        currentValue = modelTapped.getLikes();
                        break;
                    case COMMENTS:
                        currentValue = modelTapped.getComments();
                        break;
                }
                tabViewHolder.mValue.setText(FormatUtils.formatDecimal(currentValue));
                tabViewHolder.updateBackGroundAndIcon(currentValue);
            }
        }
    }

    private String getDateForDisplayInLabels(String date, StatsTimeframe timeframe) {
        String prefix = getString(R.string.stats_for);
        switch (timeframe) {
            case DAY:
                return String.format(prefix, StatsUtils.parseDateToLocalizedFormat(
                        date,
                        StatsConstants.STATS_INPUT_DATE_FORMAT,
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_SHORT_FORMAT));
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
                    sdf = new SimpleDateFormat("yyyy'W'MM'W'dd", Locale.ROOT);
                    // Calculate the end of the week
                    parsedDate = sdf.parse(date);
                    c = Calendar.getInstance();
                    c.setFirstDayOfWeek(Calendar.MONDAY);
                    c.setTime(parsedDate);
                    // first day of this week
                    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    String startDateLabel = StatsUtils.msToLocalizedString(c.getTimeInMillis(),
                            StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_LONG_FORMAT);
                    // last day of this week
                    c.add(Calendar.DAY_OF_WEEK, +6);
                    String endDateLabel = StatsUtils.msToLocalizedString(c.getTimeInMillis(),
                            StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_LONG_FORMAT);
                    return String.format(prefix, startDateLabel + " - " + endDateLabel);
                } catch (ParseException e) {
                    AppLog.e(AppLog.T.UTILS, e);
                    return "";
                }
            case MONTH:
                return String.format(prefix, StatsUtils.parseDateToLocalizedFormat(date,
                        StatsConstants.STATS_INPUT_DATE_FORMAT,
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_FORMAT));
            case YEAR:
                return String.format(prefix, StatsUtils.parseDateToLocalizedFormat(date,
                        StatsConstants.STATS_INPUT_DATE_FORMAT,
                        StatsConstants.STATS_OUTPUT_DATE_YEAR_FORMAT));
        }
        return "";
    }

    /**
     * Return the date string that is displayed under each bar in the graph
     */
    private String getDateLabelForBarInGraph(String dateToFormat) {
        switch (getTimeframe()) {
            case DAY:
                return StatsUtils.parseDateToLocalizedFormat(
                        dateToFormat,
                        StatsConstants.STATS_INPUT_DATE_FORMAT,
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_SHORT_DAY_SHORT_FORMAT);
            case WEEK:
                // first four digits are the year
                // followed by Wxx where xx is the month
                // followed by Wxx where xx is the day of the month
                // ex: 2013W07W22 = July 22, 2013
                return StatsUtils.parseDateToLocalizedFormat(dateToFormat, "yyyy'W'MM'W'dd",
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_SHORT_DAY_SHORT_FORMAT);
            case MONTH:
                return StatsUtils.parseDateToLocalizedFormat(dateToFormat, "yyyy-MM",
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_SHORT_FORMAT);
            case YEAR:
                return StatsUtils.parseDateToLocalizedFormat(dateToFormat, StatsConstants.STATS_INPUT_DATE_FORMAT,
                        StatsConstants.STATS_OUTPUT_DATE_YEAR_FORMAT);
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

            final TextView emptyLabel = emptyBarGraphView.findViewById(R.id.stats_bar_graph_empty_label);
            emptyLabel.setText("");
            if (!isLoading) {
                mNoActivtyThisPeriodContainer.setVisibility(View.VISIBLE);
            }

            if (emptyBarGraphView != null) {
                mGraphContainer.removeAllViews();
                mGraphContainer.addView(emptyBarGraphView);
            }
        }
        mDateTextView.setText("");

        for (int i = 0; i < mModuleButtonsContainer.getChildCount(); i++) {
            View o = mModuleButtonsContainer.getChildAt(i);
            if (o instanceof CheckedTextView) {
                CheckedTextView currentBtm = (CheckedTextView) o;
                OverviewLabel overviewItem = (OverviewLabel) currentBtm.getTag();
                String labelPrefix = overviewItem.getLabel() + "\n 0";
                currentBtm.setText(labelPrefix);
            }
        }
    }

    @Override
    protected String getTitle() {
        return getString(R.string.stats_view_visitors_and_views);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(StatsEvents.VisitorsAndViewsUpdated event) {
        if (!shouldUpdateFragmentOnUpdateEvent(event)) {
            return;
        }

        mVisitsData = event.mVisitsAndViews;
        mSelectedBarGraphBarIndex = -1;

        // Reset the bar to highlight
        if (mGraphView != null) {
            mGraphView.resetHighlightBar();
        }

        updateUI();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        if (!shouldUpdateFragmentOnErrorEvent(event)) {
            return;
        }

        mVisitsData = null;
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
        // AppLog.d(AppLog.T.STATS, " Tapped bar date " + mStatsDate[tappedBar]);
        mSelectedBarGraphBarIndex = tappedBar;
        updateUIBelowTheGraph(tappedBar);

        if (!NetworkUtils.checkConnection(getActivity())) {
            return;
        }

        // Update Stats here
        String date = mStatsDate[tappedBar];
        if (date == null) {
            AppLog.w(AppLog.T.STATS, "A bar was tapped but a null date is received!!");
            return;
        }

        // Calculate the correct end date for the selected period
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
                    sdf = new SimpleDateFormat("yyyy'W'MM'W'dd", Locale.ROOT);
                    // Calculate the end of the week
                    parsedDate = sdf.parse(date);
                    c.setTime(parsedDate);
                    // first day of this week
                    c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
                    // last day of this week
                    c.add(Calendar.DAY_OF_WEEK, +6);
                    calculatedDate = StatsUtils.msToLocalizedString(c.getTimeInMillis(),
                            StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
                case MONTH:
                    sdf = new SimpleDateFormat("yyyy-MM", Locale.ROOT);
                    // Calculate the end of the month
                    parsedDate = sdf.parse(date);
                    c.setTime(parsedDate);
                    // last day of this month
                    c.set(Calendar.DAY_OF_MONTH, c.getActualMaximum(Calendar.DAY_OF_MONTH));
                    calculatedDate = StatsUtils.msToLocalizedString(c.getTimeInMillis(),
                            StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
                case YEAR:
                    sdf = new SimpleDateFormat(StatsConstants.STATS_INPUT_DATE_FORMAT, Locale.ROOT);
                    // Calculate the end of the week
                    parsedDate = sdf.parse(date);
                    c.setTime(parsedDate);
                    c.set(Calendar.MONTH, Calendar.DECEMBER);
                    c.set(Calendar.DAY_OF_MONTH, 31);
                    calculatedDate = StatsUtils.msToLocalizedString(c.getTimeInMillis(),
                            StatsConstants.STATS_INPUT_DATE_FORMAT);
                    break;
            }
        } catch (ParseException e) {
            AppLog.e(AppLog.T.UTILS, e);
        }

        if (calculatedDate == null) {
            AppLog.w(AppLog.T.STATS,
                    "A call to request new stats stats is made but date received cannot be parsed!! " + date);
            return;
        }

        // Update the data below the graph
        if (mListener != null) {
            // Should never be null
            SiteModel site = mSiteStore.getSiteByLocalId(getLocalTableBlogID());
            if (site != null && SiteUtils.isAccessedViaWPComRest(site)) {
                mListener.onDateChanged(site.getSiteId(), getTimeframe(), calculatedDate);
            }
        }

        String selectedDate = mStatsDate[tappedBar];

        if (!TextUtils.isEmpty(selectedDate)) {
            mGraphView.announceForAccessibility(getString(R.string.stats_bar_desc,
                    getDateForDisplayInLabels(selectedDate, getTimeframe())));
        }

        AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.STATS_TAPPED_BAR_CHART,
                mSiteStore.getSiteByLocalId(getLocalTableBlogID()));
    }

    public enum OverviewLabel {
        VIEWS(R.string.stats_views),
        VISITORS(R.string.stats_visitors),
        LIKES(R.string.stats_likes),
        COMMENTS(R.string.stats_comments);

        private final int mLabelResId;

        OverviewLabel(int labelResId) {
            mLabelResId = labelResId;
        }

        public String getLabel() {
            return WordPress.getContext().getString(mLabelResId).toUpperCase(Locale.getDefault());
        }
    }

    @Override
    protected StatsServiceLogic.StatsEndpointsEnum[] sectionsToUpdate() {
        return new StatsServiceLogic.StatsEndpointsEnum[]{
                StatsServiceLogic.StatsEndpointsEnum.VISITS
        };
    }
}
