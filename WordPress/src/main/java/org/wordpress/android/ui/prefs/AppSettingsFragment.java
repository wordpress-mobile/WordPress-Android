package org.wordpress.android.ui.prefs;

import android.app.Dialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.BuildConfig;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.databinding.JetpackBadgeFooterBinding;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.AccountAction;
import org.wordpress.android.fluxc.generated.AccountActionBuilder;
import org.wordpress.android.fluxc.generated.WhatsNewActionBuilder;
import org.wordpress.android.fluxc.model.whatsnew.WhatsNewAnnouncementModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.AccountStore.OnAccountChanged;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.WhatsNewStore.OnWhatsNewFetched;
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewAppId;
import org.wordpress.android.fluxc.store.WhatsNewStore.WhatsNewFetchPayload;
import org.wordpress.android.ui.debug.DebugSettingsActivity;
import org.wordpress.android.ui.deeplinks.DeepLinkOpenWebLinksWithJetpackHelper;
import org.wordpress.android.ui.mysite.jetpackbadge.JetpackPoweredBottomSheetFragment;
import org.wordpress.android.ui.mysite.tabs.MySiteTabType;
import org.wordpress.android.ui.prefs.language.LocalePickerBottomSheet;
import org.wordpress.android.ui.prefs.language.LocalePickerBottomSheet.LocalePickerCallback;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateLogic;
import org.wordpress.android.ui.reader.services.update.ReaderUpdateServiceStarter;
import org.wordpress.android.ui.utils.UiHelpers;
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementDialogFragment;
import org.wordpress.android.ui.whatsnew.FeatureAnnouncementProvider;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppThemeUtils;
import org.wordpress.android.util.BuildConfigWrapper;
import org.wordpress.android.util.JetpackBrandingUtils;
import org.wordpress.android.util.JetpackBrandedScreenData;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.LocaleProvider;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPActivityUtils;
import org.wordpress.android.util.WPPrefUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.config.MySiteDashboardTabsFeatureConfig;
import org.wordpress.android.viewmodel.ContextProvider;

import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

