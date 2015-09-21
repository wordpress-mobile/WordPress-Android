package org.wordpress.android.ui.prefs.notifications;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.notifications.NotificationEvents;

import de.greenrobot.event.EventBus;

// Simple wrapper activity for NotificationsSettingsFragment
public class NotificationsSettingsActivity extends AppCompatActivity {
    private View mMessageContainer;
    private TextView mMessageTextView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.notifications_settings_activity);

        setTitle(R.string.notification_settings);

        FragmentManager fragmentManager = getFragmentManager();
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, new NotificationsSettingsFragment())
                    .commit();
        }

        mMessageContainer = findViewById(R.id.notifications_settings_message_container);
        mMessageTextView = (TextView)findViewById(R.id.notifications_settings_message);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(NotificationEvents.NotificationsSettingsStatusChanged event) {
        if (TextUtils.isEmpty(event.getMessage())) {
            mMessageContainer.setVisibility(View.GONE);
        } else {
            mMessageContainer.setVisibility(View.VISIBLE);
            mMessageTextView.setText(event.getMessage());
        }
    }
}
