package org.wordpress.android.ui.prefs.notifications;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.Switch;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.NotificationsSettings;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public class NotificationsSettingsFragment extends PreferenceFragment {
    public static final String TAG = "NotificationSettingsFragment";
    private boolean mNotificationSettingsChanged;

    private Switch mEnabledSwitch;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notifications_settings);

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
            JSONObject settingsJson;
            try {
                SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());
                settingsJson = new JSONObject(
                        sharedPreferences.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, "")
                );
            } catch (JSONException e) {
                AppLog.e(T.NOTIFS, "Could not parse notifications settings JSON");
                return null;
            }

            NotificationsSettings settings = new NotificationsSettings(settingsJson);
            if (settings.getSiteSettings() == null) {
                return null;
            }

            // Retrieve blogs that are .com or jetpack powered
            List<Map<String, Object>> blogs = WordPress.wpDB.getBlogsBy("NOT(dotcomFlag=0 AND wpVersion!='')", null, 0, false);

            Context context = getActivity();

            PreferenceCategory blogsCategory = (PreferenceCategory) findPreference(
                    getString(R.string.pref_notification_blogs));

            for (Map blog : blogs) {
                if (context == null) return null;

                String siteUrl = MapUtils.getMapStr(blog, "url");
                String title = MapUtils.getMapStr(blog, "blogName");
                long siteId = MapUtils.getMapLong(blog, "blogId");

                PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(context);
                prefScreen.setTitle(title);
                prefScreen.setSummary(UrlUtils.getDomainFromUrl(siteUrl));

                JSONObject siteSettings = settings.getSiteSettings().get(siteId);
                if (siteSettings == null) {
                    siteSettings = new JSONObject();
                }

                // Add the 3 setting types to the preference screen
                JSONObject timelineSettings = JSONUtils.queryJSON(siteSettings, "timeline", new JSONObject());
                NotificationsSettingsDialogPreference timelinePref = new NotificationsSettingsDialogPreference(
                        context, null, NotificationsSettings.Type.TIMELINE, timelineSettings, mOnSettingsChangedListener
                );
                timelinePref.setTitle(R.string.timeline);
                timelinePref.setDialogTitle(R.string.timeline);
                prefScreen.addPreference(timelinePref);

                JSONObject emailSettings = JSONUtils.queryJSON(siteSettings, "email", new JSONObject());
                NotificationsSettingsDialogPreference emailPref = new NotificationsSettingsDialogPreference(
                        context, null, NotificationsSettings.Type.EMAIL, emailSettings, mOnSettingsChangedListener
                );
                emailPref.setTitle(R.string.email);
                emailPref.setDialogTitle(R.string.email);
                prefScreen.addPreference(emailPref);

                JSONObject deviceSettings = JSONUtils.queryJSON(siteSettings, "device", new JSONObject());
                NotificationsSettingsDialogPreference devicePref = new NotificationsSettingsDialogPreference(
                        context, null, NotificationsSettings.Type.MOBILE, deviceSettings, mOnSettingsChangedListener
                );
                devicePref.setTitle(R.string.push_notifications);
                devicePref.setDialogTitle(R.string.push_notifications);
                prefScreen.addPreference(devicePref);

                blogsCategory.addPreference(prefScreen);
            }

            return null;
        }
    }

    private NotificationsSettingsDialogPreference.OnNotificationsSettingsChangedListener mOnSettingsChangedListener = new NotificationsSettingsDialogPreference.OnNotificationsSettingsChangedListener() {
        @Override
        public void OnNotificationsSettingsChanged(NotificationsSettings.Type type, JSONObject newValues) {
            if (!isAdded()) return;

            // Construct a new settings JSONObject to send back to WP.com
            JSONObject settingsObject = new JSONObject();
            try {
                settingsObject.put(type.toString(), newValues);
                new SendNotificationSettingsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, settingsObject);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
    };

    private void sendNotificationsSettings() {
        AppLog.d(T.NOTIFS, "Send push notification settings");
        new SendNotificationSettingsTask().execute();
    }

    /**
     * Performs the notification settings save in the background
     */
    private class SendNotificationSettingsTask extends AsyncTask<JSONObject, Void, Void> {
        // Sends updated notification settings to WP.com
        @Override
        protected Void doInBackground(JSONObject... params) {

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

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, @Nonnull Preference preference) {
        super.onPreferenceTreeClick(preferenceScreen, preference);

        // PreferenceScreens don't show the toolbar, so we'll manually add one
        // See: http://stackoverflow.com/a/27455363/309558
        if (preference instanceof PreferenceScreen) {
            addToolbarToPreferenceScreen((PreferenceScreen) preference);
        }

        return false;
    }

    public void addToolbarToPreferenceScreen(PreferenceScreen preferenceScreen) {
        if (!isAdded()) return;
        final Dialog dialog = preferenceScreen.getDialog();

        LinearLayout root = (LinearLayout) dialog.findViewById(android.R.id.list).getParent();
        Toolbar toolbar = (Toolbar) LayoutInflater.from(getActivity()).inflate(R.layout.toolbar, root, false);
        root.addView(toolbar, 0);
        toolbar.setTitle(preferenceScreen.getTitle());
        toolbar.setNavigationIcon(R.drawable.ic_arrow_back_white_24dp);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });
    }
}
