package org.wordpress.android.ui;

import android.app.Activity;

import org.wordpress.android.fluxc.model.SiteModel;

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

    @Inject
    ShortcutsNavigator() {
    }

    public void showTargetScreen(String action, Activity activity, SiteModel currentSite) {
        switch (action) {
            case OPEN_STATS:
                ActivityLauncher.viewBlogStats(activity, currentSite);
                break;
            case CREATE_NEW_POST:
                ActivityLauncher.addNewPostOrPageForResult(activity, currentSite, false, false);
                break;
            case OPEN_NOTIFICATIONS:
                ActivityLauncher.viewNotifications(activity);
                break;
        }
    }
}
