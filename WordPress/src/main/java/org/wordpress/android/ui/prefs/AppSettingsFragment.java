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
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.LanguageUtils;
import org.wordpress.android.util.WPMediaUtils;
import org.wordpress.android.util.WPPrefUtils;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

public class AppSettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    public static final String LANGUAGE_PREF_KEY = "language-pref";
    public static final int LANGUAGE_CHANGED = 1000;

    private static final int IDX_LEGACY_EDITOR = 0;
    private static final int IDX_VISUAL_EDITOR = 1;
    private static final int IDX_AZTEC_EDITOR = 2;

    private DetailListPreference mLanguagePreference;
    private SharedPreferences mSettings;
    private Preference mEditorFooterPref;

    // This Device settings
    private WPSwitchPreference mOptimizedImage;
    private DetailListPreference mImageMaxSizePref;
    private DetailListPreference mImageQualityPref;
    private WPSwitchPreference mOptimizedVideo;
    private DetailListPreference mVideoWidthPref;
    private DetailListPreference mVideoEncorderBitratePref;

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

        mEditorFooterPref = findPreference(getString(R.string.pref_key_editor_footer));

        findPreference(getString(R.string.pref_key_language))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_device_settings))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_editor_footer))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_app_about))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_oss_licenses))
                .setOnPreferenceClickListener(this);

        mOptimizedImage =
                (WPSwitchPreference) WPPrefUtils.getPrefAndSetChangeListener(this, R.string.pref_key_optimize_image, this);
        mImageMaxSizePref = (DetailListPreference) WPPrefUtils.getPrefAndSetChangeListener(this, R.string.pref_key_site_image_width, this);
        mImageQualityPref =
                (DetailListPreference) WPPrefUtils.getPrefAndSetChangeListener(this, R.string.pref_key_site_image_quality, this);
        mOptimizedVideo =
                (WPSwitchPreference) WPPrefUtils.getPrefAndSetChangeListener(this, R.string.pref_key_optimize_video, this);
        mVideoWidthPref =
                (DetailListPreference) WPPrefUtils.getPrefAndSetChangeListener(this, R.string.pref_key_site_video_width, this);
        mVideoEncorderBitratePref =
                (DetailListPreference) WPPrefUtils.getPrefAndSetChangeListener(this, R.string.pref_key_site_video_encoder_bitrate, this);

        // Set Local settings
        mOptimizedImage.setChecked(AppPrefs.isImageOptimize());
        setDetailListPreferenceValue(mImageMaxSizePref,
                String.valueOf(AppPrefs.getImageOptimizeMaxSize()),
                getLabelForImageMaxSizeValue(AppPrefs.getImageOptimizeMaxSize()));
        setDetailListPreferenceValue(mImageQualityPref,
                String.valueOf(AppPrefs.getImageOptimizeQuality()),
                getLabelForImageQualityValue(AppPrefs.getImageOptimizeQuality()));

        mOptimizedVideo.setChecked(AppPrefs.isVideoOptimize());
        setDetailListPreferenceValue(mVideoWidthPref,
                String.valueOf(AppPrefs.getVideoOptimizeWidth()),
                getLabelForVideoMaxWidthValue(AppPrefs.getVideoOptimizeWidth()));
        setDetailListPreferenceValue(mVideoEncorderBitratePref,
                String.valueOf(AppPrefs.getVideoOptimizeQuality()),
                getLabelForVideoEncoderBitrateValue(AppPrefs.getVideoOptimizeQuality()));
        if (!WPMediaUtils.isVideoOptimizationAvailable()) {
            WPPrefUtils.removePreference(this, R.string.pref_key_optimize_media, R.string.pref_key_optimize_video);
            WPPrefUtils.removePreference(this, R.string.pref_key_optimize_media, R.string.pref_key_site_video_width);
            WPPrefUtils.removePreference(this, R.string.pref_key_optimize_media, R.string.pref_key_site_video_encoder_bitrate);
        }

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
        } else if (preferenceKey.equals(getString(R.string.pref_key_editor_footer))) {
            return handleEditorFooterPreferenceClick();
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
        } else if (preference == mOptimizedImage) {
            AppPrefs.setImageOptimize((Boolean) newValue);
            mImageMaxSizePref.setEnabled((Boolean) newValue);
            Map<String, Object> properties = new HashMap<>();
            properties.put("enabled", newValue);
            AnalyticsTracker.track(AnalyticsTracker.Stat.SITE_SETTINGS_OPTIMIZE_IMAGES_CHANGED, properties);
        } else if (preference == mImageMaxSizePref) {
            int newWidth = Integer.parseInt(newValue.toString());
            AppPrefs.setImageOptimizeMaxSize(newWidth);
            setDetailListPreferenceValue(mImageMaxSizePref,
                    newValue.toString(),
                    getLabelForImageMaxSizeValue(AppPrefs.getImageOptimizeMaxSize()));
        } else if (preference == mImageQualityPref) {
            AppPrefs.setImageOptimizeQuality(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mImageQualityPref,
                    newValue.toString(),
                    getLabelForImageQualityValue(AppPrefs.getImageOptimizeQuality()));
        } else if (preference == mOptimizedVideo) {
            AppPrefs.setVideoOptimize((Boolean) newValue);
            mVideoEncorderBitratePref.setEnabled((Boolean) newValue);
        } else if (preference == mVideoWidthPref) {
            int newWidth = Integer.parseInt(newValue.toString());
            AppPrefs.setVideoOptimizeWidth(newWidth);
            setDetailListPreferenceValue(mVideoWidthPref,
                    newValue.toString(),
                    getLabelForVideoMaxWidthValue(AppPrefs.getVideoOptimizeWidth()));
        } else if (preference == mVideoEncorderBitratePref) {
            AppPrefs.setVideoOptimizeQuality(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mVideoEncorderBitratePref,
                    newValue.toString(),
                    getLabelForVideoEncoderBitrateValue(AppPrefs.getVideoOptimizeQuality()));
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

            editorTypePreference.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object value) {
                    if (value != null) {
                        int index = Integer.parseInt(value.toString());
                        CharSequence[] entries = editorTypePreference.getEntries();
                        editorTypePreference.setSummary(entries[index]);

                        switch (index) {
                            case IDX_VISUAL_EDITOR:
                                AppPrefs.setAztecEditorEnabled(false);
                                AppPrefs.setVisualEditorEnabled(true);
                                break;
                            case IDX_AZTEC_EDITOR:
                                AppPrefs.setAztecEditorEnabled(true);
                                AppPrefs.setVisualEditorEnabled(false);
                                AppPrefs.setNewEditorPromoRequired(false);
                                break;
                            default:
                                AppPrefs.setAztecEditorEnabled(false);
                                AppPrefs.setVisualEditorEnabled(false);
                                break;
                        }

                        toggleEditorFooterPreference();
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

            toggleEditorFooterPreference();
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

    /*
     * only show the editor footer when Aztec is enabled
     */
    private void toggleEditorFooterPreference() {
        PreferenceCategory editorCategory = (PreferenceCategory) findPreference(getActivity().getString(R.string.pref_key_editor));
        boolean showFooter = AppPrefs.isAztecEditorEnabled();
        boolean isFooterShowing = editorCategory.findPreference(getString(R.string.pref_key_editor_footer)) != null;

        if (showFooter && !isFooterShowing) {
            editorCategory.addPreference(mEditorFooterPref);
        } else if (!showFooter && isFooterShowing) {
            editorCategory.removePreference(mEditorFooterPref);
        }
    }

    private boolean handleEditorFooterPreferenceClick() {
        ActivityLauncher.showAztecEditorReleaseNotes(getActivity());
        return true;
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

    private String getLabelForImageMaxSizeValue(int newValue) {
        String[] values = getActivity().getResources().getStringArray(R.array.site_settings_image_max_size_values);
        String[] entries = getActivity().getResources().getStringArray(R.array.site_settings_image_max_size_entries);
        for (int i = 0; i < values.length ; i++) {
            if (values[i].equals(String.valueOf(newValue))) {
                return entries[i];
            }
        }

        return entries[0];
    }

    private String getLabelForImageQualityValue(int newValue) {
        String[] values = getActivity().getResources().getStringArray(R.array.site_settings_image_quality_values);
        String[] entries = getActivity().getResources().getStringArray(R.array.site_settings_image_quality_entries);
        for (int i = 0; i < values.length ; i++) {
            if (values[i].equals(String.valueOf(newValue))) {
                return entries[i];
            }
        }

        return entries[0];
    }

    private String getLabelForVideoMaxWidthValue(int newValue) {
        String[] values = getActivity().getResources().getStringArray(R.array.site_settings_video_width_values);
        String[] entries = getActivity().getResources().getStringArray(R.array.site_settings_video_width_entries);
        for (int i = 0; i < values.length ; i++) {
            if (values[i].equals(String.valueOf(newValue))) {
                return entries[i];
            }
        }

        return entries[0];
    }

    private String getLabelForVideoEncoderBitrateValue(int newValue) {
        String[] values = getActivity().getResources().getStringArray(R.array.site_settings_video_bitrate_values);
        String[] entries = getActivity().getResources().getStringArray(R.array.site_settings_video_bitrate_entries);
        for (int i = 0; i < values.length ; i++) {
            if (values[i].equals(String.valueOf(newValue))) {
                return entries[i];
            }
        }

        return entries[0];
    }

    private void setDetailListPreferenceValue(DetailListPreference pref, String value, String summary) {
        pref.setValue(value);
        pref.setSummary(summary);
        pref.refreshAdapter();
    }
}
