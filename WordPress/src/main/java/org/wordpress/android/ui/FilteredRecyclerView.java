package org.wordpress.android.ui;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.Menu;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.DrawableRes;
import androidx.annotation.MenuRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.widget.Toolbar;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.AppBarLayout;

import org.wordpress.android.R;
import org.wordpress.android.ui.utils.RecyclerViewExtensionsKt;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.RecyclerItemDecoration;

import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;


public class FilteredRecyclerView extends RelativeLayout {
    private ProgressBar mProgressLoadMore;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    private boolean mHideAppBarLayout = false;

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private Toolbar mToolbar;
    private AppBarLayout mAppBarLayout;
    private RecyclerView mSearchSuggestionsRecyclerView;

    private FilterListener mFilterListener;
    private RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter;

    private boolean mToolbarDisableScrollGestures = false;

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

    public RecyclerView getInternalRecyclerView() {
        return mRecyclerView;
    }

    public RecyclerView getSearchSuggestionsRecyclerView() {
        return mSearchSuggestionsRecyclerView;
    }


    public void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    public boolean isRefreshing() {
        return mSwipeToRefreshHelper.isRefreshing();
    }

    public void setFilterListener(FilterListener filterListener) {
        mFilterListener = filterListener;
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

    private void init(@NonNull Context context, @Nullable AttributeSet attrs) {
        inflate(getContext(), R.layout.filtered_list_component, this);

        boolean includeDefaultSpacing = true;
        if (attrs != null) {
            TypedArray a = context.getTheme().obtainStyledAttributes(
                    attrs,
                    R.styleable.FilteredRecyclerView,
                    0, 0);
            try {
                mToolbarDisableScrollGestures = a.getBoolean(
                        R.styleable.FilteredRecyclerView_wpToolbarDisableScrollGestures, false);

                mHideAppBarLayout = a.getBoolean(
                        R.styleable.FilteredRecyclerView_wpHideAppBarLayout, false);

                includeDefaultSpacing = a.getBoolean(R.styleable.FilteredRecyclerView_wpIncludeDefaultSpacing, true);
            } finally {
                a.recycle();
            }
        }

        mRecyclerView = findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        if (includeDefaultSpacing) {
            int spacingHorizontal = 0;
            int spacingVertical = DisplayUtils.dpToPx(getContext(), 1);
            mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));
        }

        mToolbar = findViewById(R.id.toolbar_with_spinner);
        mAppBarLayout = findViewById(R.id.app_bar_layout);
        AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) mToolbar.getLayoutParams();
        if (mToolbarDisableScrollGestures) {
            params.setScrollFlags(0);
        } else {
            params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
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
                                    showEmptyViewIfEmpty();
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

        mAppBarLayout.setVisibility(mHideAppBarLayout ? View.GONE : View.VISIBLE);
    }

    private boolean hasAdapter() {
        return (mAdapter != null);
    }

    public void hideEmptyView() {
        if (mEmptyView != null) {
            mEmptyView.setVisibility(View.GONE);
        }
    }

    /*
     * shows the default "empty list" message when there's nothing in the list
     */
    private void showEmptyViewIfEmpty() {
        if (mEmptyView == null) return;

        boolean isEmpty = !hasAdapter() || mAdapter.getItemCount() == 0;
        mEmptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
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

    public void addItemDivider(@DrawableRes int dividerRes) {
        if (mRecyclerView == null) return;

        RecyclerViewExtensionsKt.addItemDivider(mRecyclerView, dividerRes);
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

    public void showSearchSuggestions() {
        mSearchSuggestionsRecyclerView.setVisibility(View.VISIBLE);
    }

    public void hideSearchSuggestions() {
        mSearchSuggestionsRecyclerView.setVisibility(View.GONE);
    }

    public void setSearchSuggestionAdapter(RecyclerView.Adapter searchSuggestionAdapter) {
        mSearchSuggestionsRecyclerView.setAdapter(searchSuggestionAdapter);
    }

    public void hideAppBarLayout() {
        mAppBarLayout.setVisibility(GONE);
    }

    public void showAppBarLayout() {
        mAppBarLayout.setVisibility(VISIBLE);
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
         * Called when the user pulls to refresh, after the connection check passes.
         */
        void onLoadData(boolean forced);
    }
}
