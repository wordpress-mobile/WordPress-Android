package org.wordpress.android.ui;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.preference.PreferenceManager;
import android.view.View;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh;
import uk.co.senab.actionbarpulltorefresh.library.ActionBarPullToRefresh.SetupWizard;
import uk.co.senab.actionbarpulltorefresh.library.Options;
import uk.co.senab.actionbarpulltorefresh.library.listeners.OnRefreshListener;
import uk.co.senab.actionbarpulltorefresh.library.viewdelegates.ViewDelegate;

public class PullToRefreshHelper implements OnRefreshListener {
    private static final String REFRESH_BUTTON_HIT_COUNT = "REFRESH_BUTTON_HIT_COUNT";
    private static final Set<Integer> TOAST_FREQUENCY = new HashSet<Integer>(Arrays.asList(1, 5, 10, 20, 40, 80, 160,
            320, 640));
    private PullToRefreshHeaderTransformer mHeaderTransformer;
    private PullToRefreshLayout mPullToRefreshLayout;
    private RefreshListener mRefreshListener;
    private WeakReference<Activity> mActivityRef;

    public PullToRefreshHelper(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener) {
        init(activity, pullToRefreshLayout, listener, null);
    }

    public PullToRefreshHelper(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener,
                               java.lang.Class<?> viewClass) {
        init(activity, pullToRefreshLayout, listener, viewClass);
    }

    public void init(Activity activity, PullToRefreshLayout pullToRefreshLayout, RefreshListener listener,
                     java.lang.Class<?> viewClass) {
        mActivityRef = new WeakReference<Activity>(activity);
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
        Activity activity = mActivityRef.get();
        if (activity == null) {
            return;
        }
        setRefreshing(true);
        mRefreshListener.onRefreshStarted(mPullToRefreshLayout);
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        int refreshHits = preferences.getInt(REFRESH_BUTTON_HIT_COUNT, 0);
        refreshHits += 1;
        if (TOAST_FREQUENCY.contains(refreshHits)) {
            ToastUtils.showToast(activity, R.string.ptr_tip_message, Duration.LONG);
        }
        Editor editor = preferences.edit();
        editor.putInt(REFRESH_BUTTON_HIT_COUNT, refreshHits);
        editor.commit();
    }

    public void registerReceiver(Context context) {
        if (context == null) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(WordPress.BROADCAST_ACTION_REFRESH_MENU_PRESSED);
        context.registerReceiver(mReceiver, filter);
    }

    public void unregisterReceiver(Context context) {
        if (context == null) {
            return;
        }
        try {
            context.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            // exception occurs if receiver already unregistered (safe to ignore)
        }
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (intent.getAction().equals(WordPress.BROADCAST_ACTION_REFRESH_MENU_PRESSED)) {
                refreshAction();
            }
        }
    };
}
