package org.wordpress.android.ui.prefs;

import android.app.Dialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.StringMap;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class NotificationSettingsFragment extends PreferenceFragment {
    public static final String TAG = "NotificationSettingsFragment";
    private ArrayList<StringMap<Double>> mMutedBlogsList;
    private Map<String, Object> mNotificationSettings;
    private boolean mNotificationSettingsChanged;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.notification_settings);
        loadNotifications();
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.manage_notifications);
    }

    private void loadNotifications() {
        AppLog.d(T.NOTIFS, "Preferences > loading notification settings");

        // Add notifications group back in case it was previously removed from being logged out
        PreferenceCategory notificationTypesCategory = (PreferenceCategory) findPreference(
                getString(R.string.pref_notification_types));
        notificationTypesCategory.removeAll();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String settingsJson = settings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, null);
        if (settingsJson == null) {
            return;
        }
        try {
            Gson gson = new Gson();
            mNotificationSettings = gson.fromJson(settingsJson, HashMap.class);
            StringMap<?> mutedBlogsMap = (StringMap<?>) mNotificationSettings.get("muted_blogs");
            mMutedBlogsList = (ArrayList<StringMap<Double>>) mutedBlogsMap.get("value");
            Collections.sort(mMutedBlogsList, BlogNameComparatorForMutedBlogsList);

            Object[] mTypeList = mNotificationSettings.keySet().toArray();

            for (Object aMTypeList : mTypeList) {
                if (!aMTypeList.equals("muted_blogs") && !aMTypeList.equals("mute_until")) {
                    StringMap<?> typeMap = (StringMap<?>) mNotificationSettings
                            .get(aMTypeList.toString());
                    CheckBoxPreference typePreference = new CheckBoxPreference(getActivity());
                    typePreference.setKey(aMTypeList.toString());
                    typePreference.setChecked(MapUtils.getMapBool(typeMap, "value"));
                    typePreference.setTitle(typeMap.get("desc").toString());
                    typePreference.setOnPreferenceChangeListener(mTypeChangeListener);
                    notificationTypesCategory.addPreference(typePreference);
                }
            }

            PreferenceCategory selectBlogsCategory = (PreferenceCategory) findPreference(
                    getActivity().getString(R.string.pref_notification_blogs));
            selectBlogsCategory.removeAll();
            for (int i = 0; i < mMutedBlogsList.size(); i++) {
                StringMap<?> blogMap = mMutedBlogsList.get(i);
                String blogName = (String) blogMap.get("blog_name");
                if (blogName == null || blogName.trim().equals(""))
                    blogName = (String) blogMap.get("url");
                CheckBoxPreference blogPreference = new CheckBoxPreference(getActivity());
                blogPreference.setChecked(!MapUtils.getMapBool(blogMap, "value"));
                blogPreference.setTitle(StringUtils.unescapeHTML(blogName));
                blogPreference.setOnPreferenceChangeListener(mMuteBlogChangeListener);
                // set the order here so it matches the key in mMutedBlogsList since
                // mMuteBlogChangeListener uses the order to locate the clicked blog
                blogPreference.setOrder(i);
                selectBlogsCategory.addPreference(blogPreference);
            }
        } catch (JsonSyntaxException e) {
            AppLog.v(T.NOTIFS, "Notification Settings Json could not be parsed.");
            return;
        } catch (Exception e) {
            AppLog.v(T.NOTIFS, "Failed to load notification settings.");
            return;
        }

        CheckBoxPreference notificationsEnabledCheckBox = (CheckBoxPreference) findPreference(
                getString(R.string.pref_notifications_enabled));
        notificationsEnabledCheckBox.setOnPreferenceChangeListener(mNotificationsEnabledChangeListener);
    }

    /**
     * Listens for changes to notification type settings
     */
    private final OnPreferenceChangeListener mTypeChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            // Update the mNoteSettings map with the new value
            if (preference instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                boolean isChecked = (Boolean) newValue;
                String key = preference.getKey();
                StringMap<Integer> typeMap = (StringMap<Integer>) mNotificationSettings.get(key);
                typeMap.put("value", (isChecked) ? 1 : 0);
                mNotificationSettings.put(key, typeMap);
                checkBoxPreference.setChecked(isChecked);
                mNotificationSettingsChanged = true;
            }
            return false;
        }
    };


    /**
     * Listens for changes to notification blogs settings
     */
    private final OnPreferenceChangeListener mMuteBlogChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                boolean isChecked = (Boolean) newValue;
                int id = checkBoxPreference.getOrder();
                StringMap<Double> blogMap = mMutedBlogsList.get(id);
                blogMap.put("value", (!isChecked) ? 1.0 : 0.0);
                mMutedBlogsList.set(id, blogMap);
                StringMap<ArrayList> mutedBlogsMap = (StringMap<ArrayList>) mNotificationSettings.get("muted_blogs");
                mutedBlogsMap.put("value", mMutedBlogsList);
                mNotificationSettings.put("muted_blogs", mutedBlogsMap);
                checkBoxPreference.setChecked(isChecked);
                mNotificationSettingsChanged = true;
            }
            return false;
        }
    };

    /**
     * Listens for changes to notification enabled toggle
     */
    private final OnPreferenceChangeListener mNotificationsEnabledChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof CheckBoxPreference) {
                final boolean isChecked = (Boolean) newValue;
                if (isChecked) {
                    StringMap<String> muteUntilMap = (StringMap<String>) mNotificationSettings.get("mute_until");
                    muteUntilMap.put("value", "0");
                    mNotificationSettings.put("mute_until", muteUntilMap);
                    mNotificationSettingsChanged = true;
                    return true;
                } else {
                    final Dialog dialog = new Dialog(getActivity());
                    dialog.setContentView(R.layout.notifications_enabled_dialog);
                    dialog.setTitle(R.string.notifications);
                    dialog.setCancelable(true);

                    Button offButton = (Button) dialog.findViewById(R.id.notificationsOff);
                    offButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            enabledButtonClick(v);
                            dialog.dismiss();
                        }
                    });
                    Button oneHourButton = (Button) dialog.findViewById(R.id.notifications1Hour);
                    oneHourButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            enabledButtonClick(v);
                            dialog.dismiss();
                        }
                    });
                    Button eightHoursButton = (Button) dialog.findViewById(R.id.notifications8Hours);
                    eightHoursButton.setOnClickListener(new OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            enabledButtonClick(v);
                            dialog.dismiss();
                        }
                    });
                    dialog.show();
                }
            }
            return false;
        }
    };


    private void enabledButtonClick(View v) {
        StringMap<String> muteUntilMap = (StringMap<String>) mNotificationSettings.get("mute_until");
        if (muteUntilMap != null) {
            if (v.getId() == R.id.notificationsOff) {
                muteUntilMap.put("value", "forever");
            } else if (v.getId() == R.id.notifications1Hour) {
                muteUntilMap.put("value", String.valueOf((System.currentTimeMillis() / 1000) + 3600));
            } else if (v.getId() == R.id.notifications8Hours) {
                muteUntilMap.put("value", String.valueOf((System.currentTimeMillis() / 1000) + (3600 * 8)));
            }
            CheckBoxPreference enabledCheckBoxPreference = (CheckBoxPreference) findPreference(getString(R.string
                    .pref_notifications_enabled));
            enabledCheckBoxPreference.setChecked(false);
            mNotificationSettings.put("mute_until", muteUntilMap);
            mNotificationSettingsChanged = true;
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
            if (mNotificationSettings != null) {
                Context context = WordPress.getContext();
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = settings.edit();
                Gson gson = new Gson();
                String settingsJson = gson.toJson(mNotificationSettings);
                editor.putString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, settingsJson);
                editor.commit();
                NotificationsUtils.setPushNotificationSettings(context);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mNotificationSettingsChanged = false;
        }
    }

    private static final Comparator<StringMap<?>> BlogNameComparatorForMutedBlogsList = new Comparator<StringMap<?>>() {
        public int compare(StringMap<?> blog1, StringMap<?> blog2) {
            String blogName1 = blog1.get("blog_name").toString();
            if (blogName1.length() == 0) {
                blogName1 = blog1.get("url").toString();
            }

            String blogName2 = blog2.get("blog_name").toString();
            if (blogName2.length() == 0) {
                blogName2 = blog2.get("url").toString();
            }

            return blogName1.compareToIgnoreCase(blogName2);
        }
    };

    @Override
    public void onStop() {
        super.onStop();
        if (mNotificationSettingsChanged) {
            sendNotificationsSettings();
        }
    }
}
