package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;

import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.StringMap;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPress.SignOutAsync.SignOutCallback;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.accounts.ManageBlogsActivity;
import org.wordpress.android.ui.accounts.NewBlogActivity;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPEditTextPreference;
import org.wordpress.passcodelock.AppLockManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class SettingsFragment extends PreferenceFragment {
    private ArrayList<StringMap<Double>> mMutedBlogsList;
    private Map<String, Object> mNotificationSettings;
    private SharedPreferences mSettings;
    private boolean mNotificationSettingsChanged;

    private PreferenceGroup mNotificationsGroup;
    WPEditTextPreference mTaglineTextPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_SETTINGS);
        }

        addPreferencesFromResource(R.xml.preferences);

        mNotificationsGroup = (PreferenceGroup) findPreference("wp_pref_notifications_category");

        OnPreferenceChangeListener preferenceChangeListener = new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) { // cancelled dismiss keyboard
                    preference.setSummary(newValue.toString());
                }
                ActivityUtils.hideKeyboard(getActivity());
                return true;
            }
        };

        mTaglineTextPreference = (WPEditTextPreference) findPreference("wp_pref_post_signature");
        if (mTaglineTextPreference != null) {
            mTaglineTextPreference.setOnPreferenceChangeListener(preferenceChangeListener);
        }
        Preference signOutPreference = findPreference("wp_pref_sign_out");
        signOutPreference.setOnPreferenceClickListener(signOutPreferenceClickListener);

        Preference resetAutoShare = findPreference("wp_reset_share_pref");
        resetAutoShare.setOnPreferenceClickListener(resetAUtoSharePreferenceClickListener);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        initNotifications();

        // Passcode Lock not supported
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            final CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference(
                    "wp_pref_passlock_enabled");
            // disable on-click changes on the property
            passcodeEnabledCheckBoxPreference.setOnPreferenceClickListener(new OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    passcodeEnabledCheckBoxPreference.setChecked(
                            AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked());
                    return false;
                }
            });
        } else {
            PreferenceScreen rootScreen = (PreferenceScreen) findPreference("wp_pref_root");
            PreferenceGroup passcodeGroup = (PreferenceGroup) findPreference("wp_passcode_lock_category");
            rootScreen.removePreference(passcodeGroup);
        }
        displayPreferences();
    }

    private void initNotifications() {
        // AuthenticatorRequest notification settings if needed
        if (WordPress.hasValidWPComCredentials(getActivity())) {
            String settingsJson = mSettings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, null);
            if (settingsJson == null) {
                com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        AppLog.d(T.NOTIFS, "Get settings action succeeded");
                        Editor editor = mSettings.edit();
                        try {
                            JSONObject settingsJSON = jsonObject.getJSONObject("settings");
                            editor.putString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS,
                                    settingsJSON.toString());
                            editor.commit();
                        } catch (JSONException e) {
                            AppLog.e(T.NOTIFS,
                                    "Can't parse the JSON object returned from the server that contains PN settings.",
                                    e);
                        }
                        refreshWPComAuthCategory();
                    }
                };
                RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        AppLog.e(T.NOTIFS, "Get settings action failed", volleyError);
                    }
                };
                NotificationsUtils.getPushNotificationSettings(getActivity(), listener, errorListener);
            }
        }
    }

    private void hidePostSignatureCategory() {
        PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("wp_pref_root");
        PreferenceCategory postSignature = (PreferenceCategory) findPreference("wp_post_signature");
        if (preferenceScreen != null && postSignature != null) {
            preferenceScreen.removePreference(postSignature);
        }
    }

    private void hideNotificationBlogsCategory() {
        PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("wp_pref_notifications");
        PreferenceCategory blogs = (PreferenceCategory) findPreference("wp_pref_notification_blogs");
        if (preferenceScreen != null && blogs != null) {
            preferenceScreen.removePreference(blogs);
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        // the set of blogs may have changed while we were away
        updateSelfHostedBlogsPreferenceCategory();
        refreshWPComAuthCategory();

        //update Passcode lock row if available
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference(
                    "wp_pref_passlock_enabled");
            if (AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked()) {
                passcodeEnabledCheckBoxPreference.setChecked(true);
            } else {
                passcodeEnabledCheckBoxPreference.setChecked(false);
            }
        }
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
        }
        return super.onOptionsItemSelected(item);
    }

    public void refreshWPComAuthCategory() {
        PreferenceCategory wpComCategory = (PreferenceCategory) findPreference("wp_pref_wpcom");
        wpComCategory.removeAll();
        addWpComSignIn(wpComCategory, 0);
        addWpComShowHideButton(wpComCategory, 5);
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccountsBy("dotcomFlag = 1 AND isHidden = 0", null);
        addAccounts(wpComCategory, accounts, 10);
    }

    /**
     * Update the "wpcom blogs" preference category to contain a preference for each blog to configure
     * blog-specific settings.
     */
    void updateSelfHostedBlogsPreferenceCategory() {
        PreferenceCategory blogsCategory = (PreferenceCategory) findPreference("wp_pref_self_hosted_blogs");
        blogsCategory.removeAll();
        int order = 0;

        // Add self-hosted blog button
        Preference addBlogPreference = new Preference(getActivity());
        addBlogPreference.setTitle(R.string.add_self_hosted_blog);
        Intent intentWelcome = new Intent(getActivity(), SignInActivity.class);
        intentWelcome.putExtra(SignInActivity.START_FRAGMENT_KEY, SignInActivity.ADD_SELF_HOSTED_BLOG);
        addBlogPreference.setIntent(intentWelcome);
        addBlogPreference.setOrder(order++);
        blogsCategory.addPreference(addBlogPreference);

        // Add self hosted list
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccountsBy("dotcomFlag=0", null);
        addAccounts(blogsCategory, accounts, order);
    }

    void displayPreferences() {
        // Post signature
        if (WordPress.wpDB.getNumVisibleAccounts() == 0) {
            hidePostSignatureCategory();
            hideNotificationBlogsCategory();
        } else {
            if (mTaglineTextPreference.getText() == null || mTaglineTextPreference.getText().equals("")) {
                mTaglineTextPreference.setSummary(R.string.posted_from);
                mTaglineTextPreference.setText(getString(R.string.posted_from));
            } else {
                mTaglineTextPreference.setSummary(mTaglineTextPreference.getText());
            }
        }
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
            CheckBoxPreference enabledCheckBoxPreference = (CheckBoxPreference) findPreference(
                    "wp_pref_notifications_enabled");
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
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());
                SharedPreferences.Editor editor = settings.edit();
                Gson gson = new Gson();
                String settingsJson = gson.toJson(mNotificationSettings);
                editor.putString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, settingsJson);
                editor.commit();
                NotificationsUtils.setPushNotificationSettings(getActivity());
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mNotificationSettingsChanged = false;
        }
    }

    private void addWpComSignIn(PreferenceCategory wpComCategory, int order) {
        if (WordPress.hasValidWPComCredentials(getActivity())) {
            String username = mSettings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            Preference usernamePref = new Preference(getActivity());
            usernamePref.setTitle(getString(R.string.username));
            usernamePref.setSummary(username);
            usernamePref.setSelectable(false);
            usernamePref.setOrder(order);
            wpComCategory.addPreference(usernamePref);

            Preference createWPComBlogPref = new Preference(getActivity());
            createWPComBlogPref.setTitle(getString(R.string.create_new_blog_wpcom));
            Intent intent = new Intent(getActivity(), NewBlogActivity.class);
            createWPComBlogPref.setIntent(intent);
            createWPComBlogPref.setOrder(order + 1);
            wpComCategory.addPreference(createWPComBlogPref);

            loadNotifications();
        } else {
            Preference signInPref = new Preference(getActivity());
            signInPref.setTitle(getString(R.string.sign_in));
            signInPref.setOnPreferenceClickListener(signInPreferenceClickListener);
            wpComCategory.addPreference(signInPref);

            PreferenceScreen rootScreen = (PreferenceScreen) findPreference("wp_pref_root");
            rootScreen.removePreference(mNotificationsGroup);
        }
    }

    private void addWpComShowHideButton(PreferenceCategory wpComCategory, int order) {
        if (WordPress.wpDB.getNumDotComAccounts() > 0) {
            Preference manageBlogPreference = new Preference(getActivity());
            manageBlogPreference.setTitle(R.string.show_and_hide_blogs);
            Intent intentManage = new Intent(getActivity(), ManageBlogsActivity.class);
            manageBlogPreference.setIntent(intentManage);
            manageBlogPreference.setOrder(order);
            wpComCategory.addPreference(manageBlogPreference);
        }
    }

    private void addAccounts(PreferenceCategory category, List<Map<String, Object>> blogs, int order) {
        for (Map<String, Object> account : blogs) {
            String blogName = StringUtils.unescapeHTML(account.get("blogName").toString());
            int accountId = (Integer) account.get("id");

            Preference blogSettingsPreference = new Preference(getActivity());
            blogSettingsPreference.setTitle(blogName);

            try {
                // set blog hostname as preference summary if it differs from the blog name
                URL blogUrl = new URL(account.get("url").toString());
                if (!blogName.equals(blogUrl.getHost())) {
                    blogSettingsPreference.setSummary(blogUrl.getHost());
                }
            } catch (MalformedURLException e) {
                // do nothing
            }

            Intent intent = new Intent(getActivity(), BlogPreferencesActivity.class);
            intent.putExtra("id", accountId);
            blogSettingsPreference.setIntent(intent);
            blogSettingsPreference.setOrder(order++);
            category.addPreference(blogSettingsPreference);
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

    private void loadNotifications() {
        AppLog.d(T.NOTIFS, "Preferences > loading notification settings");

        // Add notifications group back in case it was previously removed from being logged out
        PreferenceScreen rootScreen = (PreferenceScreen) findPreference("wp_pref_root");
        rootScreen.addPreference(mNotificationsGroup);
        PreferenceCategory notificationTypesCategory = (PreferenceCategory) findPreference(
                "wp_pref_notification_types");
        notificationTypesCategory.removeAll();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        String settingsJson = settings.getString(NotificationsUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, null);
        if (settingsJson == null) {
            rootScreen.removePreference(mNotificationsGroup);
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

            PreferenceCategory selectBlogsCategory = (PreferenceCategory) findPreference("wp_pref_notification_blogs");
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

        CheckBoxPreference notificationsEnabledCheckBox = (CheckBoxPreference) findPreference("wp_pref_notifications_enabled");
        notificationsEnabledCheckBox.setOnPreferenceChangeListener(mNotificationsEnabledChangeListener);
    }

    private final OnPreferenceClickListener signInPreferenceClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent i = new Intent(getActivity(), SignInActivity.class);
            i.putExtra("wpcom", true);
            i.putExtra("auth-only", true);
            startActivityForResult(i, 0);
            return true;
        }
    };

    private final OnPreferenceClickListener resetAUtoSharePreferenceClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Editor editor = mSettings.edit();
            editor.remove(ShareIntentReceiverActivity.SHARE_IMAGE_BLOG_ID_KEY);
            editor.remove(ShareIntentReceiverActivity.SHARE_IMAGE_ADDTO_KEY);
            editor.remove(ShareIntentReceiverActivity.SHARE_TEXT_BLOG_ID_KEY);
            editor.commit();
            ToastUtils.showToast(getActivity(), R.string.auto_sharing_preference_reset, ToastUtils.Duration.SHORT);
            return true;
        }
    };

    private final OnPreferenceClickListener signOutPreferenceClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setMessage(getString(R.string.sign_out_confirm));
            dialogBuilder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // set the result code so caller knows the user signed out
                            getActivity().setResult(SettingsActivity.RESULT_SIGNED_OUT);
                            WordPress.signOutAsyncWithProgressBar(getActivity(), new SignOutCallback() {
                                @Override
                                public void onSignOut() {
                                    getActivity().finish();
                                }
                            });
                        }
                    });
            dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            // Just close the window.
                        }
                    });
            dialogBuilder.setCancelable(true);
            dialogBuilder.create().show();
            return true;
        }
    };

    public void onStop() {
        super.onStop();
        if (mNotificationSettingsChanged) {
            sendNotificationsSettings();
        }
    }
}
