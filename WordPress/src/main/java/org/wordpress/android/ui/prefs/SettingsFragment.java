package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
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

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPress.SignOutAsync.SignOutCallback;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.accounts.HelpActivity;
import org.wordpress.android.ui.accounts.ManageBlogsActivity;
import org.wordpress.android.ui.accounts.NewBlogActivity;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPEditTextPreference;
import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.passcodelock.PasscodePreferencesActivity;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;

@SuppressWarnings("deprecation")
public class SettingsFragment extends PreferenceFragment {
    private SharedPreferences mSettings;
    private WPEditTextPreference mTaglineTextPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.OPENED_SETTINGS);
        }

        addPreferencesFromResource(R.xml.settings);
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
        findPreference("wp_pref_manage_notifications").setOnPreferenceClickListener(
                notificationPreferenceClickListener);
        findPreference("wp_pref_app_about").setOnPreferenceClickListener(launchActivitiyClickListener);
        findPreference("wp_pref_open_source_licenses").setOnPreferenceClickListener(launchActivitiyClickListener);
        findPreference("wp_pref_help_and_support").setOnPreferenceClickListener(launchActivitiyClickListener);
        findPreference("wp_pref_passlock_enabled").setOnPreferenceChangeListener(passcodeCheckboxChangeListener);
        findPreference("wp_pref_sign_out").setOnPreferenceClickListener(signOutPreferenceClickListener);
        findPreference("wp_reset_share_pref").setOnPreferenceClickListener(resetAUtoSharePreferenceClickListener);

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
        if (WordPress.hasDotComToken(getActivity())) {
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
        getActivity().setTitle(R.string.settings);

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

    private void addWpComSignIn(PreferenceCategory wpComCategory, int order) {
        if (WordPress.hasDotComToken(getActivity())) {
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
        } else {
            Preference signInPref = new Preference(getActivity());
            signInPref.setTitle(getString(R.string.sign_in));
            signInPref.setOnPreferenceClickListener(signInPreferenceClickListener);
            wpComCategory.addPreference(signInPref);

            PreferenceScreen rootScreen = (PreferenceScreen) findPreference("wp_pref_root");
            PreferenceGroup notificationsGroup = (PreferenceGroup) findPreference("wp_pref_notifications_category");
            if (notificationsGroup != null) {
                rootScreen.removePreference(notificationsGroup);
            }
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

    private final OnPreferenceClickListener notificationPreferenceClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            getFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NotificationSettingsFragment())
                    .addToBackStack(null)
                    .commit();
            return true;
        }
    };

    private final OnPreferenceClickListener launchActivitiyClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Class activityToStart = HelpActivity.class;
            if ("wp_pref_app_about".equals(preference.getKey())) {
                activityToStart = AboutActivity.class;
            } else if ("wp_pref_open_source_licenses".equals(preference.getKey())) {
                activityToStart = LicensesActivity.class;
            } else if ("wp_pref_help_and_support".equals(preference.getKey())) {
                activityToStart = HelpActivity.class;
            }
            startActivity(new Intent(getActivity(), activityToStart));
            return true;
        }
    };

    private final OnPreferenceChangeListener passcodeCheckboxChangeListener = new OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object newValue) {
            startActivity(new Intent(getActivity(), PasscodePreferencesActivity.class));
            return true;
        }
    };
}
