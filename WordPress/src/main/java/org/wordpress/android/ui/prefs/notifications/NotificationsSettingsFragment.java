package org.wordpress.android.ui.prefs.notifications;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.Preference;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.Settings;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import de.greenrobot.event.EventBus;

public class NotificationsSettingsFragment extends PreferenceFragment {
    public static final String TAG = "NotificationSettingsFragment";

    // The number of notification types we support (e.g. timeline, email, mobile)
    private static final int TYPE_COUNT = 3;

    private NotificationsSettings mNotificationsSettings;

    private String mDeviceId;
    private boolean mNotificationsEnabled;

    List<PreferenceCategory> mTypePreferenceCategories = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.notifications_settings);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        mDeviceId = settings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_SERVER_ID, "");
    }

    @Override
    public void onResume() {
        super.onResume();

        mNotificationsEnabled = NotificationsUtils.isNotificationsEnabled(getActivity());

        if (hasNotificationsSettings()) {
            new LoadNotificationsTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, true);
        }

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

    private class LoadNotificationsTask extends AsyncTask<Boolean, Void, Void> {
        private boolean mShouldUpdateUI;

        @Override
        protected Void doInBackground(Boolean... params) {
            mShouldUpdateUI = params[0];
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

            return null;
        }

        @Override
        protected void onPostExecute(Void nada) {
            if (mShouldUpdateUI) {
                configureSiteSettings();
                configureOtherSettings();
                configureDotcomSettings();
            }

            updateMobileNotificationsState();
        }
    }

    // Updates the UI for preference screens based on if notifications are enabled or not
    private void updateMobileNotificationsState() {
        if (mTypePreferenceCategories == null || mTypePreferenceCategories.size() == 0) {
            return;
        }

        for (final PreferenceCategory category : mTypePreferenceCategories) {
            if (mNotificationsEnabled && category.getPreferenceCount() > TYPE_COUNT) {
                category.removePreference(category.getPreference(TYPE_COUNT));
            } else if (!mNotificationsEnabled && category.getPreferenceCount() == TYPE_COUNT) {
                Preference disabledMessage = new Preference(getActivity());
                disabledMessage.setSummary(R.string.notifications_disabled);
                disabledMessage.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference preference) {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getActivity().getApplicationContext().getPackageName(), null);
                        intent.setData(uri);

                        startActivity(intent);
                        return true;
                    }
                });

                category.addPreference(disabledMessage);
            }

            if (category.getPreference(TYPE_COUNT - 1) != null) {
                category.getPreference(TYPE_COUNT - 1).setEnabled(mNotificationsEnabled);
            }
        }

    }

    private void configureSiteSettings() {
        if (mNotificationsSettings == null || mNotificationsSettings.getSiteSettings() == null) {
            return;
        }

        // Retrieve blogs that are .com or Jetpack powered
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

        PreferenceScreen otherBlogsScreen = (PreferenceScreen) findPreference(
                getString(R.string.pref_notification_other_blogs));
        JSONObject otherSettings = mNotificationsSettings.getOtherSettings();
        addPreferencesForPreferenceScreen(otherBlogsScreen, Channel.OTHER, otherSettings, 0);
    }

    private void configureDotcomSettings() {
        if (mNotificationsSettings == null || mNotificationsSettings.getDotcomSettings() == null) {
            return;
        }

        PreferenceGroup dotcomPreferenceGroup = (PreferenceGroup) findPreference(
                getString(R.string.pref_notification_account_emails));
        NotificationsSettingsDialogPreference devicePreference = new NotificationsSettingsDialogPreference(
                getActivity(), null, Channel.DOTCOM, NotificationsSettings.Type.MOBILE, 0, mNotificationsSettings.getDotcomSettings(), mOnSettingsChangedListener
        );
        devicePreference.setTitle(R.string.notifications_updates_from_wordpress);
        devicePreference.setDialogTitle(R.string.notifications_updates_from_wordpress);
        devicePreference.setSummary(R.string.notifications_account_emails_summary);
        dotcomPreferenceGroup.addPreference(devicePreference);
    }

    private void addPreferencesForPreferenceScreen(PreferenceScreen preferenceScreen, Channel channel, JSONObject settingsObject, long siteId) {
        Context context = getActivity();
        if (context == null) return;

        PreferenceCategory rootCategory = new PreferenceCategory(context);
        rootCategory.setTitle(R.string.notification_types);
        preferenceScreen.addPreference(rootCategory);

        JSONObject timelineSettings = JSONUtils.queryJSON(settingsObject, "timeline", new JSONObject());
        NotificationsSettingsDialogPreference timelinePreference = new NotificationsSettingsDialogPreference(
                context, null, channel, NotificationsSettings.Type.TIMELINE, siteId, timelineSettings, mOnSettingsChangedListener
        );
        timelinePreference.setTitle(R.string.timeline);
        timelinePreference.setDialogTitle(R.string.timeline);
        rootCategory.addPreference(timelinePreference);

        JSONObject emailSettings = JSONUtils.queryJSON(settingsObject, "email", new JSONObject());
        NotificationsSettingsDialogPreference emailPreference = new NotificationsSettingsDialogPreference(
                context, null, channel, NotificationsSettings.Type.EMAIL, siteId, emailSettings, mOnSettingsChangedListener
        );
        emailPreference.setTitle(R.string.email);
        emailPreference.setDialogTitle(R.string.email);
        rootCategory.addPreference(emailPreference);

        JSONObject deviceSettings = JSONUtils.queryJSON(settingsObject, "device", new JSONObject());
        NotificationsSettingsDialogPreference devicePreference = new NotificationsSettingsDialogPreference(
                context, null, channel, NotificationsSettings.Type.MOBILE, siteId, deviceSettings, mOnSettingsChangedListener
        );
        devicePreference.setTitle(R.string.mobile_notifications);
        devicePreference.setDialogTitle(R.string.mobile_notifications);
        devicePreference.setEnabled(mNotificationsEnabled);
        rootCategory.addPreference(devicePreference);

        mTypePreferenceCategories.add(rootCategory);
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
                case DOTCOM:
                    try {
                        settingsObject.put(type.toString(), newValues);
                    } catch (JSONException e) {
                        AppLog.e(T.NOTIFS, "Could not build notification settings object");
                    }
                    break;
            }

            if (settingsObject.length() > 0) {
                WordPress.getRestClientUtilsV1_1().post("/me/notifications/settings", settingsObject, null, null, null);
            }
        }
    };

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
