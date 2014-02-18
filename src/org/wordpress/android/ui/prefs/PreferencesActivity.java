package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
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
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewParent;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.VolleyError;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.internal.StringMap;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.accounts.ManageBlogsActivity;
import org.wordpress.android.ui.accounts.NewBlogActivity;
import org.wordpress.android.ui.accounts.WelcomeActivity;
import org.wordpress.android.ui.notifications.NotificationUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.MapUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPEditTextPreference;
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
public class PreferencesActivity extends SherlockPreferenceActivity {
    private ArrayList<StringMap<Double>> mMutedBlogsList;
    private Map<String, Object> mNotificationSettings;
    private SharedPreferences mSettings;
    private boolean mNotificationSettingsChanged;

    private PreferenceGroup mNotificationsGroup;
    WPEditTextPreference mTaglineTextPreference;

    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        overridePendingTransition(R.anim.slide_up, R.anim.do_nothing);

        setTitle(getResources().getText(R.string.settings));

        ActionBar ab = getSupportActionBar();
        ab.setDisplayHomeAsUpEnabled(true);

        addPreferencesFromResource(R.xml.preferences);

        mNotificationsGroup = (PreferenceGroup)findPreference("wp_pref_notifications_category");

        OnPreferenceChangeListener preferenceChangeListener = new OnPreferenceChangeListener() {
            @Override
            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (newValue != null) { // cancelled dismiss keyboard
                    preference.setSummary(newValue.toString());
                }
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getListView().getWindowToken(), 0);
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

        mSettings = PreferenceManager.getDefaultSharedPreferences(this);

