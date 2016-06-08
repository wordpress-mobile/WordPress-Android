package org.wordpress.android.ui.menus.views;

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.EmptyViewMessageType;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.wordpress.android.widgets.RecyclerItemDecoration;


public class MenuItemsRecyclerView extends RelativeLayout {

    private ProgressBar mProgressLoadMore;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;

    private RecyclerView mRecyclerView;
    private TextView mEmptyView;
    private View mCustomEmptyView;

    private RecyclerView.Adapter<RecyclerView.ViewHolder> mAdapter;
    private AppLog.T mTAG;

    public MenuItemsRecyclerView(Context context) {
        super(context);
        init();
    }

    public MenuItemsRecyclerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MenuItemsRecyclerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeToRefreshHelper.setRefreshing(refreshing);
    }

    public boolean isRefreshing(){
        return mSwipeToRefreshHelper.isRefreshing();
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
        inflate(getContext(), R.layout.menu_items_recyclerview_component, this);

        int spacingHorizontal = 0;
        int spacingVertical = DisplayUtils.dpToPx(getContext(), 1);
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        mRecyclerView.addItemDecoration(new RecyclerItemDecoration(spacingHorizontal, spacingVertical));

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


    }

    private void setup(boolean refresh){
        // TODO SETUP adapter? including adapter in this component
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
            mEmptyView.setVisibility(View.VISIBLE);
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

}
