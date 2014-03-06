package org.wordpress.android.ui;

import android.app.Activity;
import android.view.View;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;

public class PullToRefreshHelper implements OnRefreshListener {
    private PullToRefreshHeaderTransformer mHeaderTransformer;
    private PullToRefreshLayout mPullToRefreshLayout;
    private RefreshListener mRefreshListener;

    public PullToRefreshHelper(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener) {
        mRefreshListener = listener;
        mPullToRefreshLayout = pullToRefreshLayout;
        mHeaderTransformer = new PullToRefreshHeaderTransformer();
        ActionBarPullToRefresh.from(activity).options(Options.create().headerTransformer(mHeaderTransformer).build())
                              .allChildrenArePullable().listener(this).setup(mPullToRefreshLayout);
    }

    public void setRefreshing(boolean refreshing) {
        mHeaderTransformer.setShowProgressBarOnly(refreshing);
        mPullToRefreshLayout.setRefreshing(refreshing);
    }

    @Override
    public void onRefreshStarted(View view) {
        mRefreshListener.onRefreshStarted(view);
    }

    public interface RefreshListener {
        public void onRefreshStarted(View view);
    }
}
