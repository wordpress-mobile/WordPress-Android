package org.wordpress.android;

import android.app.Activity;
import android.app.Application;
import android.content.ComponentCallbacks2;
import android.content.res.Configuration;
import android.os.Bundle;

import com.google.android.gcm.GCMRegistrar;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DateTimeUtils;
import org.wordpress.android.util.NetworkUtils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Detect when the app goes to the background and come back to the foreground.
 *
 * Turns out that when your app has no more visible UI, a callback is triggered.
 * The callback, implemented in this custom class, is called ComponentCallbacks2 (yes, with a two).
 *
 * This class also uses ActivityLifecycleCallbacks and a timer used as guard,
 * to make sure to detect the send to background event and not other events.
 *
 */
public class ApplicationLifecycleMonitor implements Application.ActivityLifecycleCallbacks, ComponentCallbacks2 {
    private final int DEFAULT_TIMEOUT = 2 * 60; // 2 minutes
    private Date lastPingDate;
    private Date mApplicationOpenedDate;
    boolean isInBackground = true;

    @Override
    public void onConfigurationChanged(final Configuration newConfig) {
    }

    @Override
    public void onLowMemory() {
    }

    @Override
    public void onTrimMemory(final int level) {
        if (level == ComponentCallbacks2.TRIM_MEMORY_UI_HIDDEN) {
            // We're in the Background
            isInBackground = true;
            String lastActivityString = AppPrefs.getLastActivityStr();
            ActivityId lastActivity = ActivityId.getActivityIdFromName(lastActivityString);
            Map<String, Object> properties = new HashMap<String, Object>();
            properties.put("last_visible_screen", lastActivity.toString());
            if (mApplicationOpenedDate != null) {
                Date now = new Date();
                properties.put("time_in_app", DateTimeUtils.secondsBetween(now, mApplicationOpenedDate));
                mApplicationOpenedDate = null;
            }
            AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_CLOSED, properties);
            AnalyticsTracker.endSession(false);
        } else {
            isInBackground = false;
        }

        boolean evictBitmaps = false;
        switch (level) {
            case TRIM_MEMORY_COMPLETE:
            case TRIM_MEMORY_MODERATE:
            case TRIM_MEMORY_RUNNING_MODERATE:
            case TRIM_MEMORY_RUNNING_CRITICAL:
            case TRIM_MEMORY_RUNNING_LOW:
                evictBitmaps = true;
                break;
            default:
                break;
        }

        if (evictBitmaps && WordPress.getBitmapCache() != null) {
            WordPress.getBitmapCache().evictAll();
        }
    }

    private boolean isPushNotificationPingNeeded() {
        if (lastPingDate == null) {
            // first startup
            return false;
        }

        Date now = new Date();
        if (DateTimeUtils.secondsBetween(now, lastPingDate) >= DEFAULT_TIMEOUT) {
            lastPingDate = now;
            return true;
        }
        return false;
    }

    /**
     * Check if user has valid credentials, and that at least 2 minutes are passed
     * since the last ping, then try to update the PN token.
     */
    private void updatePushNotificationTokenIfNotLimited() {
        // Synch Push Notifications settings
        if (isPushNotificationPingNeeded() && WordPress.hasDotComToken(WordPress.getContext())) {
            String token = null;
            try {
                // Register for Google Cloud Messaging
                GCMRegistrar.checkDevice(WordPress.getContext());
                GCMRegistrar.checkManifest(WordPress.getContext());
                token = GCMRegistrar.getRegistrationId(WordPress.getContext());
                String gcmId = BuildConfig.GCM_ID;
                if (gcmId == null || token == null || token.equals("") ) {
                    AppLog.e(AppLog.T.NOTIFS, "Could not ping the PNs backend, Token or gmcID not found");
                } else {
                    // Send the token to WP.com
                    NotificationsUtils.registerDeviceForPushNotifications(WordPress.getContext(), token);
                }
            } catch (Exception e) {
                AppLog.e(AppLog.T.NOTIFS, "Could not ping the PNs backend: " + e.getMessage());
            }
        }
    }

    /**
     * This method is called when:
     * 1. the app starts (but it's not opened by a service, i.e. an activity is resumed)
     * 2. the app was in background and is now foreground
     */
    public void onFromBackground() {
        AnalyticsTracker.beginSession();
        mApplicationOpenedDate = new Date();
        AnalyticsTracker.track(AnalyticsTracker.Stat.APPLICATION_OPENED);
        if (NetworkUtils.isNetworkAvailable(WordPress.getContext())) {
            // Rate limited PN Token Update
            updatePushNotificationTokenIfNotLimited();

            if (WordPress.hasDotComToken(WordPress.getContext())) {
                // Rate limited WPCom blog list Update
                WordPress.sUpdateWordPressComBlogList.runIfNotLimited();
            }

            // Rate limited blog options Update
            WordPress.sUpdateCurrentBlogOption.runIfNotLimited();
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        if (isInBackground) {
            // was in background before
            onFromBackground();
        }
        isInBackground = false;
    }

    @Override
    public void onActivityCreated(Activity arg0, Bundle arg1) {
    }

    @Override
    public void onActivityDestroyed(Activity arg0) {
    }

    @Override
    public void onActivityPaused(Activity arg0) {
        lastPingDate = new Date();
    }

    @Override
    public void onActivitySaveInstanceState(Activity arg0, Bundle arg1) {
    }

    @Override
    public void onActivityStarted(Activity arg0) {
    }

    @Override
    public void onActivityStopped(Activity arg0) {
    }
}
