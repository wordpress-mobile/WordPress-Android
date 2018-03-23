package org.wordpress.android.ui.prefs.notifications;

import android.app.FragmentManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.view.ViewCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.SwitchCompat;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.util.LocaleManager;

import de.greenrobot.event.EventBus;

// Simple wrapper activity for NotificationsSettingsFragment
public class NotificationsSettingsActivity extends AppCompatActivity {
    private TextView mMessageTextView;
    private View mMessageContainer;

    protected SharedPreferences mSharedPreferences;
    protected SwitchCompat mMasterSwitch;
    protected Toolbar mToolbarSwitch;
    protected View mFragmentContainer;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.notifications_settings_activity);
        mFragmentContainer = findViewById(R.id.fragment_container);

        // Get shared preferences for master switch.
        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(NotificationsSettingsActivity.this);

        // Set up primary and secondary toolbars for master switch.
        setUpToolbars();

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
    public void onEventMainThread(NotificationEvents.NotificationsSettingsStatusChanged event) {
        if (TextUtils.isEmpty(event.getMessage())) {
            mMessageContainer.setVisibility(View.GONE);
        } else {
            mMessageContainer.setVisibility(View.VISIBLE);
            mMessageTextView.setText(event.getMessage());
        }
    }

    /**
     * Set up both primary toolbar for navigation and search, and secondary toolbar for master switch.
     */
    private void setUpToolbars() {
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

        // Set secondary toolbar title and master switch state from shared preferences.
        boolean isMasterChecked = mSharedPreferences.getBoolean(getString(R.string.wp_pref_notifications_master), true);
        hideDisabledView(isMasterChecked);

        mToolbarSwitch = findViewById(R.id.toolbar_with_switch);
        mToolbarSwitch.inflateMenu(R.menu.notifications_settings_secondary);
        mToolbarSwitch.setTitle(isMasterChecked
                                        ? getString(R.string.notification_settings_master_status_on)
                                        : getString(R.string.notification_settings_master_status_off));

        MenuItem menuItem = mToolbarSwitch.getMenu().findItem(R.id.master_switch);
        mMasterSwitch = (SwitchCompat) menuItem.getActionView();
        ViewCompat.setLabelFor(mToolbarSwitch, mMasterSwitch.getId());
        mMasterSwitch.setChecked(isMasterChecked);

        setToolbarTitleContentDescription();
        setupFocusabilityForTalkBack();

        mMasterSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                hideDisabledView(isChecked);
                mToolbarSwitch.setTitle(isChecked
                                                ? getString(R.string.notification_settings_master_status_on)
                                                : getString(R.string.notification_settings_master_status_off));
                mSharedPreferences.edit().putBoolean(getString(R.string.wp_pref_notifications_master), isChecked)
                                  .apply();

                if (isChecked) {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_SETTINGS_APP_NOTIFICATIONS_ENABLED);
                } else {
                    AnalyticsTracker.track(AnalyticsTracker.Stat.NOTIFICATION_SETTINGS_APP_NOTIFICATIONS_DISABLED);
                }
            }
        });

        mToolbarSwitch.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) {
                mMasterSwitch.setChecked(!mMasterSwitch.isChecked());
            }
        });

        mToolbarSwitch.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View view) {
                Toast.makeText(NotificationsSettingsActivity.this, mMasterSwitch.isChecked()
                                       ? getString(R.string.notification_settings_master_hint_on)
                                       : getString(R.string.notification_settings_master_hint_off),
                               Toast.LENGTH_SHORT).show();
                return true;
            }
        });
    }

    private void setToolbarTitleContentDescription() {
        for (int i = 0; i < mToolbarSwitch.getChildCount(); i++) {
            if (mToolbarSwitch.getChildAt(i) instanceof TextView) {
                mToolbarSwitch.getChildAt(i).setContentDescription(
                        getString(R.string.notification_settings_switch_desc));
            }
        }
    }

    private void setupFocusabilityForTalkBack() {
        mMasterSwitch.setFocusable(false);
        mMasterSwitch.setClickable(false);
        mToolbarSwitch.setFocusableInTouchMode(false);
        mToolbarSwitch.setFocusable(true);
        mToolbarSwitch.setClickable(true);
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
