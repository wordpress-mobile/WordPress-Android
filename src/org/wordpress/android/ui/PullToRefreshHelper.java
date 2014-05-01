package org.wordpress.android.ui;

import android.app.Activity;
import android.view.View;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh.SetupWizard;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.ViewDelegate;

public class PullToRefreshHelper implements OnRefreshListener {
    private PullToRefreshHeaderTransformer mHeaderTransformer;
    private PullToRefreshLayout mPullToRefreshLayout;
    private RefreshListener mRefreshListener;

    public PullToRefreshHelper(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener) {
        init(activity, pullToRefreshLayout, listener, null);
    }

    public PullToRefreshHelper(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener,
                               java.lang.Class<?> viewClass) {
        init(activity, pullToRefreshLayout, listener, viewClass);
    }

    public void init(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener,
                     java.lang.Class<?> viewClass) {
        mRefreshListener = listener;
        mPullToRefreshLayout = pullToRefreshLayout;
        mHeaderTransformer = new PullToRefreshHeaderTransformer();
        SetupWizard setupWizard = ActionBarPullToRefresh.from(activity).options(Options.create().headerTransformer(
                mHeaderTransformer).build()).allChildrenArePullable().listener(this);
        if (viewClass != null) {
            setupWizard.useViewDelegate(viewClass, new ViewDelegate() {
                        @Override
                        public boolean isReadyForPull(View view, float v, float v2) {
                            return true;
                        }
                    }
            );
        }
        setupWizard.setup(mPullToRefreshLayout);
    }

    public void setRefreshing(boolean refreshing) {
        mHeaderTransformer.setShowProgressBarOnly(refreshing);
        mPullToRefreshLayout.setRefreshing(refreshing);
    }

    public boolean isRefreshing() {
        return mPullToRefreshLayout.isRefreshing();
    }

    @Override
    public void onRefreshStarted(View view) {
        mRefreshListener.onRefreshStarted(view);
    }

    public interface RefreshListener {
        public void onRefreshStarted(View view);
    }

    public void setEnabled(boolean enabled) {
        mPullToRefreshLayout.setEnabled(enabled);
    }

    public void refreshAction() {
        mRefreshListener.onRefreshStarted(mPullToRefreshLayout);
        // TODO: show a toast if 5/10/20/50 action triggered
    }
}
