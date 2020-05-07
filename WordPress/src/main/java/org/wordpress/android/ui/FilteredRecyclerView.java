package org.wordpress.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
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

import androidx.annotation.LayoutRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;
import com.google.android.material.elevation.ElevationOverlayProvider;

import org.wordpress.android.R;
import org.wordpress.android.models.FilterCriteria;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.ContextExtensionsKt;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.RecyclerItemDecoration;

import java.util.ArrayList;
import java.util.List;

import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;


public class FilteredRecyclerView extends RelativeLayout {
    private ProgressBar mProgressLoadMore;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private Spinner mSpinner;
    private boolean mSelectingRememberedFilterOnCreate = false;

    private boolean mUseTabsForFiltering = false;

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private View mCustomEmptyView;
    private Toolbar mToolbar;
    private AppBarLayout mAppBarLayout;
    private RecyclerView mSearchSuggestionsRecyclerView;

    private List<FilterCriteria> mFilterCriteriaOptions;
    private FilterCriteria mCurrentFilter;
    private FilterListener mFilterListener;
    private SpinnerAdapter mSpinnerAdapter;
    private RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter;
    private int mSpinnerTextColor;
    private int mSpinnerDrawableRight;
    private AppLog.T mTAG;

    private boolean mToolbarDisableScrollGestures = false;
    @LayoutRes private int mSpinnerItemView = 0;
    @LayoutRes private int mSpinnerDropDownItemView = 0;

    public FilteredRecyclerView(Context context) {
        this(context, null);
    }

    public FilteredRecyclerView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FilteredRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init(context, attrs);
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    public boolean isRefreshing() {
        return mSwipeToRefreshHelper.isRefreshing();
    }

    public void setCurrentFilter(FilterCriteria filter) {
        mCurrentFilter = filter;

        if (!mUseTabsForFiltering) {
            int position = mSpinnerAdapter.getIndexOfCriteria(filter);
            if (position > -1 && position != mSpinner.getSelectedItemPosition()) {
                mSpinner.setSelection(position);
            }
        }
    }

    public FilterCriteria getCurrentFilter() {
        return mCurrentFilter;
    }

    public boolean isValidFilter(FilterCriteria filter) {
        return filter != null
               && mFilterCriteriaOptions != null
               && !mFilterCriteriaOptions.isEmpty()
               && mFilterCriteriaOptions.contains(filter);
    }

    public void setFilterListener(FilterListener filterListener) {
        mFilterListener = filterListener;
        setup(false);
    }

    public void setAdapter(RecyclerView.Adapter<RecyclerView.ViewHolder> adapter) {
        mAdapter = adapter;
        mRecyclerView.setAdapter(mAdapter);
    }

    public RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter() {
        return mAdapter;
    }

    public void setSwipeToRefreshEnabled(boolean enable) {
        mSwipeToRefreshHelper.setEnabled(enable);
    }

    public void setLogT(AppLog.T tag) {
        mTAG = tag;
    }