        // Request notification settings if needed
        if (WordPress.hasValidWPComCredentials(PreferencesActivity.this)) {
            String settingsJson = mSettings.getString(NotificationUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, null);
            if (settingsJson == null) {
                com.wordpress.rest.RestRequest.Listener listener = new RestRequest.Listener() {
                    @Override
                    public void onResponse(JSONObject jsonObject) {
                        AppLog.d(T.NOTIFS, "Get settings action succeeded");
                        Editor editor = mSettings.edit();
                        try {
                            JSONObject settingsJSON = jsonObject.getJSONObject("settings");
                            editor.putString(NotificationUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, settingsJSON.toString());
                            editor.commit();
                        } catch (JSONException e) {
                            AppLog.e(T.NOTIFS, "Can't parse the JSON object returned from the server that contains PN settings.", e);
                        }
                        refreshWPComAuthCategory();
                    }
                };
                RestRequest.ErrorListener errorListener = new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        AppLog.e(T.NOTIFS, "Get settings action failed", volleyError);                    }
                };
                NotificationUtils.getPushNotificationSettings(PreferencesActivity.this, listener, errorListener);
            }
        }

        //Passcode Lock not supported
        if( AppLockManager.getInstance().isAppLockFeatureEnabled() == false ) {
            PreferenceScreen rootScreen = (PreferenceScreen)findPreference("wp_pref_root");
            PreferenceGroup passcodeGroup = (PreferenceGroup)findPreference("wp_passcode_lock_category");
            rootScreen.removePreference(passcodeGroup);
        } else {
            final CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference("wp_pref_passlock_enabled");
            //disable on-click changes on the property
            passcodeEnabledCheckBoxPreference.setOnPreferenceClickListener(
                    new OnPreferenceClickListener() {
                        @Override
                        public boolean onPreferenceClick(Preference preference) {
                            passcodeEnabledCheckBoxPreference.setChecked( AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked() );
                            return false;
                        }
                    }
                    );
        }

        displayPreferences();
    }

    private void hidePostSignatureCategory() {
        PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference("wp_pref_root");
        PreferenceCategory postSignature = (PreferenceCategory) findPreference("wp_post_signature");
        if (preferenceScreen != null && postSignature != null) {
            preferenceScreen.removePreference(postSignature);
        }
    }

    private void hideNotificationBlogsCategory() {
        PreferenceScreen preferenceScreen = (PreferenceScreen)
                findPreference("wp_pref_notifications");
        PreferenceCategory blogs = (PreferenceCategory)
                findPreference("wp_pref_notification_blogs");
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
        if( AppLockManager.getInstance().isAppLockFeatureEnabled() ) {
            CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference("wp_pref_passlock_enabled");
            if ( AppLockManager.getInstance().getCurrentAppLock().isPasswordLocked() ) {
                passcodeEnabledCheckBoxPreference.setChecked(true);
            } else {
                passcodeEnabledCheckBoxPreference.setChecked(false);
            }
        }
    }

    @Override
    protected void onPause() {
        overridePendingTransition(R.anim.do_nothing, R.anim.slide_down);
        setResult(RESULT_OK);
        super.onPause();
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference)
    {
        super.onPreferenceTreeClick(preferenceScreen, preference);
        // Workaround for Action Bar Home Button not functional with nested PreferenceScreen
        if (preference!=null && preference instanceof PreferenceScreen) {
            // If the user has clicked on a preference screen, set up the action bar
            if (preference instanceof PreferenceScreen) {
                initializeActionBar((PreferenceScreen) preference);
            }
        }
        return false;
    }
    
    /** Sets up the action bar for an {@link PreferenceScreen} */
    public static void initializeActionBar(PreferenceScreen preferenceScreen) {
        if (android.os.Build.VERSION.SDK_INT < 11)
            return;
       
        final Dialog dialog = preferenceScreen.getDialog();

        if (dialog != null) {
            // Initialize the action bar
            dialog.getActionBar().setDisplayHomeAsUpEnabled(true);

            // Apply custom home button area click listener to close the PreferenceScreen because PreferenceScreens are dialogs which swallow
            // events instead of passing to the activity
            // Related Issue: https://code.google.com/p/android/issues/detail?id=4611
            View homeBtn = dialog.findViewById(android.R.id.home);

            if (homeBtn != null) {
                OnClickListener dismissDialogClickListener = new OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dialog.dismiss();
                    }
                };

                // Prepare yourselves for some hacky programming
                ViewParent homeBtnContainer = homeBtn.getParent();

                // The home button is an ImageView inside a FrameLayout
                if (homeBtnContainer instanceof FrameLayout) {
                    ViewGroup containerParent = (ViewGroup) homeBtnContainer.getParent();

                    if (containerParent instanceof LinearLayout) {
                        // This view also contains the title text, set the whole view as clickable
                        ((LinearLayout) containerParent).setOnClickListener(dismissDialogClickListener);
                    } else {
                        // Just set it on the home button
                        ((FrameLayout) homeBtnContainer).setOnClickListener(dismissDialogClickListener);
                    }
                } else {
                    // The 'If all else fails' default case
                    homeBtn.setOnClickListener(dismissDialogClickListener);
                }
            }    
        }
    }
    
    /**
     * Update the "wpcom blogs" preference category to contain a preference for each blog to configure
     * blog-specific settings.
     */
    protected void updateSelfHostedBlogsPreferenceCategory() {
        PreferenceCategory blogsCategory = (PreferenceCategory) findPreference("wp_pref_self_hosted_blogs");
        blogsCategory.removeAll();
        int order = 0;

        // Add self-hosted blog button
        Preference addBlogPreference = new Preference(this);
        addBlogPreference.setTitle(R.string.add_self_hosted_blog);
        Intent intentWelcome = new Intent(this, WelcomeActivity.class);
        intentWelcome.putExtra(WelcomeActivity.START_FRAGMENT_KEY,
                WelcomeActivity.ADD_SELF_HOSTED_BLOG);
        addBlogPreference.setIntent(intentWelcome);
        addBlogPreference.setOrder(order++);
        blogsCategory.addPreference(addBlogPreference);

        // Add self hosted list
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccountsBy("dotcomFlag=0", null);
        addAccounts(blogsCategory, accounts, order);
    }

    protected int getEnabledBlogsCount() {
        PreferenceScreen selectBlogsCategory = (PreferenceScreen) findPreference("wp_pref_notification_blogs");
        int enabledBlogCtr = 0;
        for (int i = 0; i < selectBlogsCategory.getPreferenceCount(); i++) {
            CheckBoxPreference blogPreference = (CheckBoxPreference) selectBlogsCategory.getPreference(i);
            if (blogPreference.isChecked())
                enabledBlogCtr++;
        }
        return enabledBlogCtr;
    }

    public void displayPreferences() {
        // Post signature
        if (WordPress.wpDB.getNumVisibleAccounts() == 0) {
            hidePostSignatureCategory();
            hideNotificationBlogsCategory();
        } else {
            if (mTaglineTextPreference.getText() == null || mTaglineTextPreference.getText().equals("")) {
                if (DeviceUtils.getInstance().isBlackBerry()) {
                    mTaglineTextPreference.setSummary(R.string.posted_from_blackberry);
                    mTaglineTextPreference.setText(getString(R.string.posted_from_blackberry));
                } else {
                    mTaglineTextPreference.setSummary(R.string.posted_from);
                    mTaglineTextPreference.setText(getString(R.string.posted_from));
                }
            } else {
                mTaglineTextPreference.setSummary(mTaglineTextPreference.getText());
            }
        }

        if (DeviceUtils.getInstance().isBlackBerry()) {
            PreferenceCategory appAboutSectionName = (PreferenceCategory) findPreference("wp_pref_app_about_section");
            appAboutSectionName.setTitle(getString(R.string.app_title_blackberry));
            Preference appName = (Preference) findPreference("wp_pref_app_title");
            appName.setTitle(getString(R.string.app_title_blackberry));
        }
    }

    /**
     * Listens for changes to notification type settings
     */
    private OnPreferenceChangeListener mTypeChangeListener = new OnPreferenceChangeListener() {
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
    private OnPreferenceChangeListener mMuteBlogChangeListener = new OnPreferenceChangeListener() {

        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            if (preference instanceof CheckBoxPreference) {
                CheckBoxPreference checkBoxPreference = (CheckBoxPreference) preference;
                boolean isChecked = (Boolean) newValue;
                int id = checkBoxPreference.getOrder();
                StringMap<Double> blogMap = (StringMap<Double>) mMutedBlogsList.get(id);
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
    private OnPreferenceChangeListener mNotificationsEnabledChangeListener = new OnPreferenceChangeListener() {

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
                    final Dialog dialog = new Dialog(PreferencesActivity.this);
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
        StringMap<String> muteUntilMap = (StringMap<String>) mNotificationSettings
                .get("mute_until");
        if (muteUntilMap != null) {
            if (v.getId() == R.id.notificationsOff) {
                muteUntilMap.put("value", "forever");
            } else if (v.getId() == R.id.notifications1Hour) {
                muteUntilMap.put("value",
                        String.valueOf((System.currentTimeMillis() / 1000) + 3600));
            } else if (v.getId() == R.id.notifications8Hours) {
                muteUntilMap.put("value",
                        String.valueOf((System.currentTimeMillis() / 1000) + (3600 * 8)));
            }
            CheckBoxPreference enabledCheckBoxPreference = (CheckBoxPreference) findPreference("wp_pref_notifications_enabled");
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
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.this);
                SharedPreferences.Editor editor = settings.edit();
                Gson gson = new Gson();
                String settingsJson = gson.toJson(mNotificationSettings);
                editor.putString(NotificationUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, settingsJson);
                editor.commit();
                NotificationUtils.setPushNotificationSettings(PreferencesActivity.this);
            }
            return null;
        }

        @Override
        protected void onPostExecute(Void result) {
            mNotificationSettingsChanged = false;
        }
    }

    private void addWpComSignIn(PreferenceCategory wpComCategory, int order) {
        if (WordPress.hasValidWPComCredentials(PreferencesActivity.this)) {
            String username = mSettings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
            Preference usernamePref = new Preference(this);
            usernamePref.setTitle(getString(R.string.username));
            usernamePref.setSummary(username);
            usernamePref.setSelectable(false);
            usernamePref.setOrder(order);
            wpComCategory.addPreference(usernamePref);

            Preference createWPComBlogPref = new Preference(this);
            createWPComBlogPref.setTitle(getString(R.string.create_new_blog_wpcom));
            Intent intent = new Intent(this, NewBlogActivity.class);
            createWPComBlogPref.setIntent(intent);
            createWPComBlogPref.setOrder(order + 1);
            wpComCategory.addPreference(createWPComBlogPref);

            loadNotifications();
        } else {
            Preference signInPref = new Preference(this);
            signInPref.setTitle(getString(R.string.sign_in));
            signInPref.setOnPreferenceClickListener(signInPreferenceClickListener);
            wpComCategory.addPreference(signInPref);

            PreferenceScreen rootScreen = (PreferenceScreen)findPreference("wp_pref_root");
            rootScreen.removePreference(mNotificationsGroup);
        }
    }

    private void addWpComShowHideButton(PreferenceCategory wpComCategory, int order) {
        if (WordPress.wpDB.getNumDotComAccounts() > 0) {
            Preference manageBlogPreference = new Preference(this);
            manageBlogPreference.setTitle(R.string.show_and_hide_blogs);
            Intent intentManage = new Intent(this, ManageBlogsActivity.class);
            manageBlogPreference.setIntent(intentManage);
            manageBlogPreference.setOrder(order);
            wpComCategory.addPreference(manageBlogPreference);
        }
    }

    private void addAccounts(PreferenceCategory category, List<Map<String, Object>> blogs, int order) {
        for (Map<String, Object> account : blogs) {
            String blogName = StringUtils.unescapeHTML(account.get("blogName").toString());
            int accountId = (Integer) account.get("id");

            Preference blogSettingsPreference = new Preference(this);
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

            Intent intent = new Intent(this, BlogPreferencesActivity.class);
            intent.putExtra("id", accountId);
            blogSettingsPreference.setIntent(intent);
            blogSettingsPreference.setOrder(order++);
            category.addPreference(blogSettingsPreference);
        }
    }

    private void refreshWPComAuthCategory() {
        PreferenceCategory wpComCategory = (PreferenceCategory) findPreference("wp_pref_wpcom");
        wpComCategory.removeAll();
        addWpComSignIn(wpComCategory, 0);
        addWpComShowHideButton(wpComCategory, 5);
        List<Map<String, Object>> accounts = WordPress.wpDB.getAccountsBy("dotcomFlag = 1 AND isHidden = 0", null);
        addAccounts(wpComCategory, accounts, 10);
    }

    private static Comparator<StringMap<?>> BlogNameComparatorForMutedBlogsList = new Comparator<StringMap<?>>() {
        public int compare(StringMap<?> blog1, StringMap<?> blog2) {
            StringMap<?> blogMap1 = (StringMap<?>)blog1;
            StringMap<?> blogMap2 = (StringMap<?>)blog2;

            String blogName1 = blogMap1.get("blog_name").toString();
            if (blogName1.length() == 0) {
                blogName1 = blogMap1.get("url").toString();
            }

            String blogName2 = blogMap2.get("blog_name").toString();
            if (blogName2.length() == 0) {
                blogName2 = blogMap2.get("url").toString();
            }

          return blogName1.compareToIgnoreCase(blogName2);

        }

    };

    private void loadNotifications() {
        AppLog.d(T.NOTIFS, "Preferences > loading notification settings");

        // Add notifications group back in case it was previously removed from being logged out
        PreferenceScreen rootScreen = (PreferenceScreen)findPreference("wp_pref_root");
        rootScreen.addPreference(mNotificationsGroup);
        PreferenceCategory notificationTypesCategory = (PreferenceCategory) findPreference("wp_pref_notification_types");
        notificationTypesCategory.removeAll();
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);

        String settingsJson = settings.getString(NotificationUtils.WPCOM_PUSH_DEVICE_NOTIFICATION_SETTINGS, null);
        if (settingsJson == null) {
            rootScreen.removePreference(mNotificationsGroup);
            return;
        } else {
            try {
                Gson gson = new Gson();
                mNotificationSettings = gson.fromJson(settingsJson, HashMap.class);
                StringMap<?> mutedBlogsMap = (StringMap<?>) mNotificationSettings.get("muted_blogs");
                mMutedBlogsList = (ArrayList<StringMap<Double>>) mutedBlogsMap.get("value");
                Collections.sort(mMutedBlogsList, this.BlogNameComparatorForMutedBlogsList);

                Object[] mTypeList = mNotificationSettings.keySet().toArray();

                for (int i = 0; i < mTypeList.length; i++) {
                    if (!mTypeList[i].equals("muted_blogs") && !mTypeList[i].equals("mute_until")) {
                        StringMap<?> typeMap = (StringMap<?>) mNotificationSettings
                                .get(mTypeList[i].toString());
                        CheckBoxPreference typePreference = new CheckBoxPreference(this);
                        typePreference.setKey(mTypeList[i].toString());
                        typePreference.setChecked(MapUtils.getMapBool(typeMap, "value"));
                        typePreference.setTitle(typeMap.get("desc").toString());
                        typePreference.setOnPreferenceChangeListener(mTypeChangeListener);
                        notificationTypesCategory.addPreference(typePreference);
                    }
                }

                PreferenceCategory selectBlogsCategory = (PreferenceCategory) findPreference("wp_pref_notification_blogs");
                selectBlogsCategory.removeAll();
                for (int i = 0; i < mMutedBlogsList.size(); i++) {
                    StringMap<?> blogMap = (StringMap<?>) mMutedBlogsList.get(i);
                    String blogName = (String) blogMap.get("blog_name");
                    if (blogName == null || blogName.trim().equals(""))
                        blogName = (String) blogMap.get("url");
                    CheckBoxPreference blogPreference = new CheckBoxPreference(this);
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
    }

    private OnPreferenceClickListener signInPreferenceClickListener = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent i = new Intent(PreferencesActivity.this, WelcomeActivity.class);
            i.putExtra("wpcom", true);
            i.putExtra("auth-only", true);
            startActivityForResult(i, 0);
            return true;
        }
    };

    private OnPreferenceClickListener resetAUtoSharePreferenceClickListener =
            new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Editor editor = mSettings.edit();
            editor.remove(ShareIntentReceiverActivity.SHARE_IMAGE_BLOG_ID_KEY);
            editor.remove(ShareIntentReceiverActivity.SHARE_IMAGE_ADDTO_KEY);
            editor.remove(ShareIntentReceiverActivity.SHARE_TEXT_BLOG_ID_KEY);
            editor.commit();
            ToastUtils.showToast(getBaseContext(), R.string.auto_sharing_preference_reset,
                    ToastUtils.Duration.SHORT);
            return true;
        }
    };

    private OnPreferenceClickListener signOutPreferenceClickListener = new OnPreferenceClickListener() {

        @Override
        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(PreferencesActivity.this);
            dialogBuilder.setTitle(getResources().getText(R.string.sign_out));
            dialogBuilder.setMessage(getString(R.string.sign_out_confirm));
            dialogBuilder.setPositiveButton(R.string.sign_out,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            WordPress.signOut(PreferencesActivity.this);
                            finish();
                        }
                    });
            dialogBuilder.setNegativeButton(R.string.cancel,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog,
                                            int whichButton) {
                            // Just close the window.
                        }
                    });
            dialogBuilder.setCancelable(true);
            if (!isFinishing())
                dialogBuilder.create().show();
            return true;
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        refreshWPComAuthCategory();
        super.onActivityResult(requestCode, resultCode, data);
    }

    public void onStop() {
        super.onStop();
        if (mNotificationSettingsChanged) {
            sendNotificationsSettings();
        }
    }
}
