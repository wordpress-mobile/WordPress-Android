package org.wordpress.android.ui.prefs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.util.WPPrefUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;

@SuppressWarnings("deprecation")
public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    public static final String LANGUAGE_PREF_KEY = "language-pref";
    public static final int LANGUAGE_CHANGED = 1000;

    private PreferenceScreen mPreferenceScreen;
    private Preference mUsernamePreference;
    private EditTextPreference mEmailPreference;
    private DetailListPreference mPrimarySitePreference;
    private EditTextPreference mWebAddressPreference;
    private DetailListPreference mLanguagePreference;
    private Snackbar mEmailSnackbar;
    private SharedPreferences mSettings;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setRetainInstance(true);
        addPreferencesFromResource(R.xml.settings);

        mPreferenceScreen = (PreferenceScreen) findPreference(getActivity().getString(R.string.pref_key_settings_root));

        mUsernamePreference = findPreference(getString(R.string.pref_key_username));
        mEmailPreference = (EditTextPreference) findPreference(getString(R.string.pref_key_email));
        mPrimarySitePreference = (DetailListPreference) findPreference(getString(R.string.pref_key_primary_site));
        mWebAddressPreference = (EditTextPreference) findPreference(getString(R.string.pref_key_web_address));
        mLanguagePreference = (DetailListPreference) findPreference(getString(R.string.pref_key_language));

        mEmailPreference.setOnPreferenceChangeListener(this);
        mPrimarySitePreference.setOnPreferenceChangeListener(this);
        mWebAddressPreference.setOnPreferenceChangeListener(this);
        mLanguagePreference.setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.pref_key_language))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_app_about))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_oss_licenses))
                .setOnPreferenceClickListener(this);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());
        checkWordPressComOnlyFields();

        List<Map<String, Object>> blogList = WordPress.wpDB.getBlogsBy("dotcomFlag=1", new String[]{"homeURL"});
        mPrimarySitePreference.setEntries(BlogUtils.getBlogNamesFromAccountMapList(blogList));
        mPrimarySitePreference.setEntryValues(BlogUtils.getBlogIdsFromAccountMapList(blogList));
        mPrimarySitePreference.setDetails(BlogUtils.getHomeURLOrHostNamesFromAccountMapList(blogList));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View coordinatorView = inflater.inflate(R.layout.preference_coordinator, container, false);
        CoordinatorLayout coordinator = (CoordinatorLayout) coordinatorView.findViewById(R.id.coordinator);
        View preferenceView = super.onCreateView(inflater, coordinator, savedInstanceState);
        coordinator.addView(preferenceView);
        return coordinatorView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        updateLanguagePreference(getResources().getConfiguration().locale.toString());
        refreshAccountDetails();
    }

    @Override
    public void onResume() {
        super.onResume();

        getActivity().setTitle(R.string.settings);
    }

    @Override
    public void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String preferenceKey = preference != null ? preference.getKey() : "";

        if (preferenceKey.equals(getString(R.string.pref_key_app_about))) {
            return handleAboutPreferenceClick();
        } else if (preferenceKey.equals(getString(R.string.pref_key_oss_licenses))) {
            return handleOssPreferenceClick();
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) return false;

        if (preference == mEmailPreference) {
            Account account = AccountHelper.getDefaultAccount();
            // if the user changed her email to her verified email, just cancel the pending email change
            if (account.getEmail().equals(newValue.toString())) {
                cancelPendingEmailChange();
            } else {
                updateEmail(newValue.toString());
                showPendingEmailChangeSnackbar(newValue.toString());
            }
            return false;
        } else if (preference == mPrimarySitePreference) {
            changePrimaryBlogPreference(newValue.toString());
            updatePrimaryBlog(newValue.toString());
        } else if (preference == mWebAddressPreference) {
            mWebAddressPreference.setSummary(newValue.toString());
            updateWebAddress(newValue.toString());
        } else if (preference == mLanguagePreference) {
            changeLanguage(newValue.toString());
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


    private void refreshAccountDetails() {
        Account account = AccountHelper.getDefaultAccount();
        mUsernamePreference.setSummary(account.getUserName());
        mEmailPreference.setSummary(account.getEmail());
        mWebAddressPreference.setSummary(account.getWebAddress());

        String blogId = String.valueOf(account.getPrimaryBlogId());
        changePrimaryBlogPreference(blogId);

        checkIfEmailChangeIsPending();
    }

    private void checkWordPressComOnlyFields() {
        if (!AccountHelper.isSignedInWordPressDotCom()) {
            mPreferenceScreen.removePreference(mUsernamePreference);
        }
    }

    private void checkIfEmailChangeIsPending() {
        final Account account = AccountHelper.getDefaultAccount();
        if (account.getPendingEmailChange()) {
            showPendingEmailChangeSnackbar(account.getNewEmail());
        }
    }

    private void showPendingEmailChangeSnackbar(String newEmail) {
        if (getView() != null) {
            if (mEmailSnackbar == null || !mEmailSnackbar.isShown()) {
                View.OnClickListener clickListener = new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        cancelPendingEmailChange();
                    }
                };

                mEmailSnackbar = Snackbar
                        .make(getView(), "", Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.button_revert), clickListener);
                mEmailSnackbar.getView().setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.grey_dark));
                mEmailSnackbar.setActionTextColor(ContextCompat.getColor(getActivity(), R.color.blue_medium));
                TextView textView = (TextView) mEmailSnackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
                textView.setMaxLines(4);
            }
            // instead of creating a new snackbar, update the current one to avoid the jumping animation
            mEmailSnackbar.setText(getString(R.string.pending_email_change_snackbar, newEmail));
            if (!mEmailSnackbar.isShown()) {
                mEmailSnackbar.show();
            }
        }
    }

    private void cancelPendingEmailChange() {
        Map<String, String> params = new HashMap<>();
        params.put(Account.RestParam.toString(Account.RestParam.EMAIL_CHANGE_PENDING), "false");
        AccountHelper.getDefaultAccount().postAccountSettings(params);
        if (mEmailSnackbar != null && mEmailSnackbar.isShown()) {
            mEmailSnackbar.dismiss();
        }
    }

    private void changeLanguage(String languageCode) {
        if (mLanguagePreference == null || TextUtils.isEmpty(languageCode)) return;

        Resources res = getResources();
        Configuration conf = res.getConfiguration();
        Locale currentLocale = conf.locale != null ? conf.locale : Locale.getDefault();

        if (currentLocale.toString().equals(languageCode)) return;

        updateLanguagePreference(languageCode);

        // update configuration
        Locale newLocale = WPPrefUtils.languageLocale(languageCode);
        conf.locale = newLocale;
        res.updateConfiguration(conf, res.getDisplayMetrics());
        mSettings.edit().putString(LANGUAGE_PREF_KEY, newLocale.toString()).apply();

        // Track the change only if the user selected a non default Device language. This is only used in
        // Mixpanel, because we have both the device language and app selected language data in Tracks
        // metadata.
        if (!Locale.getDefault().equals(newLocale)) {
            Map<String, Object> properties = new HashMap<>();
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

    private void updateLanguagePreference(String languageCode) {
        if (mLanguagePreference == null || TextUtils.isEmpty(languageCode)) return;

        String[] availableLocales = getResources().getStringArray(R.array.language_codes);
        Arrays.sort(availableLocales);

        if (mLanguagePreference.getEntryValues() == null || mLanguagePreference.getEntryValues().length == 0) {
            // update details to display in selected locale
            mLanguagePreference.setEntryValues(availableLocales);
        }

        mLanguagePreference.setValue(languageCode);
        String summary = WPPrefUtils.getLanguageString(languageCode, WPPrefUtils.languageLocale(languageCode));
        mLanguagePreference.setSummary(summary);

        mLanguagePreference.setEntries(WPPrefUtils.createLanguageDisplayStrings(availableLocales));
        mLanguagePreference.setDetails(WPPrefUtils.createLanguageDetailDisplayStrings(availableLocales, languageCode));
        mLanguagePreference.refreshAdapter();
    }

    private void changePrimaryBlogPreference(String blogId) {
        mPrimarySitePreference.setValue(blogId);
        Blog primaryBlog = WordPress.wpDB.getBlogForDotComBlogId(blogId);
        if (primaryBlog != null) {
            mPrimarySitePreference.setSummary(UrlUtils.getHost(primaryBlog.getHomeURL()));
            mPrimarySitePreference.refreshAdapter();
        }
    }

    private boolean handleAboutPreferenceClick() {
        startActivity(new Intent(getActivity(), AboutActivity.class));
        return true;
    }

    private boolean handleOssPreferenceClick() {
        startActivity(new Intent(getActivity(), LicensesActivity.class));
        return true;
    }

    private void updateEmail(String newEmail) {
        Account account = AccountHelper.getDefaultAccount();
        Map<String, String> params = new HashMap<>();
        params.put(Account.RestParam.toString(Account.RestParam.EMAIL), newEmail);
        account.postAccountSettings(params);
    }

    private void updatePrimaryBlog(String blogId) {
        Account account = AccountHelper.getDefaultAccount();
        Map<String, String> params = new HashMap<>();
        params.put(Account.RestParam.toString(Account.RestParam.PRIMARY_BLOG), blogId);
        account.postAccountSettings(params);
    }

    public void updateWebAddress(String newWebAddress) {
        Account account = AccountHelper.getDefaultAccount();
        Map<String, String> params = new HashMap<>();
        params.put(Account.RestParam.toString(Account.RestParam.WEB_ADDRESS), newWebAddress);
        account.postAccountSettings(params);
    }

    public void onEventMainThread(PrefsEvents.AccountSettingsChanged event) {
        if (isAdded()) {
            refreshAccountDetails();
        }
    }
}