public class AppSettingsFragment extends PreferenceFragment
        implements OnPreferenceClickListener, Preference.OnPreferenceChangeListener, LocalePickerCallback {
    public static final int LANGUAGE_CHANGED = 1000;

    private WPPreference mLanguagePreference;
    private ListPreference mAppThemePreference;
    private ListPreference mInitialScreenPreference;

    // This Device settings
    private WPSwitchPreference mOptimizedImage;
    private DetailListPreference mImageMaxSizePref;
    private DetailListPreference mImageQualityPref;
    private WPSwitchPreference mOptimizedVideo;
    private DetailListPreference mVideoWidthPref;
    private DetailListPreference mVideoEncorderBitratePref;
    private PreferenceScreen mPrivacySettings;
    private WPSwitchPreference mStripImageLocation;
    private WPSwitchPreference mReportCrashPref;
    private WPSwitchPreference mOpenWebLinksWithJetpack;

    private Preference mWhatsNew;

    @Inject SiteStore mSiteStore;
    @Inject AccountStore mAccountStore;
    @Inject Dispatcher mDispatcher;
    @Inject ContextProvider mContextProvider;
    @Inject FeatureAnnouncementProvider mFeatureAnnouncementProvider;
    @Inject BuildConfigWrapper mBuildConfigWrapper;
    @Inject MySiteDashboardTabsFeatureConfig mMySiteDashboardTabsFeatureConfig;
    @Inject JetpackBrandingUtils mJetpackBrandingUtils;
    @Inject LocaleProvider mLocaleProvider;
    @Inject DeepLinkOpenWebLinksWithJetpackHelper mOpenWebLinksWithJetpackHelper;
    @Inject UiHelpers mUiHelpers;

    private static final String TRACK_STYLE = "style";
    private static final String TRACK_ENABLED = "enabled";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);
        mDispatcher.register(this);

        setRetainInstance(true);

        addPreferencesFromResource(R.xml.app_settings);

        findPreference(getString(R.string.pref_key_send_usage)).setOnPreferenceChangeListener(
                new Preference.OnPreferenceChangeListener() {
                    @Override
                    public boolean onPreferenceChange(Preference preference, Object newValue) {
                        if (newValue == null) {
                            return false;
                        }

                        boolean hasUserOptedOut = !(boolean) newValue;
                        AnalyticsUtils.updateAnalyticsPreference(
                                getActivity(),
                                mDispatcher,
                                mAccountStore,
                                hasUserOptedOut);

                        return true;
                    }
                }
        );
        updateAnalyticsSyncUI();

        mLanguagePreference = (WPPreference) findPreference(getString(R.string.pref_key_language));
        mLanguagePreference.setOnPreferenceChangeListener(this);
        mLanguagePreference.setOnPreferenceClickListener(this);
        mLanguagePreference.setSummary(mLocaleProvider.getAppLanguageDisplayString());

        mAppThemePreference = (ListPreference) findPreference(getString(R.string.pref_key_app_theme));
        mAppThemePreference.setOnPreferenceChangeListener(this);

        mInitialScreenPreference = (ListPreference) findPreference(getString(R.string.pref_key_initial_screen));
        mInitialScreenPreference.setOnPreferenceChangeListener(this);

        findPreference(getString(R.string.pref_key_language))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_device_settings))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_debug_settings))
                .setOnPreferenceClickListener(this);

        mOptimizedImage =
                (WPSwitchPreference) WPPrefUtils
                        .getPrefAndSetChangeListener(this, R.string.pref_key_optimize_image, this);
        mImageMaxSizePref = (DetailListPreference) WPPrefUtils
                .getPrefAndSetChangeListener(this, R.string.pref_key_site_image_width, this);
        mImageQualityPref =
                (DetailListPreference) WPPrefUtils
                        .getPrefAndSetChangeListener(this, R.string.pref_key_site_image_quality, this);
        mOptimizedVideo =
                (WPSwitchPreference) WPPrefUtils
                        .getPrefAndSetChangeListener(this, R.string.pref_key_optimize_video, this);

        mVideoWidthPref =
                (DetailListPreference) WPPrefUtils
                        .getPrefAndSetChangeListener(this, R.string.pref_key_site_video_width, this);
        mVideoEncorderBitratePref =
                (DetailListPreference) WPPrefUtils
                        .getPrefAndSetChangeListener(this, R.string.pref_key_site_video_encoder_bitrate, this);
        mPrivacySettings = (PreferenceScreen) WPPrefUtils
                .getPrefAndSetClickListener(this, R.string.pref_key_privacy_settings, this);

        mStripImageLocation =
                (WPSwitchPreference) WPPrefUtils
                        .getPrefAndSetChangeListener(this, R.string.pref_key_strip_image_location, this);
        mReportCrashPref = (WPSwitchPreference) WPPrefUtils
                .getPrefAndSetChangeListener(this, R.string.pref_key_send_crash, this);

        mOpenWebLinksWithJetpack =
                (WPSwitchPreference) WPPrefUtils
                        .getPrefAndSetChangeListener(this, R.string.pref_key_open_web_links_with_jetpack, this);

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

        mStripImageLocation.setChecked(AppPrefs.isStripImageLocation());

        mOpenWebLinksWithJetpack.setChecked(AppPrefs.getIsOpenWebLinksWithJetpack());

        mWhatsNew = findPreference(getString(R.string.pref_key_whats_new));

        removeWhatsNewPreference();
        mDispatcher.dispatch(WhatsNewActionBuilder.newFetchCachedAnnouncementAction());

        if (!BuildConfig.OFFER_GUTENBERG) {
            removeExperimentalCategory();
        }

        if (!BuildConfig.ENABLE_DEBUG_SETTINGS) {
            removeDebugSettingsCategory();
        }

        if (!mMySiteDashboardTabsFeatureConfig.isEnabled()) {
            removeInitialScreen();
        }

        if (!mOpenWebLinksWithJetpackHelper.shouldShowAppSetting()) {
            removeOpenWebLinksWithJetpack();
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = super.onCreateView(inflater, container, savedInstanceState);

        final ListView listOfPreferences = view.findViewById(android.R.id.list);
        if (listOfPreferences != null) {
            ViewCompat.setNestedScrollingEnabled(listOfPreferences, true);
            addJetpackBadgeAsFooterIfEnabled(inflater, listOfPreferences);
        }
        return view;
    }

    private void addJetpackBadgeAsFooterIfEnabled(LayoutInflater inflater, ListView listView) {
        if (mJetpackBrandingUtils.shouldShowJetpackBranding()) {
            final JetpackBrandedScreenData screen = JetpackBrandedScreenData.APP_SETTINGS.INSTANCE;
            final JetpackBadgeFooterBinding binding = JetpackBadgeFooterBinding.inflate(inflater);
            binding.footerJetpackBadge.jetpackPoweredBadge.setText(
                    mUiHelpers.getTextOfUiString(
                            getContext(),
                            mJetpackBrandingUtils.getBrandingTextForScreen(screen)
                    )
            );

            if (mJetpackBrandingUtils.shouldShowJetpackPoweredBottomSheet()) {
                binding.footerJetpackBadge.jetpackPoweredBadge.setOnClickListener(v -> {
                    mJetpackBrandingUtils.trackBadgeTapped(screen);
                    new JetpackPoweredBottomSheetFragment().show(
                            ((AppCompatActivity) getActivity()).getSupportFragmentManager(),
                            JetpackPoweredBottomSheetFragment.TAG);
                });
            }
            listView.addFooterView(binding.getRoot(), null, false);
        }
    }

    private void removeExperimentalCategory() {
        PreferenceCategory experimentalPreferenceCategory =
                (PreferenceCategory) findPreference(getString(R.string.pref_key_experimental_section));
        PreferenceScreen preferenceScreen =
                (PreferenceScreen) findPreference(getString(R.string.pref_key_app_settings_root));
        preferenceScreen.removePreference(experimentalPreferenceCategory);
    }

    private void removeDebugSettingsCategory() {
        Preference experimentalPreference =
                findPreference(getString(R.string.pref_key_debug_settings));
        PreferenceScreen preferenceScreen =
                (PreferenceScreen) findPreference(getString(R.string.pref_key_app_settings_root));
        preferenceScreen.removePreference(experimentalPreference);
    }

    private void removeWhatsNewPreference() {
        PreferenceScreen preferenceScreen =
                (PreferenceScreen) findPreference(getString(R.string.pref_key_app_settings_root));
        preferenceScreen.removePreference(mWhatsNew);
    }

    private void addWhatsNewPreference() {
        PreferenceScreen preferenceScreen =
                (PreferenceScreen) findPreference(getString(R.string.pref_key_app_settings_root));
        preferenceScreen.addPreference(mWhatsNew);
    }

    private void removeInitialScreen() {
        Preference initialScreenPreference =
                findPreference(getString(R.string.pref_key_initial_screen));
        PreferenceScreen preferenceScreen =
                (PreferenceScreen) findPreference(getString(R.string.pref_key_app_settings_root));
        preferenceScreen.removePreference(initialScreenPreference);
    }

    private void removeOpenWebLinksWithJetpack() {
        Preference openWebLinksWithJetpackPreference =
                findPreference(getString(R.string.pref_key_open_web_links_with_jetpack));
        PreferenceScreen preferenceScreen =
                (PreferenceScreen) findPreference(getString(R.string.pref_key_app_settings_root));
        preferenceScreen.removePreference(openWebLinksWithJetpackPreference);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAccountStore.hasAccessToken() && NetworkUtils.isNetworkAvailable(getActivity())) {
            mDispatcher.dispatch(AccountActionBuilder.newFetchSettingsAction());
        }
    }

    @Override
    public void onStart() {
        super.onStart();
    }

    @Override
    public void onStop() {
        mDispatcher.unregister(this);
        super.onStop();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        reattachLocalePickerCallback();
        // flush gathered events (if any)
        AnalyticsTracker.flush();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWhatsNewFetched(OnWhatsNewFetched event) {
        if (event.isFromCache()) {
            mDispatcher.dispatch(WhatsNewActionBuilder
                    .newFetchRemoteAnnouncementAction(
                            new WhatsNewFetchPayload(mBuildConfigWrapper.getAppVersionName(),
                                    WhatsNewAppId.WP_ANDROID)));
        }

        if (event.error != null || event.getWhatsNewItems() == null || event.getWhatsNewItems().isEmpty()) {
            return;
        }

        WhatsNewAnnouncementModel latestAnnouncement = event.getWhatsNewItems().get(0);
        mWhatsNew.setSummary(getString(R.string.version_with_name_param, latestAnnouncement.getAppVersionName()));
        mWhatsNew.setOnPreferenceClickListener(this);
        if (mBuildConfigWrapper.isWhatsNewFeatureEnabled()) {
            addWhatsNewPreference();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onAccountChanged(OnAccountChanged event) {
        if (!isAdded()) {
            return;
        }

        if (event.isError()) {
            switch (event.error.type) {
                case SETTINGS_FETCH_GENERIC_ERROR:
                    ToastUtils
                            .showToast(getActivity(), R.string.error_fetch_account_settings, ToastUtils.Duration.LONG);
                    break;
                case SETTINGS_FETCH_REAUTHORIZATION_REQUIRED_ERROR:
                    ToastUtils.showToast(getActivity(), R.string.error_disabled_apis,
                            ToastUtils.Duration.LONG);
                    break;
                case SETTINGS_POST_ERROR:
                    ToastUtils.showToast(getActivity(), R.string.error_post_account_settings, ToastUtils.Duration.LONG);
                    break;
            }
        } else if (event.causeOfChange == AccountAction.FETCH_SETTINGS) {
            // no need to sync with remote here, or do anything else here, since the logic is already in WordPress.java
            updateAnalyticsSyncUI();
        }
    }

    /* Make sure the UI is synced with the backend value */
    private void updateAnalyticsSyncUI() {
        if (!isAdded()) {
            return;
        }
        if (mAccountStore.hasAccessToken()) {
            SwitchPreference tracksOptOutPreference =
                    (SwitchPreference) findPreference(getString(R.string.pref_key_send_usage));
            tracksOptOutPreference.setChecked(!mAccountStore.getAccount().getTracksOptOut());
        }
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String preferenceKey = preference != null ? preference.getKey() : "";

        if (preferenceKey.equals(getString(R.string.pref_key_device_settings))) {
            return handleDevicePreferenceClick();
        } else if (preferenceKey.equals(getString(R.string.pref_key_debug_settings))) {
            return handleDebugSettingsPreferenceClick();
        } else if (preference == mPrivacySettings) {
            return handlePrivacyClick();
        } else if (preference == mWhatsNew) {
            return handleFeatureAnnouncementClick();
        } else if (preference == mLanguagePreference) {
            return handleAppLocalePickerClick();
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) {
            return false;
        }

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
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_SETTINGS_MAX_IMAGE_SIZE_CHANGED);
        } else if (preference == mImageQualityPref) {
            AppPrefs.setImageOptimizeQuality(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mImageQualityPref,
                    newValue.toString(),
                    getLabelForImageQualityValue(AppPrefs.getImageOptimizeQuality()));
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_SETTINGS_IMAGE_QUALITY_CHANGED);
        } else if (preference == mOptimizedVideo) {
            AppPrefs.setVideoOptimize((Boolean) newValue);
            mVideoEncorderBitratePref.setEnabled((Boolean) newValue);
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_SETTINGS_VIDEO_OPTIMIZATION_CHANGED, Collections
                    .singletonMap(TRACK_ENABLED, newValue));
        } else if (preference == mVideoWidthPref) {
            int newWidth = Integer.parseInt(newValue.toString());
            AppPrefs.setVideoOptimizeWidth(newWidth);
            setDetailListPreferenceValue(mVideoWidthPref,
                    newValue.toString(),
                    getLabelForVideoMaxWidthValue(AppPrefs.getVideoOptimizeWidth()));
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_SETTINGS_MAX_VIDEO_SIZE_CHANGED);
        } else if (preference == mVideoEncorderBitratePref) {
            AppPrefs.setVideoOptimizeQuality(Integer.parseInt(newValue.toString()));
            setDetailListPreferenceValue(mVideoEncorderBitratePref,
                    newValue.toString(),
                    getLabelForVideoEncoderBitrateValue(AppPrefs.getVideoOptimizeQuality()));
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_SETTINGS_VIDEO_QUALITY_CHANGED);
        } else if (preference == mStripImageLocation) {
            AppPrefs.setStripImageLocation((Boolean) newValue);
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_SETTINGS_REMOVE_LOCATION_FROM_MEDIA_CHANGED, Collections
                    .singletonMap(TRACK_ENABLED, newValue));
        } else if (preference == mAppThemePreference) {
            AppThemeUtils.Companion.setAppTheme(getActivity(), (String) newValue);
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_SETTINGS_APPEARANCE_CHANGED, Collections
                    .singletonMap(TRACK_STYLE, (String) newValue));
            // restart activity to make sure changes are applied to PreferenceScreen
            getActivity().recreate();
        } else if (preference == mInitialScreenPreference) {
            String trackValue = newValue.equals(MySiteTabType.SITE_MENU.getLabel())
                    ? MySiteTabType.SITE_MENU.getTrackingLabel()
                    : MySiteTabType.DASHBOARD.getTrackingLabel();
            Map<String, Object> properties = new HashMap<>();
            properties.put("selected", trackValue);
            AnalyticsTracker.track(Stat.APP_SETTINGS_INITIAL_SCREEN_CHANGED, properties);
        } else if (preference == mReportCrashPref) {
            AnalyticsTracker.track(Stat.PRIVACY_SETTINGS_REPORT_CRASHES_TOGGLED, Collections
                    .singletonMap(TRACK_ENABLED, newValue));
        } else if (preference == mOpenWebLinksWithJetpack) {
            handleOpenLinksInJetpack((Boolean) newValue);
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

    private void changeLanguage(String languageCode) {
        if (mLanguagePreference == null || TextUtils.isEmpty(languageCode)) {
            return;
        }

        if (LocaleManager.isSameLanguage(languageCode)) {
            return;
        }

        LocaleManager.setNewLocale(WordPress.getContext(), languageCode);
        WordPress.updateContextLocale();
        mContextProvider.refreshContext();

        // Track language change on Analytics because we have both the device language and app selected language
        // data in Tracks metadata.
        Map<String, Object> properties = new HashMap<>();
        properties.put("app_locale", Locale.getDefault());
        AnalyticsTracker.track(Stat.ACCOUNT_SETTINGS_LANGUAGE_CHANGED, properties);

        // Language is now part of metadata, so we need to refresh them
        AnalyticsUtils.refreshMetadata(mAccountStore, mSiteStore);

        // Refresh the app
        Intent refresh = new Intent(getActivity(), getActivity().getClass());
        startActivity(refresh);
        getActivity().setResult(LANGUAGE_CHANGED);
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
        getActivity().finish();

        // update Reader tags as they need be localized
        ReaderUpdateServiceStarter.startService(WordPress.getContext(), EnumSet.of(ReaderUpdateLogic.UpdateTask.TAGS));
    }

    private boolean handleDebugSettingsPreferenceClick() {
        startActivity(new Intent(getActivity(), DebugSettingsActivity.class));
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

        AnalyticsTracker.track(Stat.APP_SETTINGS_OPEN_DEVICE_SETTINGS_TAPPED);
        return true;
    }

    private String getLabelForImageMaxSizeValue(int newValue) {
        String[] values = getActivity().getResources().getStringArray(R.array.site_settings_image_max_size_values);
        String[] entries = getActivity().getResources().getStringArray(R.array.site_settings_image_max_size_entries);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(String.valueOf(newValue))) {
                return entries[i];
            }
        }

        return entries[0];
    }

    private String getLabelForImageQualityValue(int newValue) {
        String[] values = getActivity().getResources().getStringArray(R.array.site_settings_image_quality_values);
        String[] entries = getActivity().getResources().getStringArray(R.array.site_settings_image_quality_entries);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(String.valueOf(newValue))) {
                return entries[i];
            }
        }

        return entries[0];
    }

    private String getLabelForVideoMaxWidthValue(int newValue) {
        String[] values = getActivity().getResources().getStringArray(R.array.site_settings_video_width_values);
        String[] entries = getActivity().getResources().getStringArray(R.array.site_settings_video_width_entries);
        for (int i = 0; i < values.length; i++) {
            if (values[i].equals(String.valueOf(newValue))) {
                return entries[i];
            }
        }

        return entries[0];
    }

    private String getLabelForVideoEncoderBitrateValue(int newValue) {
        String[] values = getActivity().getResources().getStringArray(R.array.site_settings_video_bitrate_values);
        String[] entries = getActivity().getResources().getStringArray(R.array.site_settings_video_bitrate_entries);
        for (int i = 0; i < values.length; i++) {
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

    private boolean handlePrivacyClick() {
        AnalyticsTracker.track(Stat.APP_SETTINGS_PRIVACY_SETTINGS_TAPPED);

        if (mPrivacySettings == null || !isAdded()) {
            return false;
        }

        String title = getString(R.string.preference_privacy_settings);
        Dialog dialog = mPrivacySettings.getDialog();
        if (dialog != null) {
            WPActivityUtils.addToolbarToDialog(this, dialog, title);
        }

        AnalyticsTracker.track(Stat.PRIVACY_SETTINGS_OPENED);
        return true;
    }

    private boolean handleFeatureAnnouncementClick() {
        if (getActivity() instanceof AppCompatActivity) {
            AnalyticsTracker.track(Stat.FEATURE_ANNOUNCEMENT_SHOWN_FROM_APP_SETTINGS);
            new FeatureAnnouncementDialogFragment()
                    .show(((AppCompatActivity) getActivity()).getSupportFragmentManager(),
                            FeatureAnnouncementDialogFragment.TAG);
            return true;
        } else {
            throw new IllegalArgumentException(
                    "Parent activity is not AppCompatActivity. FeatureAnnouncementDialogFragment must be called "
                    + "using support fragment manager from AppCompatActivity.");
        }
    }

    private boolean handleAppLocalePickerClick() {
        if (getActivity() instanceof AppCompatActivity) {
            LocalePickerBottomSheet bottomSheet = LocalePickerBottomSheet.newInstance();
            bottomSheet.setLocalePickerCallback(this);
            bottomSheet.show(((AppCompatActivity) getActivity()).getSupportFragmentManager(),
                    LocalePickerBottomSheet.TAG);
            return true;
        } else {
            throw new IllegalArgumentException(
                    "Parent activity is not AppCompatActivity. LocalePickerBottomSheet must be called "
                    + "using support fragment manager from AppCompatActivity.");
        }
    }

    private void reattachLocalePickerCallback() {
        if (getActivity() instanceof AppCompatActivity) {
            LocalePickerBottomSheet bottomSheet = (LocalePickerBottomSheet) (((AppCompatActivity) getActivity()))
                    .getSupportFragmentManager().findFragmentByTag(LocalePickerBottomSheet.TAG);
            if (bottomSheet != null) {
                bottomSheet.setLocalePickerCallback(this);
            }
        }
    }

    @Override
    public void onLocaleSelected(@NotNull String languageCode) {
        onPreferenceChange(mLanguagePreference, languageCode);
    }

    private void handleOpenLinksInJetpack(Boolean newValue) {
        try {
            if (newValue) {
                mOpenWebLinksWithJetpackHelper.disableDeepLinks();
            } else {
                mOpenWebLinksWithJetpackHelper.enableDeepLinks();
            }
            AppPrefs.setIsOpenWebLinksWithJetpack(newValue);
            AnalyticsTracker.track(AnalyticsTracker.Stat.APP_SETTINGS_OPEN_WEB_LINKS_WITH_JETPACK_CHANGED, Collections
                    .singletonMap(TRACK_ENABLED, newValue));
        } catch (Exception e) {
            ToastUtils.showToast(
                    getActivity(),
                    (newValue ? R.string.preference_open_links_in_jetpack_setting_change_enable_error
                            : R.string.preference_open_links_in_jetpack_setting_change_disable_error),
                    ToastUtils.Duration.LONG);
            AppLog.e(AppLog.T.UTILS, "Unable to enable or disable open with Jetpack components ", e);
        }
    }
}
