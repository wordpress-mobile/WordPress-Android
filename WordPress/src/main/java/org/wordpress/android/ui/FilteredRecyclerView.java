package org.wordpress.android.ui;

import android.content.Context;
import android.support.annotation.MenuRes;
import android.support.design.widget.AppBarLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Spinner;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.models.FilterCriteria;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.RecyclerItemDecoration;

import java.util.ArrayList;
import java.util.List;


public class FilteredRecyclerView extends RelativeLayout {

    private ProgressBar mProgressLoadMore;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private Spinner mSpinner;
    private boolean mSelectingRememberedFilterOnCreate = false;

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private View mCustomEmptyView;
    private Toolbar mToolbar;
    private AppBarLayout mAppBarLayout;

    private List<FilterCriteria> mFilterCriteriaOptions;
    private FilterCriteria mCurrentFilter;
    private FilterListener mFilterListener;
    private SpinnerAdapter mSpinnerAdapter;
    private RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter;
    private int mSpinnerTextColor;
    private int mSpinnerDrawableRight;
    private AppLog.T mTAG;

    public FilteredRecyclerView(Context context) {
        super(context);
        init();
    }

    public FilteredRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public FilteredRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    public boolean isRefreshing(){
        return mSwipeToRefreshHelper.isRefreshing();
    }

    public void setCurrentFilter(FilterCriteria filter) {
        mCurrentFilter = filter;
        int position = mSpinnerAdapter.getIndexOfCriteria(filter);
        if (position > -1 && position != mSpinner.getSelectedItemPosition()) {
            mSpinner.setSelection(position);
        }
    }

    public FilterCriteria getCurrentFilter() {
        return mCurrentFilter;
    }

    public void setFilterListener(FilterListener filterListener){
        mFilterListener = filterListener;
        setup(false);
    }

    public void setAdapter(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter){
        mAdapter = adapter;
        mRecyclerView.setAdapter(mAdapter);
    }

    public RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter(){
        return mAdapter;
    }

    public void setSwipeToRefreshEnabled(boolean enable){
        mSwipeToRefreshHelper.setEnabled(enable);
    }

    public void setLogT(AppLog.T tag){
        mTAG = tag;
    }

    public void setCustomEmptyView(View v){
        mCustomEmptyView = v;
    }

