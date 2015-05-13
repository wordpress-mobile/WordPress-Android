package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
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
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.ui.notifications.utils.NotificationsUtils;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPEditTextPreference;
import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.passcodelock.PasscodePreferencesActivity;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("deprecation")
public class SettingsFragment extends PreferenceFragment {
    public static final String SETTINGS_PREFERENCES = "settings-pref";

    private AlertDialog mDialog;
    private SharedPreferences mSettings;
    private WPEditTextPreference mTaglineTextPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Resources resources = getResources();

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

        mTaglineTextPreference = (WPEditTextPreference) findPreference(resources.getString(R.string.pref_key_post_sig));
        if (mTaglineTextPreference != null) {
            mTaglineTextPreference.setOnPreferenceChangeListener(preferenceChangeListener);
        }
        findPreference(resources.getString(R.string.pref_key_notifications)).setOnPreferenceClickListener(notificationPreferenceClickListener);
        findPreference(resources.getString(R.string.pref_key_language)).setOnPreferenceClickListener(languagePreferenceClickListener);
        findPreference(resources.getString(R.string.pref_key_app_about)).setOnPreferenceClickListener(launchActivityClickListener);
        findPreference(resources.getString(R.string.pref_key_oss_licenses)).setOnPreferenceClickListener(launchActivityClickListener);
        findPreference(resources.getString(R.string.pref_key_passlock)).setOnPreferenceChangeListener(passcodeCheckboxChangeListener);
        findPreference(resources.getString(R.string.pref_key_reset_shared_pref)).setOnPreferenceClickListener(resetAutoSharePreferenceClickListener);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        initNotifications();

        // Passcode Lock not supported
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            final CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference(
                    resources.getString(R.string.pref_key_passlock));
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
            PreferenceScreen rootScreen = (PreferenceScreen) findPreference(resources.getString(R.string.pref_key_settings_root));
            PreferenceGroup passcodeGroup = (PreferenceGroup) findPreference(resources.getString(R.string.pref_key_passlock_section));
            rootScreen.removePreference(passcodeGroup);
        }
        updatePostSignature();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    private void initNotifications() {
        if (!AccountHelper.isSignedInWordPressDotCom()) {
            hideManageNotificationCategory();
            return;
        }
        // Request notification settings if needed
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
                        editor.apply();
                    } catch (JSONException e) {
                        AppLog.e(T.NOTIFS, "Can't parse PN settings from server response", e);
                    }
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

    private void hideManageNotificationCategory() {
        PreferenceScreen preferenceScreen =
                (PreferenceScreen) findPreference(getActivity().getString(R.string.pref_key_settings_root));
        PreferenceCategory notifs =
                (PreferenceCategory) findPreference(getActivity().getString(R.string.pref_key_notifications_section));
        if (preferenceScreen != null && notifs != null) {
            preferenceScreen.removePreference(notifs);
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getActivity().setTitle(R.string.settings);

        //update Passcode lock row if available
        if (AppLockManager.getInstance().isAppLockFeatureEnabled()) {
            CheckBoxPreference passcodeEnabledCheckBoxPreference = (CheckBoxPreference) findPreference(getResources().getString(R.string.pref_key_passlock));
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
                if (getActivity() instanceof SettingsActivity) {
                    ((SettingsActivity) getActivity()).checkForBlogChangeAndFinish();
                } else {
                    getActivity().finish();
                }
        }
        return super.onOptionsItemSelected(item);
    }

    private void updatePostSignature() {
        if (mTaglineTextPreference.getText() == null || mTaglineTextPreference.getText().equals("")) {
            mTaglineTextPreference.setSummary(R.string.posted_from);
            mTaglineTextPreference.setText(getString(R.string.posted_from));
        } else {
            mTaglineTextPreference.setSummary(mTaglineTextPreference.getText());
        }
    }

    private final OnPreferenceClickListener resetAutoSharePreferenceClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Editor editor = mSettings.edit();
            editor.remove(ShareIntentReceiverActivity.SHARE_IMAGE_BLOG_ID_KEY);
            editor.remove(ShareIntentReceiverActivity.SHARE_IMAGE_ADDTO_KEY);
            editor.remove(ShareIntentReceiverActivity.SHARE_TEXT_BLOG_ID_KEY);
            editor.apply();
            ToastUtils.showToast(getActivity(), R.string.auto_sharing_preference_reset, ToastUtils.Duration.SHORT);
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

    private final OnPreferenceClickListener languagePreferenceClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
            dialogBuilder.setTitle(getString(R.string.language));

            String[] availableLocales = getResources().getStringArray(R.array.available_languages);
            final String[] values = new String[availableLocales.length + 1];
            final Map<String, String> localeMap = new HashMap<>();

            for (int i = 0; i < availableLocales.length; ++i) {
                String localString = availableLocales[i];
                if (localString.contains("-")) {
                    localString = localString.substring(0, localString.indexOf("-"));
                }
                Locale locale = new Locale(localString);
                values[i + 1] = locale.getDisplayLanguage() + " (" + availableLocales[i] + ")";
                localeMap.put(values[i + 1], availableLocales[i]);
            }
            values[0] = getActivity().getString(R.string.device) + " (" + Locale.getDefault().getLanguage() + ")";
            localeMap.put(values[0], Locale.getDefault().getLanguage());
            Arrays.sort(values, 1, values.length);

            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, values);
            ListView listView = new ListView(getActivity());
            listView.setAdapter(adapter);
            listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                    Resources res = getResources();
                    DisplayMetrics dm = res.getDisplayMetrics();
                    Configuration conf = res.getConfiguration();
                    String localString = localeMap.get(values[position]);
                    if (localString.contains("-")) {
                        conf.locale = new Locale(localString.substring(0, localString.indexOf("-")), localString.substring(localString.indexOf("-") + 1, localString.length()));
                    } else {
                        conf.locale = new Locale(localString);
                    }
                    res.updateConfiguration(conf, dm);

                    mSettings.edit().putString(SETTINGS_PREFERENCES, localeMap.get(values[position])).apply();

                    Intent refresh = new Intent(getActivity(), getActivity().getClass());
                    startActivity(refresh);
                    if (getActivity() instanceof SettingsActivity) {
                        ((SettingsActivity) getActivity()).checkForBlogChangeAndFinish();
                    } else {
                        getActivity().finish();
                    }
                }
            });

            dialogBuilder.setView(listView);
            dialogBuilder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });

            mDialog = dialogBuilder.show();

            return true;
        }
    };

    private final OnPreferenceClickListener launchActivityClickListener = new OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            if (getActivity().getString(R.string.pref_key_oss_licenses).equals(preference.getKey())) {
                startActivity(new Intent(getActivity(), LicensesActivity.class));
                return true;
            } else if (getActivity().getString(R.string.pref_key_app_about).equals(preference.getKey())) {
                startActivity(new Intent(getActivity(), AboutActivity.class));
                return true;
            }
            return false;
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
