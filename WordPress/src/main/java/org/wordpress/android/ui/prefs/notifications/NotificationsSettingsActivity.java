package org.wordpress.android.ui.prefs.notifications;

import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.preference.PreferenceManager;

import com.google.android.material.elevation.ElevationOverlayProvider;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.prefs.notifications.PrefMasterSwitchToolbarView.MasterSwitchToolbarListener;
import org.wordpress.android.util.ContextExtensionsKt;

// Simple wrapper activity for NotificationsSettingsFragment
public class NotificationsSettingsActivity extends LocaleAwareActivity
        implements MasterSwitchToolbarListener {
    private TextView mMessageTextView;
    private View mMessageContainer;

    protected SharedPreferences mSharedPreferences;
    protected View mFragmentContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notifications_settings_activity);
        mFragmentContainer = findViewById(R.id.fragment_container);

        // Get shared preferences for master switch.
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(NotificationsSettingsActivity.this);

        // Set up primary toolbar
        setUpToolbar();

        // Set up master switch
        setUpMasterSwitch();

        FragmentManager fragmentManager = getFragmentManager();
        if (savedInstanceState == null) {
            fragmentManager.beginTransaction()
                           .add(R.id.fragment_container, new NotificationsSettingsFragment())
                           .commit();
        }

        mMessageContainer = findViewById(R.id.notifications_settings_message_container);
        mMessageTextView = findViewById(R.id.notifications_settings_message);
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

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(NotificationEvents.NotificationsSettingsStatusChanged event) {
        if (TextUtils.isEmpty(event.getMessage())) {
            mMessageContainer.setVisibility(View.GONE);
        } else {
            mMessageContainer.setVisibility(View.VISIBLE);
            mMessageTextView.setText(event.getMessage());
        }
    }

    /**
     * Set up primary toolbar for navigation and search
     */
    private void setUpToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar_with_search);

        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setTitle(R.string.notification_settings);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setDisplayShowTitleEnabled(true);
        }
    }

    /**
     * Sets up master switch to disable/enable all notification settings
     */
    private void setUpMasterSwitch() {
        PrefMasterSwitchToolbarView masterSwitchToolBarView = findViewById(R.id.master_switch);
        masterSwitchToolBarView.setMasterSwitchToolbarListener(this);

        // Set master switch state from shared preferences.
        boolean isMasterChecked = mSharedPreferences.getBoolean(getString(R.string.wp_pref_notifications_master), true);
        masterSwitchToolBarView.loadInitialState(isMasterChecked);

        ElevationOverlayProvider elevationOverlayProvider = new ElevationOverlayProvider(this);
        float cardElevation = getResources().getDimension(R.dimen.card_elevation);
        int appBarColor = elevationOverlayProvider
                .compositeOverlay(ContextExtensionsKt.getColorFromAttribute(this, R.attr.wpColorAppBar), cardElevation);

        masterSwitchToolBarView.setBackgroundColor(appBarColor);

        hideDisabledView(isMasterChecked);
    }

    @Override
    public void onMasterSwitchCheckedChanged(
            CompoundButton buttonView,
            boolean isChecked
    ) {
        mSharedPreferences.edit().putBoolean(getString(R.string.wp_pref_notifications_master), isChecked)
                          .apply();

        hideDisabledView(isChecked);

        if (isChecked) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_SETTINGS_APP_NOTIFICATIONS_ENABLED);
        } else {
            AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_SETTINGS_APP_NOTIFICATIONS_DISABLED);
        }
    }

    /**
     * Hide view when Notification Settings are disabled by toggling the master switch off.
     *
     * @param isMasterChecked TRUE to hide disabled view, FALSE to show disabled view
     */
    protected void hideDisabledView(boolean isMasterChecked) {
        LinearLayout notificationsDisabledView = findViewById(R.id.notification_settings_disabled_view);
        notificationsDisabledView.setVisibility(isMasterChecked ? View.INVISIBLE : View.VISIBLE);
        mFragmentContainer.setVisibility(isMasterChecked ? View.VISIBLE : View.GONE);
    }
}
