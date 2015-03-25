package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckedTextView;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.volley.NoConnectionError;
import com.android.volley.VolleyError;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.exceptions.StatsError;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.TypefaceCache;

import java.io.Serializable;

import de.greenrobot.event.EventBus;

public abstract class StatsAbstractListFragment extends StatsAbstractFragment {

    protected static final String ARGS_IS_SINGLE_VIEW = "ARGS_IS_SINGLE_VIEW";

    // Used when the fragment has 2 pages/kind of stats in it. Not meaning the bottom pagination.
    protected static final String ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX = "ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX";
    protected static final int MAX_NUM_OF_ITEMS_DISPLAYED_IN_SINGLE_VIEW_LIST = 1000;
    protected static final int MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST = 10;

    protected static final int NO_STRING_ID = -1;

    protected TextView mModuleTitleTextView;
    protected TextView mEmptyLabel;
    protected TextView mTotalsLabel;
    protected LinearLayout mListContainer;
    protected LinearLayout mList;
    protected Button mViewAll;

    protected LinearLayout mTopPagerContainer;
    protected int mTopPagerSelectedButtonIndex = 0;

    // Bottom and Top Pagination for modules that has pagination enabled.
    protected LinearLayout mBottomPaginationContainer;
    protected Button mBottomPaginationGoBackButton;
    protected Button mBottomPaginationGoForwardButton;
    protected TextView mBottomPaginationText;
    protected LinearLayout mTopPaginationContainer;
    protected Button mTopPaginationGoBackButton;
    protected Button mTopPaginationGoForwardButton;
    protected TextView mTopPaginationText;

    protected LinearLayout mEmptyModulePlaceholder;

    protected Serializable[] mDatamodels;

    protected SparseBooleanArray mGroupIdToExpandedMap;

    protected abstract int getEntryLabelResId();
    protected abstract int getTotalsLabelResId();
    protected abstract int getEmptyLabelTitleResId();
    protected abstract int getEmptyLabelDescResId();
    protected abstract StatsService.StatsEndpointsEnum[] getSectionsToUpdate();
    protected abstract void updateUI();
    protected abstract boolean isExpandableList();
    protected abstract boolean isViewAllOptionAvailable();


    protected StatsResourceVars mResourceVars;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mResourceVars = new StatsResourceVars(activity);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        if (isExpandableList()) {
            view = inflater.inflate(R.layout.stats_expandable_list_fragment, container, false);
        } else {
            view = inflater.inflate(R.layout.stats_list_fragment, container, false);
        }

        mEmptyModulePlaceholder = (LinearLayout) view.findViewById(R.id.stats_empty_module_placeholder);
        mModuleTitleTextView = (TextView) view.findViewById(R.id.stats_module_title);
        mModuleTitleTextView.setText(getTitle());

        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setText(getEntryLabelResId());
        TextView totalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        totalsLabel.setText(getTotalsLabelResId());

        mEmptyLabel = (TextView) view.findViewById(R.id.stats_list_empty_text);
        mTotalsLabel = (TextView) view.findViewById(R.id.stats_module_totals_label);
        mList = (LinearLayout) view.findViewById(R.id.stats_list_linearlayout);
        mListContainer = (LinearLayout) view.findViewById(R.id.stats_list_container);
        mViewAll = (Button) view.findViewById(R.id.btnViewAll);
        mTopPagerContainer = (LinearLayout) view.findViewById(R.id.stats_pager_tabs);

        // Load pagination items
        mBottomPaginationContainer = (LinearLayout) view.findViewById(R.id.stats_bottom_pagination_container);
        mBottomPaginationGoBackButton = (Button) mBottomPaginationContainer.findViewById(R.id.stats_pagination_go_back);
        mBottomPaginationGoForwardButton = (Button) mBottomPaginationContainer.findViewById(R.id.stats_pagination_go_forward);
        mBottomPaginationText = (TextView) mBottomPaginationContainer.findViewById(R.id.stats_pagination_text);
        mTopPaginationContainer = (LinearLayout) view.findViewById(R.id.stats_top_pagination_container);
        mTopPaginationContainer.setBackgroundResource(R.drawable.stats_pagination_item_background);
        mTopPaginationGoBackButton = (Button) mTopPaginationContainer.findViewById(R.id.stats_pagination_go_back);
        mTopPaginationGoForwardButton = (Button) mTopPaginationContainer.findViewById(R.id.stats_pagination_go_forward);
        mTopPaginationText = (TextView) mTopPaginationContainer.findViewById(R.id.stats_pagination_text);

        return view;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        //AppLog.d(AppLog.T.STATS, this.getTag() + " > onCreate");
        mGroupIdToExpandedMap = new SparseBooleanArray();

