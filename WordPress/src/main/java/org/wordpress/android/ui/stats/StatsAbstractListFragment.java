package org.wordpress.android.ui.stats;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.Html;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.android.volley.NoConnectionError;
import com.android.volley.VolleyError;

import org.wordpress.android.R;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.Locale;


public abstract class StatsAbstractListFragment extends StatsAbstractFragment {

    protected static final String ARGS_IS_SINGLE_VIEW = "ARGS_IS_SINGLE_VIEW";

    // Used when the fragment has 2 pages/kind of stats in it. Not meaning the bottom pagination.
    protected static final String ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX = "ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX";
    protected static final int MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST = 1000;

    protected static final int NO_STRING_ID = -1;

    protected TextView mModuleTitleTextView;
    protected TextView mModuleTitleTextPlaceholderTextView;
    protected TextView mEmptyLabel;
    protected TextView mTotalsLabel;
    protected LinearLayout mListContainer;
    protected LinearLayout mList;
    protected Serializable[] mDatamodels;
    protected Button mViewAll;
    protected RadioGroup mTopPagerRadioGroup;
    protected int mTopPagerSelectedButtonIndex = 0;

    protected LinearLayout mPaginationContainer;
    protected Button mPaginationGoBackButton;
    protected Button mPaginationGoForwardButton;
    protected TextView mPaginationText;
    protected OnRequestDataListener mMoreDataListener;

    protected SparseBooleanArray mGroupIdToExpandedMap;

    protected abstract int getEntryLabelResId();
    protected abstract int getTotalsLabelResId();
    protected abstract int getEmptyLabelTitleResId();
    protected abstract int getEmptyLabelDescResId();
    protected abstract StatsService.StatsEndpointsEnum[] getSectionToUpdate();
    protected abstract void updateUI();
    protected abstract boolean isExpandableList();
    protected abstract boolean isViewAllOptionAvailable();


    protected StatsResourceVars mResourceVars;


    // Container Activity must implement this interface
    public interface OnRequestDataListener {
        public void onRefreshRequested(StatsService.StatsEndpointsEnum[] endPointsNeedUpdate);
        public void onMoreDataRequested(StatsService.StatsEndpointsEnum endPointNeedUpdate, int pageNumber);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        mResourceVars = new StatsResourceVars(activity);
        try {
            mMoreDataListener = (OnRequestDataListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString() + " must implement OnRequestMoreDataListener");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view;
        if (isExpandableList()) {
            view = inflater.inflate(R.layout.stats_expandable_list_fragment, container, false);
        } else {
            view = inflater.inflate(R.layout.stats_list_fragment, container, false);
        }

        mModuleTitleTextView = (TextView) view.findViewById(R.id.stats_module_title);
        mModuleTitleTextView.setText(getTitle().toUpperCase(Locale.getDefault()));

        mModuleTitleTextPlaceholderTextView = (TextView) view.findViewById(R.id.stats_module_title_placeholder);
        mModuleTitleTextPlaceholderTextView.setText("               ");

        TextView entryLabel = (TextView) view.findViewById(R.id.stats_list_entry_label);
        entryLabel.setText(getEntryLabelResId());
        TextView totalsLabel = (TextView) view.findViewById(R.id.stats_list_totals_label);
        totalsLabel.setText(getTotalsLabelResId());

        mEmptyLabel = (TextView) view.findViewById(R.id.stats_list_empty_text);
        mTotalsLabel = (TextView) view.findViewById(R.id.stats_module_totals_label);
        mList = (LinearLayout) view.findViewById(R.id.stats_list_linearlayout);
        mListContainer = (LinearLayout) view.findViewById(R.id.stats_list_container);
        mViewAll = (Button) view.findViewById(R.id.btnViewAll);
        mTopPagerRadioGroup = (RadioGroup) view.findViewById(R.id.stats_pager_tabs);
        mPaginationContainer = (LinearLayout) view.findViewById(R.id.stats_pagination_container);
        mPaginationGoBackButton = (Button) view.findViewById(R.id.stats_pagination_go_back);
        mPaginationGoForwardButton = (Button) view.findViewById(R.id.stats_pagination_go_forward);
        mPaginationText = (TextView) view.findViewById(R.id.stats_pagination_text);

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
        outState.putSerializable(ARG_REST_RESPONSE, mDatamodels);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onPause() {
        super.onPause();
        //AppLog.d(AppLog.T.STATS, this.getTag() + " > onPause");
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.unregisterReceiver(mReceiver);
    }

    @Override
    public void onResume() {
        //AppLog.d(AppLog.T.STATS, this.getTag() + " > onResume");
        super.onResume();
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(getActivity());
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_STATS_SECTION_UPDATED));

