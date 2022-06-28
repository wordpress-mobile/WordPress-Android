package org.wordpress.android.ui;

import android.app.Activity;

import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.util.AppLog;

import javax.inject.Inject;

/**
 * Screen navigator used when the app is opened from an Android Shortcut.
 */
public class ShortcutsNavigator {
    public static final String ACTION_OPEN_SHORTCUT =
            "org.wordpress.android.ui.ShortcutsNavigator.ACTION_OPEN_SHORTCUT";

    @Inject ShortcutsNavigator() {
    }

    public void showTargetScreen(String action, Activity activity, SiteModel currentSite) {
        Shortcut shortcut = Shortcut.fromActionString(action);
        if (shortcut == null) {
            AppLog.e(AppLog.T.MAIN, String.format("Unknown Android Shortcut action[%s]", action));
            return;
        }

        switch (shortcut) {
            case OPEN_STATS:
                AnalyticsTracker.track(AnalyticsTracker.Stat.SHORTCUT_STATS_CLICKED);
                ActivityLauncher.viewBlogStats(activity, currentSite);
                break;
            case CREATE_NEW_POST:
                AnalyticsTracker.track(AnalyticsTracker.Stat.SHORTCUT_NEW_POST_CLICKED);
                ActivityLauncher.addNewPostForResult(
                        activity,
                        currentSite,
                        false,
                        PagePostCreationSourcesDetail.POST_FROM_SHORTCUT,
                        -1,
                        null
                );
                break;
            case OPEN_NOTIFICATIONS:
                AnalyticsTracker.track(AnalyticsTracker.Stat.SHORTCUT_NOTIFICATIONS_CLICKED);
                ActivityLauncher.viewNotifications(activity);
                break;
            default:
                AppLog.e(AppLog.T.MAIN, String.format("Unknown Android Shortcut[%s]", shortcut));
        }
    }
}