    public void setCustomEmptyView(View v) {
        mCustomEmptyView = v;
    }

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        inflate(getContext(), R.layout.filtered_list_component, this);

        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.FilteredRecyclerView,
                    0, 0);
            try {
                mToolbarDisableScrollGestures = a.getBoolean(
                        R.styleable.FilteredRecyclerView_wpToolbarDisableScrollGestures, false);
                mSpinnerItemView = a.getResourceId(R.styleable.FilteredRecyclerView_wpSpinnerItemView, 0);
                mSpinnerDropDownItemView = a.getResourceId(
                        R.styleable.FilteredRecyclerView_wpSpinnerDropDownItemView, 0);

                mUseTabsForFiltering = a.getBoolean(
                        R.styleable.FilteredRecyclerView_wpUseTabsForFiltering, false);
            } finally {
                a.recycle();
            }
        }

        int spacingHorizontal = 0;
        int spacingVertical = DisplayUtils.dpToPx(getContext(), 1);
        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

        mToolbar = findViewById(R.id.toolbar_with_spinner);
        mAppBarLayout = findViewById(R.id.app_bar_layout);

        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(getContext());
        float cardElevation = getResources().getDimension(R.dimen.card_elevation);
        int appBarColor =
                elevationOverlayProvider
                        .compositeOverlay(ContextExtensionsKt.getColorFromAttribute(getContext(), R.attr.wpColorAppBar),
                                cardElevation);

        mToolbar.setBackgroundColor(appBarColor);

        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        if (mToolbarDisableScrollGestures) {
            params.setScrollFlags(0);
        } else {
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL
                                  | AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS);
        }

        mSearchSuggestionsRecyclerView = findViewById(R.id.suggestions_recycler_view);

        mEmptyView = findViewById(R.id.empty_view);

        // progress bar that appears when loading more items
        mProgressLoadMore = findViewById(R.id.progress_loading);
        mProgressLoadMore.setVisibility(View.GONE);

        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
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
                                if (mFilterListener != null) {
                                    mFilterListener.onLoadData(true);
                                }
                            }
                        });
                    }
                }
        );

        if (mSpinner == null) {
            mSpinner = findViewById(R.id.filter_spinner);
        }


        if (mUseTabsForFiltering) {
            mSpinner.setVisibility(View.GONE);
        } else {
            mSpinner.setVisibility(View.VISIBLE);
        }
    }

    private void setup(boolean refresh) {
        List<FilterCriteria> criterias = mFilterListener.onLoadFilterCriteriaOptions(refresh);
        if (criterias != null) {
            mFilterCriteriaOptions = criterias;
        }
        if (criterias == null) {
            mFilterListener.onLoadFilterCriteriaOptionsAsync(new FilterCriteriaAsyncLoaderListener() {
                @Override
                public void onFilterCriteriasLoaded(List<FilterCriteria> criteriaList) {
                    if (criteriaList != null) {
                        mFilterCriteriaOptions = new ArrayList<>();
                        mFilterCriteriaOptions.addAll(criteriaList);
                        initFilterAdapter();
                        setCurrentFilter(mFilterListener.onRecallSelection());
                    }
                }
            }, refresh);
        } else {
            initFilterAdapter();
            setCurrentFilter(mFilterListener.onRecallSelection());
        }
    }

    private void manageFilterSelection(int position) {
        if (mSelectingRememberedFilterOnCreate) {
            mSelectingRememberedFilterOnCreate = false;
            return;
        }

        FilterCriteria selectedCriteria;
        if (mUseTabsForFiltering) {
            selectedCriteria = mFilterCriteriaOptions.get(position);
        } else {
            selectedCriteria = (FilterCriteria) mSpinnerAdapter.getItem(position);
        }

        if (mCurrentFilter == selectedCriteria) {
            AppLog.d(mTAG, "The selected STATUS is already active: "
                           + selectedCriteria.getLabel());
            return;
        }

        AppLog.d(mTAG, "NEW STATUS : " + selectedCriteria.getLabel());
        setCurrentFilter(selectedCriteria);
        if (mFilterListener != null) {
            mFilterListener.onFilterSelected(position, selectedCriteria);
            setRefreshing(true);
            mFilterListener.onLoadData(false);
        }
    }

    private void initFilterAdapter() {
        mSelectingRememberedFilterOnCreate = true;

        mSpinnerAdapter = new SpinnerAdapter(getContext(),
                mFilterCriteriaOptions, mSpinnerItemView, mSpinnerDropDownItemView);

        mSpinner.setAdapter(mSpinnerAdapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                manageFilterSelection(position);
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

    public void hideEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    public void updateEmptyView(EmptyViewMessageType emptyViewMessageType) {
        if (mEmptyView == null) return;

        if (!hasAdapter() || mAdapter.getItemCount() == 0) {
            if (mFilterListener != null) {
                if (mCustomEmptyView == null) {
                    String msg = mFilterListener.onShowEmptyViewMessage(emptyViewMessageType);
                    if (msg == null) {
                        msg = getContext().getString(R.string.empty_list_default);
                    }
                    mEmptyView.setText(msg);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
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

    public void setToolbarBackgroundColor(int color) {
        mToolbar.setBackgroundColor(color);
    }

    public void setToolbarSpinnerTextColor(int color) {
        mSpinnerTextColor = color;
    }

    public void setToolbarSpinnerDrawable(int drawableResId) {
        mSpinnerDrawableRight = drawableResId;
    }

    public void setToolbarLeftPadding(int paddingLeft) {
        ViewCompat.setPaddingRelative(mToolbar, paddingLeft, mToolbar.getPaddingTop(),
                ViewCompat.getPaddingEnd(mToolbar), mToolbar.getPaddingBottom());
    }

    public void setToolbarRightPadding(int paddingRight) {
        ViewCompat.setPaddingRelative(mToolbar, ViewCompat.getPaddingStart(mToolbar), mToolbar.getPaddingTop(),
                paddingRight, mToolbar.getPaddingBottom());
    }

    public void setToolbarLeftAndRightPadding(int paddingLeft, int paddingRight) {
        ViewCompat.setPaddingRelative(mToolbar, paddingLeft, mToolbar.getPaddingTop(), paddingRight,
                mToolbar.getPaddingBottom());
    }

    public void setToolbarTitle(@StringRes int title, int startMargin) {
        mToolbar.setTitle(title);
        mToolbar.setTitleMarginStart(startMargin);
    }

    public void setToolbarScrollFlags(int flags) {
        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        params.setScrollFlags(flags);
        mToolbar.setLayoutParams(params);
    }

    public void scrollRecycleViewToPosition(int position) {
        if (mRecyclerView == null) {
            return;
        }

        mRecyclerView.scrollToPosition(position);
    }

    public int getCurrentPosition() {
        if (mRecyclerView != null && mRecyclerView.getLayoutManager() != null) {
            return ((LinearLayoutManager) mRecyclerView.getLayoutManager()).findFirstVisibleItemPosition();
        } else {
            return -1;
        }
    }

    public void smoothScrollToPosition(int position) {
        if (mRecyclerView != null && mRecyclerView.getLayoutManager() != null) {
            mRecyclerView.getLayoutManager().smoothScrollToPosition(mRecyclerView, null, position);
        }
    }

    public void addItemDecoration(RecyclerView.ItemDecoration decor) {
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

    public AppBarLayout getAppBarLayout() {
        return mAppBarLayout;
    }

    /*
     * use this if you need to reload the criterias for this FilteredRecyclerView. The actual data loading goes
     * through the FilteredRecyclerView lifecycle using its listeners:
     *
     * - FilterCriteriaAsyncLoaderListener
     * and
     * - FilterListener.onLoadFilterCriteriaOptions
     * */
    public void refreshFilterCriteriaOptions() {
        setup(true);
    }

    public void showSearchSuggestions() {
        mSearchSuggestionsRecyclerView.setVisibility(View.VISIBLE);
    }

    public void hideSearchSuggestions() {
        mSearchSuggestionsRecyclerView.setVisibility(View.GONE);
    }

    public void setSearchSuggestionAdapter(RecyclerView.Adapter searchSuggestionAdapter) {
        mSearchSuggestionsRecyclerView.setAdapter(searchSuggestionAdapter);
    }

    /*
     * adapter used by the filter spinner
     */
    private class SpinnerAdapter extends BaseAdapter {
        private final List<FilterCriteria> mFilterValues;
        private final LayoutInflater mInflater;
        @LayoutRes private final int mItemView;
        @LayoutRes private final int mDropDownItemView;

        SpinnerAdapter(Context context,
                       List<FilterCriteria> filterValues,
                       @LayoutRes int itemView,
                       @LayoutRes int dropDownItemView) {
            super();
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mFilterValues = filterValues;

            if (itemView == 0) {
                mItemView = R.layout.filter_spinner_item;
            } else {
                mItemView = itemView;
            }

            if (dropDownItemView == 0) {
                mDropDownItemView = R.layout.toolbar_spinner_dropdown_item;
            } else {
                mDropDownItemView = dropDownItemView;
            }
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
                view = mInflater.inflate(mItemView, parent, false);

                final TextView text = view.findViewById(R.id.text);
                FilterCriteria selectedCriteria = (FilterCriteria) getItem(position);
                text.setText(selectedCriteria.getLabel());
                if (mSpinnerTextColor != 0) {
                    text.setTextColor(mSpinnerTextColor);
                }

                if (mSpinnerDrawableRight != 0) {
                    text.setCompoundDrawablePadding(getResources().getDimensionPixelSize(R.dimen.margin_medium));
                    text.setGravity(Gravity.CENTER_VERTICAL | Gravity.START);
                }
            } else {
                view = convertView;
            }

            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            FilterCriteria selectedCriteria = (FilterCriteria) getItem(position);
            final TagViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(mDropDownItemView, parent, false);
                holder = new TagViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (TagViewHolder) convertView.getTag();
            }

            holder.mTextView.setText(selectedCriteria.getLabel());
            return convertView;
        }

        private class TagViewHolder {
            private final TextView mTextView;

            TagViewHolder(View view) {
                mTextView = view.findViewById(R.id.text);
            }
        }

        public int getIndexOfCriteria(FilterCriteria tm) {
            if (tm != null && mFilterValues != null) {
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
         * Async method below
         */
        List<FilterCriteria> onLoadFilterCriteriaOptions(boolean refresh);

        /**
         * Called upon initialization - you can use this callback to start an asynctask to build an array of
         * FilterCriterias here. Once the AsyncTask is done, it should call the provided listener
         * The Spinner is then loaded with such array of FilterCriterias, through which the main data can be filtered.
         *
         * @param listener to be called to pass the FilterCriteria array when done
         * @param refresh  "true"if the criterias need be refreshed
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
        void onLoadData(boolean forced);

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
    public interface FilterCriteriaAsyncLoaderListener {
        /**
         * Will be called during initialization of FilteredRecyclerView once you're ready building the
         * FilterCriteria array
         *
         * @param criteriaList the array of FilterCriteria objects you just built
         */
        void onFilterCriteriasLoaded(List<FilterCriteria> criteriaList);
    }
}
