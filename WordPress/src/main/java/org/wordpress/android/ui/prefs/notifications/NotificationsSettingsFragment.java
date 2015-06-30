package org.wordpress.android.ui.prefs.notifications;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.support.v4.view.MenuItemCompat;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.widget.CompoundButton;
import android.widget.Switch;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.GravatarUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.List;
import java.util.Map;

public class NotificationsSettingsFragment extends PreferenceFragment {
    public static final String TAG = "NotificationSettingsFragment";
    private boolean mNotificationSettingsChanged;

    private Switch mEnabledSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notification_settings);

        setHasOptionsMenu(true);
    }

    @Override
    public void onResume() {
        super.onResume();

        new LoadNotificationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);

        inflater.inflate(R.menu.notifications_settings, menu);

        MenuItem enabledMenuItem = menu.findItem(R.id.notifications_enabled);
        if (enabledMenuItem != null && MenuItemCompat.getActionView(enabledMenuItem) != null) {
            mEnabledSwitch = (Switch)MenuItemCompat.getActionView(enabledMenuItem).findViewById(R.id.notifications_enabled_switch);
            mEnabledSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    setAllCategoriesEnabled(isChecked);
                }
            });
        }
    }

    private void setAllCategoriesEnabled(boolean isEnabled) {
        PreferenceCategory blogsCategory = (PreferenceCategory) findPreference(
                getString(R.string.pref_notification_blogs));
        PreferenceCategory otherBlogsCategory = (PreferenceCategory) findPreference(
                getString(R.string.pref_notification_other_blogs));
        PreferenceCategory accountEmailsCategory = (PreferenceCategory) findPreference(
                getString(R.string.pref_notification_account_emails));
        PreferenceCategory sightsAndSoundsCategory = (PreferenceCategory) findPreference(
                getString(R.string.pref_notification_sights_sounds));

        blogsCategory.setEnabled(isEnabled);
        otherBlogsCategory.setEnabled(isEnabled);
        accountEmailsCategory.setEnabled(isEnabled);
        sightsAndSoundsCategory.setEnabled(isEnabled);
    }

    private class LoadNotificationsTask extends AsyncTask<Void, Void, Void> {

        @Override
        protected Void doInBackground(Void... params) {
            List<Map<String, Object>> allBlogs = WordPress.wpDB.getBlogsBy("NOT(dotcomFlag=0 AND wpVersion!='')", null, 0, false);

            Context context = getActivity();

            PreferenceCategory blogsCategory = (PreferenceCategory) findPreference(
                    getString(R.string.pref_notification_blogs));

            for (Map blog : allBlogs) {
                if (context == null) return null;

                String siteUrl = MapUtils.getMapStr(blog, "url");
                NotificationsPreference preference = new NotificationsPreference(context);
                preference.setLayoutResource(R.layout.notifications_site_preference);

                String title = MapUtils.getMapStr(blog, "blogName");
                preference.setTitle(title);
                preference.setSummary(UrlUtils.getDomainFromUrl(siteUrl));
                preference.setBlavatarUrl(GravatarUtils.blavatarFromUrl(
                        siteUrl,
                        context.getResources().getDimensionPixelSize(R.dimen.avatar_sz_medium))
                );
                blogsCategory.addPreference(preference);
            }

            return null;
        }
    }

    private void sendNotificationsSettings() {
        AppLog.d(T.NOTIFS, "Send push notification settings");
        new sendNotificationSettingsTask().execute();
    }

    /**
     * Performs the notification settings save in the background
     */
    private class sendNotificationSettingsTask extends AsyncTask<Void, Void, Void> {
        // Sends updated notification settings to WP.com
        @Override
        protected Void doInBackground(Void... params) {
            /*if (mNotificationSettings != null) {
                Context context = WordPress.getContext();
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                Gson gson = new Gson();
                String settingsJson = gson.toJson(mNotificationSettings);
                editor.putString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, settingsJson);
                editor.apply();
                NotificationsUtils.setPushNotificationSettings(context);
            }*/
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mNotificationSettingsChanged = false;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mNotificationSettingsChanged) {
            sendNotificationsSettings();
        }
    }
}
