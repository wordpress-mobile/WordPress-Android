package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.v4.text.BidiFormatter;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.ui.ShareIntentReceiverActivity;
import org.wordpress.android.util.ActivityUtils;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPEditTextPreference;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@SuppressWarnings("deprecation")
public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener {
    public static final String LANGUAGE_PREF_KEY = "language-pref";
    public static final int LANGUAGE_CHANGED = 1000;

    private AlertDialog mDialog;
    private SharedPreferences mSettings;
    private WPEditTextPreference mTaglineTextPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        mTaglineTextPreference = (WPEditTextPreference) findPreference(getString(R.string.pref_key_post_sig));
        if (mTaglineTextPreference != null) {
            mTaglineTextPreference.setOnPreferenceChangeListener(preferenceChangeListener);
        }

        findPreference(getString(R.string.pref_key_language))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_app_about))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_oss_licenses))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_reset_shared_pref))
                .setOnPreferenceClickListener(this);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        updatePostSignature();
    }

    @Override
    public void onPause() {
        super.onPause();

        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().setTitle(R.string.settings);
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String preferenceKey = preference != null ? preference.getKey() : "";

        if (preferenceKey.equals(getString(R.string.pref_key_app_about))) {
            return handleAboutPreferenceClick();
        } else if (preferenceKey.equals(getString(R.string.pref_key_oss_licenses))) {
            return handleOssPreferenceClick();
        } else if (preferenceKey.equals(getString(R.string.pref_key_language))) {
            return handleLanguagePreferenceClick();
        } else if (preferenceKey.equals(getString(R.string.pref_key_reset_shared_pref))) {
            return handleResetAutoSharePreferencesClick();
        }

        return false;
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

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
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

    private boolean handleResetAutoSharePreferencesClick() {
        Editor editor = mSettings.edit();
        editor.remove(ShareIntentReceiverActivity.SHARE_IMAGE_BLOG_ID_KEY);
        editor.remove(ShareIntentReceiverActivity.SHARE_IMAGE_ADDTO_KEY);
        editor.remove(ShareIntentReceiverActivity.SHARE_TEXT_BLOG_ID_KEY);
        editor.apply();
        ToastUtils.showToast(getActivity(), R.string.auto_sharing_preference_reset, ToastUtils.Duration.SHORT);
        return true;
    }

    private boolean handleLanguagePreferenceClick() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(getActivity());
        dialogBuilder.setTitle(getString(R.string.language));

        String[] availableLocales = getResources().getStringArray(R.array.available_languages);
        final String[] values = new String[availableLocales.length + 1];
        final Map<String, String> localeMap = new HashMap<>();

        BidiFormatter bidiFormatter = BidiFormatter.getInstance(getResources().getConfiguration().locale);

        for (int i = 0; i < availableLocales.length; ++i) {
            String localString = availableLocales[i];
            if (localString.contains("-")) {
                localString = localString.substring(0, localString.indexOf("-"));
            }
            Locale locale = new Locale(localString);

            // language names localised using their own locale
            // with direction (ltr or rtl) set based on currently selected locale
            String localisedLanguageName = StringUtils.capitalize(locale.getDisplayLanguage(locale));
            values[i + 1] = bidiFormatter.unicodeWrap(localisedLanguageName) + " (" + availableLocales[i] + ")";
            localeMap.put(values[i + 1], availableLocales[i]);
        }
        values[0] = getActivity().getString(R.string.device) + " (" + Locale.getDefault().getLanguage() + ")";
        localeMap.put(values[0], Locale.getDefault().getLanguage());
        // Sorted array will always start with the default "Device (xx)" entry
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

                if (position > 0) {
                    mSettings.edit().putString(LANGUAGE_PREF_KEY, localeMap.get(values[position])).apply();
                } else {
                    // remove custom locale key when original device locale is selected
                    mSettings.edit().remove(LANGUAGE_PREF_KEY).apply();
                }


                // Track the change only if the user selected a non default Device language. This is only used in
                // Mixpanel, because we have both the device language and app selected language data in Tracks
                // metadata.
                if (position != 0) {
                    Map<String, Object> properties = new HashMap<String, Object>();
                    properties.put("forced_app_locale", conf.locale.toString());
                    AnalyticsTracker.track(Stat.ACCOUNT_SETTINGS_LANGUAGE_SELECTION_FORCED, properties);
                }

                // Language is now part of metadata, so we need to refresh them
                AnalyticsUtils.refreshMetadata();

                // Refresh the app
                Intent refresh = new Intent(getActivity(), getActivity().getClass());
                startActivity(refresh);
                getActivity().setResult(LANGUAGE_CHANGED);
                getActivity().finish();
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

    private boolean handleAboutPreferenceClick() {
        startActivity(new Intent(getActivity(), AboutActivity.class));
        return true;
    }

    private boolean handleOssPreferenceClick() {
        startActivity(new Intent(getActivity(), LicensesActivity.class));
        return true;
    }
}
