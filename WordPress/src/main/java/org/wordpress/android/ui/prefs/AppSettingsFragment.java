package org.wordpress.android.ui.prefs;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.text.TextUtils;
import android.util.Pair;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.util.WPPrefUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

public class AppSettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    public static final String LANGUAGE_PREF_KEY = "language-pref";
    public static final int LANGUAGE_CHANGED = 1000;

    private DetailListPreference mLanguagePreference;
    private SharedPreferences mSettings;

    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        setRetainInstance(true);
        addPreferencesFromResource(R.xml.app_settings);

        mLanguagePreference = (DetailListPreference) findPreference(getString(R.string.pref_key_language));
        mLanguagePreference.setOnPreferenceChangeListener(this);

        findPreference(getString(R.string.pref_key_language))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_device_settings))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_app_about))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_oss_licenses))
                .setOnPreferenceClickListener(this);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        updateEditorSettings();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateLanguagePreference(getResources().getConfiguration().locale.toString());
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String preferenceKey = preference != null ? preference.getKey() : "";

        if (preferenceKey.equals(getString(R.string.pref_key_device_settings))) {
            return handleDevicePreferenceClick();
        } else if (preferenceKey.equals(getString(R.string.pref_key_app_about))) {
            return handleAboutPreferenceClick();
        } else if (preferenceKey.equals(getString(R.string.pref_key_oss_licenses))) {
            return handleOssPreferenceClick();
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) return false;

        if (preference == mLanguagePreference) {
            changeLanguage(newValue.toString());
            return false;
        }

        return true;
    }

    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                getActivity().finish();
        }
        return super.onOptionsItemSelected(item);
    }

    private void updateEditorSettings() {
        if (!AppPrefs.isVisualEditorAvailable()) {
            PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference(getActivity()
                    .getString(R.string.pref_key_account_settings_root));
            PreferenceCategory editor = (PreferenceCategory) findPreference(getActivity()
                    .getString(R.string.pref_key_editor));
            if (preferenceScreen != null && editor != null) {
                preferenceScreen.removePreference(editor);
            }
        } else {
            final ListPreference editorTypePreference = (ListPreference) findPreference(getActivity().getString(R.string.pref_key_editor_type));

            // If user has Aztec preference from previous installation and it's not available anymore, don't use it
            if (!AppPrefs.isAztecEditorAvailable() && "2".equals(editorTypePreference.getValue())) {
                if (AppPrefs.isVisualEditorEnabled()) {
                    editorTypePreference.setValue("1");
                } else {
                    editorTypePreference.setValue("0");
                }
            }

            // if Aztec unavailable, only show the old list old of editors
            if (!AppPrefs.isAztecEditorAvailable()) {
                editorTypePreference.setEntries(R.array.editor_entries_without_aztec);
                editorTypePreference.setEntryValues(R.array.editor_values_without_aztec);
            }

            editorTypePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object value) {
                    if (value != null) {
                        int index = Integer.parseInt(value.toString());
                        CharSequence[] entries = editorTypePreference.getEntries();
                        editorTypePreference.setSummary(entries[index]);

                        switch (index) {
                            case 1:
                                AppPrefs.setAztecEditorEnabled(false);
                                AppPrefs.setVisualEditorEnabled(true);
                                break;
                            case 2:
                                AppPrefs.setAztecEditorEnabled(true);
                                AppPrefs.setVisualEditorEnabled(false);
                                break;
                            default:
                                AppPrefs.setAztecEditorEnabled(false);
                                AppPrefs.setVisualEditorEnabled(false);
                                break;
                        }

                        return true;
                    } else {
                        return false;
                    }
                }
            });

            String editorTypeKey = getString(R.string.pref_key_editor_type);
            String editorTypeSetting = mSettings.getString(editorTypeKey, "");

            if (!editorTypeSetting.equalsIgnoreCase("")) {
                CharSequence[] entries = editorTypePreference.getEntries();
                editorTypePreference.setSummary(entries[Integer.parseInt(editorTypeSetting)]);
            }
        }
    }

    private void changeLanguage(String languageCode) {
        if (mLanguagePreference == null || TextUtils.isEmpty(languageCode)) return;

        Resources res = getResources();
        Configuration conf = res.getConfiguration();
        // will return conf.locale if conf is non-null, or Locale.getDefault()
        Locale currentLocale = LanguageUtils.getCurrentDeviceLanguage(WordPress.getContext());
        Locale newLocale = WPPrefUtils.languageLocale(languageCode);

        if (currentLocale.toString().equals(newLocale.getDisplayLanguage())) {
            return;
        }

        if (Locale.getDefault().toString().equals(newLocale.toString())) {
            // remove custom locale key when original device locale is selected
            mSettings.edit().remove(LANGUAGE_PREF_KEY).apply();
        } else {
            mSettings.edit().putString(LANGUAGE_PREF_KEY, newLocale.toString()).apply();
        }
        updateLanguagePreference(languageCode);

        // update configuration
        conf.locale = newLocale;
        res.updateConfiguration(conf, res.getDisplayMetrics());

        // Track language change on Analytics because we have both the device language and app selected language
        // data in Tracks metadata.
        Map<String, Object> properties = new HashMap<>();
        properties.put("app_locale", conf.locale.toString());
        AnalyticsTracker.track(Stat.ACCOUNT_SETTINGS_LANGUAGE_CHANGED, properties);

        // Language is now part of metadata, so we need to refresh them
        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);

        // Refresh the app
        Intent refresh = new Intent(getActivity(), getActivity().getClass());
        startActivity(refresh);
        getActivity().setResult(LANGUAGE_CHANGED);
        getActivity().finish();
    }

    private void updateLanguagePreference(String languageCode) {
        if (mLanguagePreference == null || TextUtils.isEmpty(languageCode)) return;

        Locale languageLocale = WPPrefUtils.languageLocale(languageCode);
        String[] availableLocales = getResources().getStringArray(R.array.available_languages);

        Pair<String[], String[]> pair = WPPrefUtils.createSortedLanguageDisplayStrings(availableLocales, languageLocale);
        // check for a possible NPE
        if (pair == null) return;

        String[] sortedEntries = pair.first;
        String[] sortedValues = pair.second;

        mLanguagePreference.setEntries(sortedEntries);
        mLanguagePreference.setEntryValues(sortedValues);
        mLanguagePreference.setDetails(WPPrefUtils.createLanguageDetailDisplayStrings(sortedValues));

        mLanguagePreference.setValue(languageCode);
        mLanguagePreference.setSummary(WPPrefUtils.getLanguageString(languageCode, languageLocale));
        mLanguagePreference.refreshAdapter();
    }

    private boolean handleAboutPreferenceClick() {
        startActivity(new Intent(getActivity(), AboutActivity.class));
        return true;
    }

    private boolean handleDevicePreferenceClick() {
        try {
            // open specific app info screen
            Intent intent = new Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + getActivity().getPackageName()));
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            AppLog.w(AppLog.T.SETTINGS, exception.getMessage());
            // open generic apps screen
            Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS);
            startActivity(intent);
        }

        return true;
    }

    private boolean handleOssPreferenceClick() {
        startActivity(new Intent(getActivity(), LicensesActivity.class));
        return true;
    }
}