    private void init() {
        inflate(getContext(), R.layout.filtered_list_component, this);

        int spacingHorizontal = 0;
        int spacingVertical = DisplayUtils.dpToPx(getContext(), 1);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        mToolbar = (Toolbar) findViewById(R.id.toolbar_with_spinner);
        mAppBarLayout = (AppBarLayout) findViewById(R.id.app_bar_layout);

        mEmptyView = (TextView) findViewById(R.id.empty_view);

        // progress bar that appears when loading more items
        mProgressLoadMore = (ProgressBar) findViewById(R.id.progress_loading);
        mProgressLoadMore.setVisibility(View.GONE);

        mSwipeToRefreshHelper = new SwipeToRefreshHelper(getContext(),
                (CustomSwipeRefreshLayout) findViewById(R.id.ptr_layout),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        post(new Runnable() {
                            @Override
                            public void run() {
                                if (!NetworkUtils.checkConnection(getContext())) {
                                    mSwipeToRefreshHelper.setRefreshing(false);
                                    updateEmptyView(EmptyViewMessageType.NETWORK_ERROR);
                                    return;
                                }
                                if (mFilterListener != null){
                                    mFilterListener.onLoadData();
                                }
                            }
                        });
                    }
                });


        if (mSpinner == null) {
            mSpinner = (Spinner) findViewById(R.id.filter_spinner);
        }

    }

    private void setup(boolean refresh){
        List<FilterCriteria> criterias = mFilterListener.onLoadFilterCriteriaOptions(refresh);
        if (criterias != null){
            mFilterCriteriaOptions = criterias;
        }
        if (criterias == null){
            mFilterListener.onLoadFilterCriteriaOptionsAsync(new FilterCriteriaAsyncLoaderListener() {
                @Override
                public void onFilterCriteriasLoaded(List<FilterCriteria> criteriaList) {
                    if (criteriaList != null) {
                        mFilterCriteriaOptions = new ArrayList<FilterCriteria>();
                        mFilterCriteriaOptions.addAll(criteriaList);
                        initSpinnerAdapter();
                        setCurrentFilter(mFilterListener.onRecallSelection());
                    }
                }
            }, refresh);
        } else {
            initSpinnerAdapter();
            setCurrentFilter(mFilterListener.onRecallSelection());
        }
    }

    private void initSpinnerAdapter(){
        mSpinnerAdapter = new SpinnerAdapter(getContext(), mFilterCriteriaOptions);

        mSelectingRememberedFilterOnCreate = true;
        mSpinner.setAdapter(mSpinnerAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (mSelectingRememberedFilterOnCreate) {
                    mSelectingRememberedFilterOnCreate = false;
                    return;
                }

                FilterCriteria selectedCriteria =
                        (FilterCriteria) mSpinnerAdapter.getItem(position);

                if (mCurrentFilter == selectedCriteria) {
                    AppLog.d(mTAG, "The selected STATUS is already active: " +
                            selectedCriteria.getLabel());
                    return;
                }

                AppLog.d(mTAG, "NEW STATUS : " + selectedCriteria.getLabel());
                setCurrentFilter(selectedCriteria);
                if (mFilterListener != null) {
                    mFilterListener.onFilterSelected(position, selectedCriteria);
                    setRefreshing(true);
                    mFilterListener.onLoadData();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // nop
            }
        });

    }

    private boolean hasAdapter() {
        return (mAdapter != null);
    }

    public boolean emptyViewIsVisible(){
        return (mEmptyView != null && mEmptyView.getVisibility() == View.VISIBLE);
    }

    public void hideEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    public void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        if (mEmptyView == null) return;

        if ((hasAdapter() && mAdapter.getItemCount() == 0) || !hasAdapter()) {
            if (mFilterListener != null){
                if (mCustomEmptyView == null){
                    String msg = mFilterListener.onShowEmptyViewMessage(emptyViewMessageType);
                    if (msg == null){
                        msg = getContext().getString(R.string.empty_list_default);
                    }
                    mEmptyView.setText(msg);
                    mEmptyView.setVisibility(View.VISIBLE);
                }
                else {
                    mEmptyView.setVisibility(View.GONE);
                    mFilterListener.onShowCustomEmptyView(emptyViewMessageType);
                }
            }

        } else {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    /**
     * show/hide progress bar which appears at the bottom when loading more items
     */
    public void showLoadingProgress() {
        if (mProgressLoadMore != null) {
            mProgressLoadMore.setVisibility(View.VISIBLE);
        }
    }

    public void hideLoadingProgress() {
        if (mProgressLoadMore != null) {
            mProgressLoadMore.setVisibility(View.GONE);
        }
    }

    /*
     * add a menu to the right side of the toolbar, returns the toolbar menu so the caller
     * can act upon it
     */
    public Menu addToolbarMenu(@MenuRes int menuResId) {
        mToolbar.inflateMenu(menuResId);
        return mToolbar.getMenu();
    }

    public void setToolbarBackgroundColor(int color){
        mToolbar.setBackgroundColor(color);
    }

    public void setToolbarSpinnerTextColor(int color){
        mSpinnerTextColor = color;
    }

    public void setToolbarSpinnerDrawable(int drawableResId){
        mSpinnerDrawableRight = drawableResId;
    }

    public void setToolbarLeftPadding(int paddingLeft){
        mToolbar.setPadding(paddingLeft,
                mToolbar.getPaddingTop(),
                mToolbar.getPaddingRight(),
                mToolbar.getPaddingBottom());
    }

    public void setToolbarRightPadding(int paddingRight){
        mToolbar.setPadding(
                mToolbar.getPaddingLeft(),
                mToolbar.getPaddingTop(),
                paddingRight,
                mToolbar.getPaddingBottom());
    }

    public void setToolbarLeftAndRightPadding(int paddingLeft, int paddingRight){
        mToolbar.setPadding(
                paddingLeft,
                mToolbar.getPaddingTop(),
                paddingRight,
                mToolbar.getPaddingBottom());
    }

    public void scrollRecycleViewToPosition(int position) {
        if (mRecyclerView == null) return;

        mRecyclerView.scrollToPosition(position);
    }

    public int getCurrentPosition() {
        if (mRecyclerView != null && mRecyclerView.getLayoutManager() != null) {
            return ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else {
            return -1;
        }
    }

    public void smoothScrollToPosition(int position){
        if (mRecyclerView != null && mRecyclerView.getLayoutManager() != null) {
            mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, position);
        }
    }

    public void addItemDecoration(RecyclerView.ItemDecoration decor){
        if (mRecyclerView == null) return;

        mRecyclerView.addItemDecoration(decor);
    }

    public void addOnScrollListener(RecyclerView.OnScrollListener listener) {
        if (mRecyclerView != null) {
            mRecyclerView.addOnScrollListener(listener);
        }
    }

    public void removeOnScrollListener(RecyclerView.OnScrollListener listener) {
        if (mRecyclerView != null) {
            mRecyclerView.removeOnScrollListener(listener);
        }
    }

    public void hideToolbar(){
        mAppBarLayout.setExpanded(false, true);
    }

    public void showToolbar(){
        mAppBarLayout.setExpanded(true, true);
    }

    /*
    * use this if you need to reload the criterias for this FilteredRecyclerView. The actual data loading goes
    * through the FilteredRecyclerView lifecycle using its listeners:
    *
    * - FilterCriteriaAsyncLoaderListener
    * and
    *  - FilterListener.onLoadFilterCriteriaOptions
    * */
    public void refreshFilterCriteriaOptions(){
        setup(true);
    }

    /*
     * adapter used by the filter spinner
     */
    private class SpinnerAdapter extends BaseAdapter {
        private final List<FilterCriteria> mFilterValues;
        private final LayoutInflater mInflater;

        SpinnerAdapter(Context context, List<FilterCriteria> filterValues) {
            super();
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mFilterValues = filterValues;
        }

        @Override
        public int getCount() {
            return (mFilterValues != null ? mFilterValues.size() : 0);
        }

        @Override
        public Object getItem(int position) {
            return mFilterValues.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.filter_spinner_item, parent, false);

                final TextView text = (TextView) view.findViewById(R.id.text);
                FilterCriteria selectedCriteria = (FilterCriteria)getItem(position);
                text.setText(selectedCriteria.getLabel());
                if (mSpinnerTextColor != 0){
                    text.setTextColor(mSpinnerTextColor);
                }

                if (mSpinnerDrawableRight != 0){
                    text.setCompoundDrawablesWithIntrinsicBounds(0, 0, mSpinnerDrawableRight, 0);
                    text.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.margin_medium));
                    text.setGravity(Gravity.CENTER_VERTICAL | Gravity.LEFT);
                }

            } else {
                view = convertView;
            }

            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            FilterCriteria selectedCriteria = (FilterCriteria)getItem(position);
            final TagViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.toolbar_spinner_dropdown_item, parent, false);
                holder = new TagViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (TagViewHolder) convertView.getTag();
            }

            holder.textView.setText(selectedCriteria.getLabel());
            return convertView;
        }

        private class TagViewHolder {
            private final TextView textView;
            TagViewHolder(View view) {
                textView = (TextView) view.findViewById(R.id.text);
            }
        }

        public int getIndexOfCriteria(FilterCriteria tm) {
            if (tm != null && mFilterValues != null){
                for (int i = 0; i < mFilterValues.size(); i++) {
                    FilterCriteria criteria = mFilterValues.get(i);
                    if (criteria != null && criteria.equals(tm)) {
                        return i;
                    }
                }
            }
            return -1;
        }
    }

    /*
     * returns true if the first item is still visible in the RecyclerView - will return
     * false if the first item is scrolled out of view, or if the list is empty
     */
    public boolean isFirstItemVisible() {
        if (mRecyclerView == null
                || mRecyclerView.getLayoutManager() == null) {
            return false;
        }

        View child = mRecyclerView.getLayoutManager().getChildAt(0);
        return (child != null && mRecyclerView.getLayoutManager().getPosition(child) == 0);
    }


    /**
     * implement this interface to use FilterRecyclerView
     */
    public interface FilterListener {
        /**
         * Called upon initialization - provide an array of FilterCriterias here. These are the possible criterias
         * the Spinner is loaded with, and through which the data can be filtered.
         *
         * @param refresh "true"if the criterias need be refreshed
         * @return an array of FilterCriteria to be used on Spinner initialization, or null if going to use the
         *          Async method below
         */
        List<FilterCriteria> onLoadFilterCriteriaOptions(boolean refresh);

        /**
         * Called upon initialization - you can use this callback to start an asynctask to build an array of
         * FilterCriterias here. Once the AsyncTask is done, it should call the provided listener
         * The Spinner is then loaded with such array of FilterCriterias, through which the main data can be filtered.
         *
         * @param listener to be called to pass the FilterCriteria array when done
         * @param refresh "true"if the criterias need be refreshed
         */
        void onLoadFilterCriteriaOptionsAsync(FilterCriteriaAsyncLoaderListener listener, boolean refresh);

        /**
         * Called upon initialization, right after onLoadFilterCriteriaOptions().
         * Once the criteria options are set up, use this callback to return the latest option selected on the
         * screen the last time the user visited it, or a default value for the filter Spinner to be initialized with.
         *
         * @return
         */
        FilterCriteria onRecallSelection();

        /**
         * When this method is called, you should load data into the FilteredRecyclerView adapter, using the
         * latest criteria passed to you in a previous onFilterSelected() call.
         * Within the FilteredRecyclerView lifecycle, this is triggered in three different moments:
         * 1 - upon initialisation
         * 2 - each time a screen refresh is requested
         * 3 - each time the user changes the filter spinner selection
         */
        void onLoadData();

        /**
         * Called each time the user changes the Spinner selection (i.e. changes the criteria on which to filter
         * the data). You should only take note of the change, and remember it, as a request to load data with
         * the newly selected filter shall always arrive through onLoadData().
         * The parameters passed in this callback can be used alternatively as per your convenience.
         *
         * @param position of the selected criteria within the array returned by onLoadFilterCriteriaOptions()
         * @param criteria the actual criteria selected
         */
        void onFilterSelected(int position, FilterCriteria criteria);

        /**
         * Called when there's no data to show.
         *
         * @param emptyViewMsgType this will hint you on the reason why no data is being shown, so you can return
         *                         a proper message to be displayed to the user
         * @return the message to be displayed to the user, or null if using a Custom Empty View (see below)
         */
        String onShowEmptyViewMessage(EmptyViewMessageType emptyViewMsgType);

        /**
         * Called when there's no data to show, and only if a custom EmptyView is set (onShowEmptyViewMessage will
         * be called otherwise).
         *
         * @param emptyViewMsgType this will hint you on the reason why no data is being shown, and
         *                         also here you should perform any actions on your custom empty view
         * @return nothing
         */
        void onShowCustomEmptyView(EmptyViewMessageType emptyViewMsgType);

    }

    /**
     * implement this interface to load filtering options (that is, an array of FilterCriteria) asynchronously
     */
    public interface FilterCriteriaAsyncLoaderListener{
        /**
         * Will be called during initialization of FilteredRecyclerView once you're ready building the FilterCriteria array
         *
         * @param criteriaList the array of FilterCriteria objects you just built
         */
        void onFilterCriteriasLoaded(List<FilterCriteria> criteriaList);
    }

}