        // Init the UI
        if (mDatamodels != null) {
            updateUI();
        } else {
            //showHideNoResultsUI(true);
            showEmptyUI();
            mMoreDataListener.onRefreshRequested(getSectionToUpdate());
        }
    }

    protected void showEmptyUI() {
        mModuleTitleTextPlaceholderTextView.setVisibility(View.VISIBLE);
        mTopPagerRadioGroup.setVisibility(View.GONE);
        mModuleTitleTextView.setVisibility(View.GONE);
        mEmptyLabel.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);
        mList.setVisibility(View.GONE);
        mViewAll.setVisibility(View.GONE);
        mPaginationContainer.setVisibility(View.GONE);
        return;
    }

    protected void showHideNoResultsUI(boolean showNoResultsUI) {
        mModuleTitleTextPlaceholderTextView.setVisibility(View.GONE);
        mModuleTitleTextView.setVisibility(View.VISIBLE);

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
            mPaginationContainer.setVisibility(View.GONE);
        } else {
            mEmptyLabel.setVisibility(View.GONE);
            mListContainer.setVisibility(View.VISIBLE);
            mList.setVisibility(View.VISIBLE );
            if (!isSingleView() && isViewAllOptionAvailable()) {
                // No view all button if already in single view
                configureViewAllButton();
            } else {
                mViewAll.setVisibility(View.GONE);
            }
        }
    }

    protected void showErrorUI(Serializable error) {
        mGroupIdToExpandedMap.clear();
        mModuleTitleTextPlaceholderTextView.setVisibility(View.GONE);
        mModuleTitleTextView.setVisibility(View.VISIBLE);

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

    protected boolean isErrorResponse(int index) {
        return mDatamodels != null && mDatamodels[index] != null
                && mDatamodels[index] instanceof VolleyError;
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
                if (mTopPagerRadioGroup.getVisibility() == View.VISIBLE) {
                    viewAllIntent.putExtra(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, mTopPagerSelectedButtonIndex);
                }
                //viewAllIntent.putExtra(StatsAbstractFragment.ARG_REST_RESPONSE, mDatamodels[mTopPagerSelectedButtonIndex]);
                getActivity().startActivity(viewAllIntent);
            }
        });
    }

    protected int getMaxNumberOfItemsToShowInList() {
        return isSingleView() ? MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST : 10;
    }

    /*
 * receives broadcast when data has been updated
 */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = StringUtils.notNullStr(intent.getAction());

            if (!action.equals(StatsService.ACTION_STATS_SECTION_UPDATED)) {
                return;
            }

            if (!intent.hasExtra(StatsService.EXTRA_ENDPOINT_NAME)) {
                return;
            }

            if (!isAdded()) {
                return;
            }

            StatsService.StatsEndpointsEnum sectionToUpdate = (StatsService.StatsEndpointsEnum) intent.getSerializableExtra(StatsService.EXTRA_ENDPOINT_NAME);
            StatsService.StatsEndpointsEnum[] sectionsToUpdate = getSectionToUpdate();
            int indexOfDatamodelMatch = -1;
            for (int i = 0; i < getSectionToUpdate().length; i++) {
                if (sectionToUpdate == sectionsToUpdate[i]) {
                    indexOfDatamodelMatch = i;
                    break;
                }
            }

            if (-1 == indexOfDatamodelMatch) {
                return;
            }

            mGroupIdToExpandedMap.clear();
            if (action.equals(StatsService.ACTION_STATS_SECTION_UPDATED)) {
                Serializable dataObj = intent.getSerializableExtra(StatsService.EXTRA_ENDPOINT_DATA);

                if (mDatamodels == null) {
                    mDatamodels = new Serializable[getSectionToUpdate().length];
                }

                //dataObj = (dataObj == null || dataObj instanceof VolleyError) ? null : dataObj;
                mDatamodels[indexOfDatamodelMatch] = dataObj;
                updateUI();
            }

            return;
        }
    };
}