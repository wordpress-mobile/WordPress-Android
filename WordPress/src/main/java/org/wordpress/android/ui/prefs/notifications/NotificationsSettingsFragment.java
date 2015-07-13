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

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.NotificationsSettings;
import org.wordpress.android.models.NotificationsSettings.Channel;
import org.wordpress.android.models.NotificationsSettings.Type;
import org.wordpress.android.ui.notifications.NotificationEvents;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.JSONUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.UrlUtils;

import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import de.greenrobot.event.EventBus;

public class NotificationsSettingsFragment extends PreferenceFragment {
    public static final String TAG = "NotificationSettingsFragment";

    private NotificationsSettings mNotificationsSettings;
    private Switch mEnabledSwitch;

    private String mDeviceId;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notifications_settings);

        setHasOptionsMenu(true);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mDeviceId = settings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_SERVER_ID, "");

        if (hasNotificationsSettings()) {
            new LoadNotificationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, true);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        refreshSettings();
    }

    private void refreshSettings() {
        if (!hasNotificationsSettings()) {
            EventBus.getDefault().post(new NotificationEvents.NotificationsSettingsStatusChanged(getString(R.string.loading)));
        }

        NotificationsUtils.getPushNotificationSettings(getActivity(), new RestRequest.Listener() {
            @Override
            public void onResponse(JSONObject response) {
                AppLog.d(T.NOTIFS, "Get settings action succeeded");
                if (!isAdded()) return;

                boolean settingsExisted = hasNotificationsSettings();
                if (!settingsExisted) {
                    EventBus.getDefault().post(new NotificationEvents.NotificationsSettingsStatusChanged(null));
                }

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, response.toString());
                editor.apply();

                new LoadNotificationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, !settingsExisted);
            }
        }, new RestRequest.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                if (!isAdded()) return;
                AppLog.e(T.NOTIFS, "Get settings action failed", error);

                if (!hasNotificationsSettings()) {
                    EventBus.getDefault().post(new NotificationEvents.NotificationsSettingsStatusChanged(getString(R.string.error_loading_notifications)));
                }
            }
        });
    }

    private boolean hasNotificationsSettings() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

        return sharedPreferences.contains(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS);
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
        Preference blogsCategory = findPreference(
                getString(R.string.pref_notification_blogs));
        Preference otherBlogsCategory = findPreference(
                getString(R.string.pref_notification_other_blogs));
        Preference accountEmailsCategory = findPreference(
                getString(R.string.pref_notification_account_emails));
        Preference sightsAndSoundsCategory = findPreference(
                getString(R.string.pref_notification_sights_sounds));

        blogsCategory.setEnabled(isEnabled);
        otherBlogsCategory.setEnabled(isEnabled);
        accountEmailsCategory.setEnabled(isEnabled);
        sightsAndSoundsCategory.setEnabled(isEnabled);
    }

    private class LoadNotificationsTask extends AsyncTask<Boolean, Void, Void> {

        @Override
        protected Void doInBackground(Boolean... params) {
            boolean shouldUpdateUI = params[0];
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

            mNotificationsSettings = new NotificationsSettings(settingsJson);

            if (shouldUpdateUI) {
                configureSiteSettings();
                configureOtherSettings();
            }

            return null;
        }
    }

    private void configureSiteSettings() {
        if (mNotificationsSettings == null || mNotificationsSettings.getSiteSettings() == null) {
            return;
        }

        // Retrieve blogs that are .com or jetpack powered
        List<Map<String, Object>> blogs = WordPress.wpDB.getBlogsBy("NOT(dotcomFlag=0 AND wpVersion!='')", null, 0, false);

        Context context = getActivity();

        PreferenceCategory blogsCategory = (PreferenceCategory) findPreference(
                getString(R.string.pref_notification_blogs));

        for (Map blog : blogs) {
            if (context == null) return;

            String siteUrl = MapUtils.getMapStr(blog, "url");
            String title = MapUtils.getMapStr(blog, "blogName");
            long siteId = MapUtils.getMapLong(blog, "blogId");

            PreferenceScreen prefScreen = getPreferenceManager().createPreferenceScreen(context);
            prefScreen.setTitle(title);
            prefScreen.setSummary(UrlUtils.getDomainFromUrl(siteUrl));

            JSONObject siteSettings = mNotificationsSettings.getSiteSettings().get(siteId);
            if (siteSettings == null) {
                siteSettings = new JSONObject();
            }

            addPreferencesForPreferenceScreen(prefScreen, Channel.SITES, siteSettings, siteId);

            blogsCategory.addPreference(prefScreen);
        }
    }

    private void configureOtherSettings() {
        if (mNotificationsSettings == null || mNotificationsSettings.getOtherSettings() == null) {
            return;
        }

        PreferenceScreen otherPreferenceScreen = (PreferenceScreen) findPreference(
                getString(R.string.pref_notification_other_blogs));
        JSONObject otherSettings = mNotificationsSettings.getOtherSettings();
        addPreferencesForPreferenceScreen(otherPreferenceScreen, Channel.OTHER, otherSettings, 0);
    }

    private void addPreferencesForPreferenceScreen(PreferenceScreen preferenceScreen, Channel channel, JSONObject settingsObject, long siteId) {
        Context context = getActivity();
        if (context == null) return;

        JSONObject timelineSettings = JSONUtils.queryJSON(settingsObject, "timeline", new JSONObject());
        NotificationsSettingsDialogPreference timelinePreference = new NotificationsSettingsDialogPreference(
                context, null, channel, NotificationsSettings.Type.TIMELINE, siteId, timelineSettings, mOnSettingsChangedListener
        );
        timelinePreference.setTitle(R.string.timeline);
        timelinePreference.setDialogTitle(R.string.timeline);
        preferenceScreen.addPreference(timelinePreference);

        JSONObject emailSettings = JSONUtils.queryJSON(settingsObject, "email", new JSONObject());
        NotificationsSettingsDialogPreference emailPreference = new NotificationsSettingsDialogPreference(
                context, null, channel, NotificationsSettings.Type.EMAIL, siteId, emailSettings, mOnSettingsChangedListener
        );
        emailPreference.setTitle(R.string.email);
        emailPreference.setDialogTitle(R.string.email);
        preferenceScreen.addPreference(emailPreference);

        JSONObject deviceSettings = JSONUtils.queryJSON(settingsObject, "device", new JSONObject());
        NotificationsSettingsDialogPreference devicePreference = new NotificationsSettingsDialogPreference(
                context, null, channel, NotificationsSettings.Type.MOBILE, siteId, deviceSettings, mOnSettingsChangedListener
        );
        devicePreference.setTitle(R.string.mobile_notifications);
        devicePreference.setDialogTitle(R.string.mobile_notifications);
        preferenceScreen.addPreference(devicePreference);
    }

    private NotificationsSettingsDialogPreference.OnSiteSettingsChangedListener mOnSettingsChangedListener =
            new NotificationsSettingsDialogPreference.OnSiteSettingsChangedListener() {
        @SuppressWarnings("unchecked")
        @Override
        public void OnNotificationsSettingsChanged(Channel channel, NotificationsSettings.Type type, long siteId, JSONObject newValues) {
            if (!isAdded()) return;

            // Construct a new settings JSONObject to send back to WP.com
            JSONObject settingsObject = new JSONObject();
            switch (channel) {
                case SITES:
                    try {
                        JSONObject subObject = new JSONObject();
                        if (type == Type.MOBILE) {
                            newValues.put("device_id", Long.parseLong(mDeviceId));
                        }
                        subObject.put(type.toString(), newValues);
                        subObject.put("site_id", siteId);

                        JSONArray sitesArray = new JSONArray();
                        sitesArray.put(subObject);

                        settingsObject.put("sites", sitesArray);

                    } catch (JSONException e) {
                        AppLog.e(T.NOTIFS, "Could not build notification settings object");
                    }
                    break;
                case OTHER:
                    try {
                        JSONObject subObject = new JSONObject();
                        if (type == Type.MOBILE) {
                            newValues.put("device_id", Long.parseLong(mDeviceId));
                        }
                        subObject.put(type.toString(), newValues);

                        settingsObject.put("other", subObject);
                    } catch (JSONException e) {
                        AppLog.e(T.NOTIFS, "Could not build notification settings object");
                    }
                    break;
            }

            if (settingsObject.length() > 0) {
                new SendNotificationSettingsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, settingsObject);
            }
        }
    };

    /**
     * Performs the notification settings save in the background
     */
    private class SendNotificationSettingsTask extends AsyncTask<JSONObject, Void, Void> {
        // Sends updated notification settings to WP.com
        @Override
        protected Void doInBackground(JSONObject... params) {
            if (params.length < 1) return null;

            JSONObject apiParams = params[0];

            WordPress.getRestClientUtilsV1_1().post("/me/notifications/settings", apiParams, null, null, null);
            return null;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
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