        if (savedInstanceState != null) {
            AppLog.d(AppLog.T.STATS, this.getTag() + " > restoring instance state");
            if (savedInstanceState.containsKey(ARG_REST_RESPONSE)) {
                Serializable oldData = savedInstanceState.getSerializable(ARG_REST_RESPONSE);
                if (oldData != null && oldData instanceof Serializable[]) {
                    mDatamodels = (Serializable[]) oldData;
                }
            }
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        //AppLog.d(AppLog.T.STATS, this.getTag() + " > saving instance state");

        // Do not serialize VolleyError, but rewrite in a simple stats Exception.
        // VolleyErrors should be serializable, but for some reason they are not.
        // FIX for https://github.com/wordpress-mobile/WordPress-Android/issues/2228
        if (mDatamodels != null) {
            for (int i=0; i < mDatamodels.length; i++) {
                if (mDatamodels[i] != null && mDatamodels[i] instanceof VolleyError) {
                    VolleyError currentVolleyError = (VolleyError) mDatamodels[i];
                    mDatamodels[i] = StatsUtils.rewriteVolleyError(currentVolleyError, getString(R.string.error_refresh_stats));
                }
            }
        }

        outState.putSerializable(ARG_REST_RESPONSE, mDatamodels);
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
        //AppLog.d(AppLog.T.STATS, this.getTag() + " > onResume");
        super.onResume();

        // Init the UI
        if (mDatamodels != null) {
            updateUI();
        } else {
            //showHideNoResultsUI(true);
            showEmptyUI();
            mMoreDataListener.onRefreshRequested(getSectionsToUpdate());
        }
    }

