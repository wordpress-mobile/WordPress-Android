package org.wordpress.android.ui;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.ViewDelegate;

public class PullToRefreshHelper implements OnRefreshListener {
    private PullToRefreshHeaderTransformer mHeaderTransformer;
    private PullToRefreshLayout mPullToRefreshLayout;
    private RefreshListener mRefreshListener;

    public PullToRefreshHelper(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener) {
        mRefreshListener = listener;
        mPullToRefreshLayout = pullToRefreshLayout;
        mHeaderTransformer = new PullToRefreshHeaderTransformer();
        ActionBarPullToRefresh.from(activity).options(Options.create().headerTransformer(mHeaderTransformer).build())
                              .allChildrenArePullable().listener(this)
                              .useViewDelegate(TextView.class, new ViewDelegate() {
                                          @Override
                                          public boolean isReadyForPull(View view, float v, float v2) {
                                              return true;
                                          }
                                      }
                              ).setup(mPullToRefreshLayout);
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
