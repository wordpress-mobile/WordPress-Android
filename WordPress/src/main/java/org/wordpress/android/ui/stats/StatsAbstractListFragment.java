package org.wordpress.android.ui.stats;

import android.content.Intent;
import android.os.Bundle;
import android.text.Html;
import android.text.TextUtils;
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

import org.wordpress.android.R;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.widgets.TypefaceCache;

public abstract class StatsAbstractListFragment extends StatsAbstractFragment {

    // Used when the fragment has 2 pages/kind of stats in it. Not meaning the bottom pagination.
    static final String ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX = "ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX";
    private static final String ARGS_EXPANDED_ROWS = "ARGS_EXPANDED_ROWS";
    private static final int MAX_NUM_OF_ITEMS_DISPLAYED_IN_SINGLE_VIEW_LIST = 1000;
    static final int MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST = 10;

    private static final int NO_STRING_ID = -1;

    private TextView mModuleTitleTextView;
    private TextView mEmptyLabel;
    TextView mTotalsLabel;
    private LinearLayout mListContainer;
    LinearLayout mList;
    private Button mViewAll;

    LinearLayout mTopPagerContainer;
    int mTopPagerSelectedButtonIndex = 0;

    // Bottom and Top Pagination for modules that has pagination enabled.
    LinearLayout mBottomPaginationContainer;
    Button mBottomPaginationGoBackButton;
    Button mBottomPaginationGoForwardButton;
    TextView mBottomPaginationText;
    LinearLayout mTopPaginationContainer;
    Button mTopPaginationGoBackButton;
    Button mTopPaginationGoForwardButton;
    TextView mTopPaginationText;

    private LinearLayout mEmptyModulePlaceholder;

    SparseBooleanArray mGroupIdToExpandedMap;

    protected abstract int getEntryLabelResId();
    protected abstract int getTotalsLabelResId();
    protected abstract int getEmptyLabelTitleResId();
    protected abstract int getEmptyLabelDescResId();
    protected abstract boolean isExpandableList();
    protected abstract boolean isViewAllOptionAvailable();

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
        mGroupIdToExpandedMap = new SparseBooleanArray();
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(ARGS_EXPANDED_ROWS)) {
                mGroupIdToExpandedMap = savedInstanceState.getParcelable(ARGS_EXPANDED_ROWS);
            }
            mTopPagerSelectedButtonIndex = savedInstanceState.getInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        if (mGroupIdToExpandedMap.size() > 0) {
            outState.putParcelable(ARGS_EXPANDED_ROWS, new SparseBooleanArrayParcelable(mGroupIdToExpandedMap));
        }
        outState.putInt(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, mTopPagerSelectedButtonIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void showPlaceholderUI() {
        mTopPagerContainer.setVisibility(View.GONE);
        mEmptyLabel.setVisibility(View.GONE);
        mListContainer.setVisibility(View.GONE);
        mList.setVisibility(View.GONE);
        mViewAll.setVisibility(View.GONE);
        mBottomPaginationContainer.setVisibility(View.GONE);
        mTopPaginationContainer.setVisibility(View.GONE);
        mEmptyModulePlaceholder.setVisibility(View.VISIBLE);
    }

    void showHideNoResultsUI(boolean showNoResultsUI) {
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

    @Override
    protected void showErrorUI(String label) {
        if (!isAdded()) {
            return;
        }

        mGroupIdToExpandedMap.clear();
        mModuleTitleTextView.setVisibility(View.VISIBLE);
        mEmptyModulePlaceholder.setVisibility(View.GONE);

        // Use the generic error message when the string passed to this method is null.
        if (TextUtils.isEmpty(label)) {
            label = "<b>" + getString(R.string.error_refresh_stats) + "</b>";
        }

        if (label.contains("<")) {
            mEmptyLabel.setText(Html.fromHtml(label));
        } else {
            mEmptyLabel.setText(label);
        }
        mEmptyLabel.setVisibility(View.VISIBLE);
        mListContainer.setVisibility(View.GONE);
        mList.setVisibility(View.GONE);
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

                if (!hasDataAvailable()) {
                    return;
                }

                Intent viewAllIntent = new Intent(getActivity(), StatsViewAllActivity.class);
                viewAllIntent.putExtra(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, getLocalTableBlogID());
                viewAllIntent.putExtra(StatsAbstractFragment.ARGS_TIMEFRAME, getTimeframe());
                viewAllIntent.putExtra(StatsAbstractFragment.ARGS_VIEW_TYPE, getViewType());
                viewAllIntent.putExtra(StatsAbstractFragment.ARGS_SELECTED_DATE, getDate());
                viewAllIntent.putExtra(ARGS_IS_SINGLE_VIEW, true);
                if (mTopPagerContainer.getVisibility() == View.VISIBLE) {
                    viewAllIntent.putExtra(ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, mTopPagerSelectedButtonIndex);
                }
                //viewAllIntent.putExtra(StatsAbstractFragment.ARG_REST_RESPONSE, mDatamodels[mTopPagerSelectedButtonIndex]);
                getActivity().startActivity(viewAllIntent);
            }
        });
    }

    int getMaxNumberOfItemsToShowInList() {
        return isSingleView() ? MAX_NUM_OF_ITEMS_DISPLAYED_IN_SINGLE_VIEW_LIST : MAX_NUM_OF_ITEMS_DISPLAYED_IN_LIST;
    }

    void setupTopModulePager(LayoutInflater inflater, ViewGroup container, View view, String[] buttonTitles) {
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

    private final View.OnClickListener TopModulePagerOnClickListener = new View.OnClickListener() {
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
            if (entryLabel != null) {
                entryLabel.setText(getEntryLabelResId());
            }
            updateUI();
        }
    };
}
