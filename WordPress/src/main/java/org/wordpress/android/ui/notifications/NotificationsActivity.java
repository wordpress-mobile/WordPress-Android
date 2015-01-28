package org.wordpress.android.ui.notifications;

import android.content.Intent;
import android.support.v7.app.ActionBar;
import android.os.Bundle;

import org.wordpress.android.GCMIntentService;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.WPDrawerActivity;
import org.wordpress.android.util.AppLog;

public class NotificationsActivity extends WPDrawerActivity {
    private NotificationsListFragment mNotesListFragment;

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
            mNotesListFragment = new NotificationsListFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.notifications_container, mNotesListFragment)
                    .commit();

            launchWithNoteId();

            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATIONS_ACCESSED);
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppLog.i(AppLog.T.NOTIFS, "Launching NotificationsActivity with new intent");

        launchWithNoteId();
    }

    private void launchWithNoteId() {
        if (isFinishing() || mNotesListFragment == null) return;

        Intent intent = getIntent();
        if (intent.hasExtra(NotificationsListFragment.NOTE_ID_EXTRA)) {
            boolean shouldShowKeyboard = intent.getBooleanExtra(NotificationsListFragment.NOTE_INSTANT_REPLY_EXTRA, false);
            mNotesListFragment.openNote(intent.getStringExtra(NotificationsListFragment.NOTE_ID_EXTRA), this, shouldShowKeyboard);
        }

        GCMIntentService.clearNotificationsMap();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // This can be removed if min_sdk is 16 or higher
        if (mNotesListFragment != null) {
            mNotesListFragment.onActivityResult(requestCode, resultCode, data);
        }
    }
}
