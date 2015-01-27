package org.wordpress.android.ui.notifications;

import android.support.v7.app.ActionBar;
import android.os.Bundle;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.WPDrawerActivity;
import org.wordpress.android.util.AppLog;

public class NotificationsActivity extends WPDrawerActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        AppLog.i(AppLog.T.NOTIFS, "Creating NotificationsActivity");

        createMenuDrawer(R.layout.notifications_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setTitle(getResources().getString(R.string.notifications));
        }

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.notifications_container, new NotificationsListFragment())
                    .commit();

            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED);
        }

        GCMIntentService.clearNotificationsMap();
    }
}