    protected void showEmptyUI() {
        mTopPagerContainer.setVisibility(View.GONE);
        mEmptyLabel.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);
        mList.setVisibility(View.GONE);
        mViewAll.setVisibility(View.GONE);
        mBottomPaginationContainer.setVisibility(View.GONE);
        mTopPaginationContainer.setVisibility(View.GONE);
        mEmptyModulePlaceholder.setVisibility(View.VISIBLE);
    }

    protected void showHideNoResultsUI(boolean showNoResultsUI) {
        mModuleTitleTextView.setVisibility(View.VISIBLE);
        mEmptyModulePlaceholder.setVisibility(View.GONE);

        if (showNoResultsUI) {
            mGroupIdToExpandedMap.clear();
            String label;
            if (getEmptyLabelDescResId() == NO_STRING_ID) {
                label = "<b>" + getString(getEmptyLabelTitleResId()) + "</b><br/><br/>";
            } else {
                label = "<b>" + getString(getEmptyLabelTitleResId()) + "</b><br/><br/>" + getString(getEmptyLabelDescResId());
            }
            if (label.contains("<")) {
                mEmptyLabel.setText(Html.fromHtml(label));
            } else {
                mEmptyLabel.setText(label);
            }
            mEmptyLabel.setVisibility(View.VISIBLE);
            mListContainer.setVisibility(View.GONE);
            mList.setVisibility(View.GONE);
            mViewAll.setVisibility(View.GONE);
            mBottomPaginationContainer.setVisibility(View.GONE);
            mTopPaginationContainer.setVisibility(View.GONE);
        } else {
            mEmptyLabel.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
            mList.setVisibility(View.VISIBLE);

            if (!isSingleView() && isViewAllOptionAvailable()) {
                // No view all button if already in single view
                configureViewAllButton();
            } else {
                mViewAll.setVisibility(View.GONE);
            }
        }
    }

    protected void showErrorUI() {
        if (!isAdded()) {
            return;
        }
        showErrorUI(mDatamodels[mTopPagerSelectedButtonIndex]);
    }

    protected void showErrorUI(Serializable error) {
        if (!isAdded()) {
            return;
        }

        mGroupIdToExpandedMap.clear();
        mModuleTitleTextView.setVisibility(View.VISIBLE);
        mEmptyModulePlaceholder.setVisibility(View.GONE);

        String label = "<b>" + getString(R.string.error_refresh_stats) + "</b>";

        if (error instanceof NoConnectionError) {
            label += "<br/>" + getString(R.string.no_network_message);
        }

        // No need to show detailed error messages to the user
        /*else if (error instanceof VolleyError) {
            String volleyErrorMsg =   ((VolleyError)error).getMessage();
            if (org.apache.commons.lang.StringUtils.isNotBlank(volleyErrorMsg)){
                label += volleyErrorMsg;
            }
        }*/

        if (label.contains("<")) {
            mEmptyLabel.setText(Html.fromHtml(label));
        } else {
            mEmptyLabel.setText(label);
        }
        mEmptyLabel.setVisibility(View.VISIBLE);
        mListContainer.setVisibility(View.GONE);
        mList.setVisibility(View.GONE);
    }

    /**
     * Check if the current datamodel is populated and is NOT an error response.
     *
     */
    protected boolean isDataEmpty() {
        return isDataEmpty(mTopPagerSelectedButtonIndex);
    }

    protected boolean isDataEmpty(int index) {
        return mDatamodels == null
                || mDatamodels[index] == null
                || isErrorResponse(index);
    }

    /**
     * Check if the current datamodel is an error response.
     *
     * @return true if it is a Volley Error
     */
    protected boolean isErrorResponse() {
        return isErrorResponse(mTopPagerSelectedButtonIndex);
    }

    protected boolean isErrorResponse(int index) {
        return mDatamodels != null && mDatamodels[index] != null
                && (mDatamodels[index] instanceof VolleyError || mDatamodels[index] instanceof StatsError);
    }

    protected boolean isSingleView() {
        return getArguments().getBoolean(ARGS_IS_SINGLE_VIEW, false);
    }

    private void configureViewAllButton() {
        if (isSingleView()) {
            // No view all button if you're already in single view
            mViewAll.setVisibility(View.GONE);
            return;
        }
        mViewAll.setVisibility(View.VISIBLE);
        mViewAll.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isSingleView()) {
                    return; // already in single view
                }

                // Model cannot be null here
                if (mDatamodels == null) {
                    return;
                }

                Intent viewAllIntent = new Intent(getActivity(), StatsViewAllActivity.class);
                viewAllIntent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, getLocalTableBlogID());
                viewAllIntent.putExtra(StatsAbstractFragment.ARGS_TIMEFRAME, getTimeframe());
                viewAllIntent.putExtra(StatsAbstractFragment.ARGS_VIEW_TYPE, getViewType().ordinal());
                viewAllIntent.putExtra(StatsAbstractFragment.ARGS_START_DATE, getStartDate());
                viewAllIntent.putExtra(ARGS_IS_SINGLE_VIEW, true);
                if (mTopPagerContainer.getVisibility() == View.VISIBLE) {
                    viewAllIntent.putExtra(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, mTopPagerSelectedButtonIndex);
                }
                //viewAllIntent.putExtra(StatsAbstractFragment.ARG_REST_RESPONSE, mDatamodels[mTopPagerSelectedButtonIndex]);
                getActivity().startActivity(viewAllIntent);
            }
        });
    }

    protected int getMaxNumberOfItemsToShowInList() {
        return isSingleView() ? MAX_NUM_OF_ITEMS_DISPLAYED_IN_SINGLE_VIEW_LIST : MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST;
    }

    protected void setupTopModulePager(LayoutInflater inflater, ViewGroup container, View view, String[] buttonTitles) {
        int dp4 = DisplayUtils.dpToPx(view.getContext(), 4);
        int dp80 = DisplayUtils.dpToPx(view.getContext(), 80);

        for (int i = 0; i < buttonTitles.length; i++) {
            CheckedTextView rb = (CheckedTextView) inflater.inflate(R.layout.stats_top_module_pager_button, container, false);
            RadioGroup.LayoutParams params = new RadioGroup.LayoutParams(RadioGroup.LayoutParams.MATCH_PARENT,
                    RadioGroup.LayoutParams.WRAP_CONTENT);
            params.weight = 1;
            rb.setTypeface((TypefaceCache.getTypeface(view.getContext())));
            if (i == 0) {
                params.setMargins(0, 0, dp4, 0);
            } else {
                params.setMargins(dp4, 0, 0, 0);
            }
            rb.setMinimumWidth(dp80);
            rb.setGravity(Gravity.CENTER);
            rb.setLayoutParams(params);
            rb.setText(buttonTitles[i]);
            rb.setChecked(i == mTopPagerSelectedButtonIndex);
            rb.setOnClickListener(TopModulePagerOnClickListener);
            mTopPagerContainer.addView(rb);
        }
        mTopPagerContainer.setVisibility(View.VISIBLE);
    }

    protected View.OnClickListener TopModulePagerOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if (!isAdded()) {
                return;
            }

            CheckedTextView ctv = (CheckedTextView) v;
            if (ctv.isChecked()) {
                // already checked. Do nothing
                return;
            }

            int numberOfButtons = mTopPagerContainer.getChildCount();
            int checkedId = -1;
            for (int i = 0; i < numberOfButtons; i++) {
                CheckedTextView currentCheckedTextView = (CheckedTextView)mTopPagerContainer.getChildAt(i);
                if (ctv == currentCheckedTextView) {
                    checkedId = i;
                    currentCheckedTextView.setChecked(true);
                } else {
                    currentCheckedTextView.setChecked(false);
                }
            }

            if (checkedId == -1)
                return;

            mTopPagerSelectedButtonIndex = checkedId;

            TextView entryLabel = (TextView) getView().findViewById(R.id.stats_list_entry_label);
            entryLabel.setText(getEntryLabelResId());
            updateUI();
        }
    };

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdated event) {
        if (!isAdded()) {
            return;
        }

        StatsService.StatsEndpointsEnum sectionToUpdate = event.mEndPointName;
        StatsService.StatsEndpointsEnum[] sectionsToUpdate = getSectionsToUpdate();
        int indexOfDatamodelMatch = -1;
        for (int i = 0; i < getSectionsToUpdate().length; i++) {
            if (sectionToUpdate == sectionsToUpdate[i]) {
                indexOfDatamodelMatch = i;
                break;
            }
        }

        if (-1 == indexOfDatamodelMatch) {
            return;
        }

        mGroupIdToExpandedMap.clear();

        if (mDatamodels == null) {
            mDatamodels = new Serializable[getSectionsToUpdate().length];
        }

        mDatamodels[indexOfDatamodelMatch] = event.mResponseObjectModel;
        updateUI();
    }

    @Override
    protected void resetDataModel() {
        mDatamodels = null;
    }
}
