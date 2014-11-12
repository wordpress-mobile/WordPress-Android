package org.wordpress.android.util.ptr;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.TypedArray;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener;
import android.util.TypedValue;

import org.wordpress.android.util.R;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;

import java.lang.ref.WeakReference;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class PullToRefreshHelper implements OnRefreshListener {
    public static final String BROADCAST_ACTION_REFRESH_MENU_PRESSED = "REFRESH_MENU_PRESSED";
    private static final String REFRESH_BUTTON_HIT_COUNT = "REFRESH_BUTTON_HIT_COUNT";
    private static final Set<Integer> TOAST_FREQUENCY = new HashSet<Integer>(Arrays.asList(1, 5, 10, 20, 40, 80, 160,
            320, 640));
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private RefreshListener mRefreshListener;
    private WeakReference<Activity> mActivityRef;

    public interface RefreshListener {
        public void onRefreshStarted();
    }

    public PullToRefreshHelper(Activity activity, SwipeRefreshLayout swipeRefreshLayout, RefreshListener listener) {
        init(activity, swipeRefreshLayout, listener);
    }

    public void init(Activity activity, SwipeRefreshLayout swipeRefreshLayout, RefreshListener listener) {
        mActivityRef = new WeakReference<Activity>(activity);
        mRefreshListener = listener;
        mSwipeRefreshLayout = swipeRefreshLayout;
        mSwipeRefreshLayout.setOnRefreshListener(this);
        final TypedArray styleAttrs = obtainStyledAttrsFromThemeAttr(activity, R.attr.swipeToRefreshStyle,
                R.styleable.RefreshIndicator);
        int color = styleAttrs.getColor(R.styleable.RefreshIndicator_refreshIndicatorColor,
                android.R.color.holo_blue_dark);
        mSwipeRefreshLayout.setColorSchemeColors(color, color, color, color);
    }

    public void setRefreshing(boolean refreshing) {
        mSwipeRefreshLayout.setRefreshing(refreshing);
    }

    public boolean isRefreshing() {
        return mSwipeRefreshLayout.isRefreshing();
    }

    @Override
    public void onRefresh() {
        mRefreshListener.onRefreshStarted();
    }

    public void setEnabled(boolean enabled) {
        mSwipeRefreshLayout.setEnabled(enabled);
    }

    public void refreshAction() {
        Activity activity = mActivityRef.get();
        if (activity == null) {
            return;
        }
        setRefreshing(true);
        mRefreshListener.onRefreshStarted();
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(activity);
        int refreshHits = preferences.getInt(REFRESH_BUTTON_HIT_COUNT, 0);
        refreshHits += 1;
        if (TOAST_FREQUENCY.contains(refreshHits)) {
            ToastUtils.showToast(activity, R.string.ptr_tip_message, Duration.LONG);
        }
        Editor editor = preferences.edit();
        editor.putInt(REFRESH_BUTTON_HIT_COUNT, refreshHits);
        editor.apply();
    }

    public void registerReceiver(Context context) {
        if (context == null) {
            return;
        }
        IntentFilter filter = new IntentFilter();
        filter.addAction(BROADCAST_ACTION_REFRESH_MENU_PRESSED);
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
        lbm.registerReceiver(mReceiver, filter);
    }

    public void unregisterReceiver(Context context) {
        if (context == null) {
            return;
        }
        try {
            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(context);
            lbm.unregisterReceiver(mReceiver);
        } catch (IllegalArgumentException e) {
            // exception occurs if receiver already unregistered (safe to ignore)
        }
    }

    public static TypedArray obtainStyledAttrsFromThemeAttr(Context context, int themeAttr, int[] styleAttrs) {
        TypedValue outValue = new TypedValue();
        context.getTheme().resolveAttribute(themeAttr, outValue, true);
        int styleResId = outValue.resourceId;
        return context.obtainStyledAttributes(styleResId, styleAttrs);
    }

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                return;
            }
            if (intent.getAction().equals(BROADCAST_ACTION_REFRESH_MENU_PRESSED)) {
                refreshAction();
            }
        }
    };
}
