package org.wordpress.android.ui;

import android.app.Activity;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.AppLog;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Screen navigator used when the app is opened from an Android Shortcut.
 */
@Singleton
public class ShortcutsNavigator {
    public static final String ACTION_OPEN_SHORTCUT =
            "org.wordpress.android.ui.ShortcutsNavigator.ACTION_OPEN_SHORTCUT";
    private static final String OPEN_STATS = "org.wordpress.android.ui.ShortcutsNavigator.OPEN_STATS";
    private static final String CREATE_NEW_POST = "org.wordpress.android.ui.ShortcutsNavigator.CREATE_NEW_POST";
    private static final String OPEN_NOTIFICATIONS = "org.wordpress.android.ui.ShortcutsNavigator.OPEN_NOTIFICATIONS";

    @Inject ShortcutsNavigator() {
    }

    public void showTargetScreen(String action, Activity activity, SiteModel currentSite) {
        switch (action) {
            case OPEN_STATS:
                AnalyticsTracker.track(AnalyticsTracker.Stat.SHORTCUT_STATS_CLICKED);
                ActivityLauncher.viewBlogStats(activity, currentSite);
                break;
            case CREATE_NEW_POST:
                AnalyticsTracker.track(AnalyticsTracker.Stat.SHORTCUT_NEW_POST_CLICKED);
                ActivityLauncher.addNewPostOrPageForResult(activity, currentSite, false, false);
                break;
            case OPEN_NOTIFICATIONS:
                AnalyticsTracker.track(AnalyticsTracker.Stat.SHORTCUT_NOTIFICATIONS_CLICKED);
                ActivityLauncher.viewNotifications(activity);
                break;
            default:
                AppLog.e(AppLog.T.MAIN, String.format("Unknown Android Shortcut action[%s]", action));
        }
    }
}
