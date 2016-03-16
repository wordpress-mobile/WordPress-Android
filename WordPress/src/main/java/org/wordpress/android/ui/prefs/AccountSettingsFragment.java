package org.wordpress.android.ui.prefs;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Pair;
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
import org.wordpress.android.models.AccountModel;
import org.wordpress.android.models.Blog;
import org.wordpress.android.stores.store.AccountStore;
import org.wordpress.android.stores.store.SiteStore;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.BlogUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.WPPrefUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

@SuppressWarnings("deprecation")
public class AccountSettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    public static final String LANGUAGE_PREF_KEY = "language-pref";
    public static final int LANGUAGE_CHANGED = 1000;

    private PreferenceScreen mPreferenceScreen;
    private Preference mUsernamePreference;
    private EditTextPreferenceWithValidation mEmailPreference;
    private DetailListPreference mPrimarySitePreference;
    private EditTextPreferenceWithValidation mWebAddressPreference;
    private DetailListPreference mLanguagePreference;
    private Snackbar mEmailSnackbar;
    private SharedPreferences mSettings;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getActivity().getApplication()).component().inject(this);

        setRetainInstance(true);
        addPreferencesFromResource(R.xml.settings);

        mPreferenceScreen = (PreferenceScreen) findPreference(getActivity().getString(R.string.pref_key_settings_root));

        mUsernamePreference = findPreference(getString(R.string.pref_key_username));
        mEmailPreference = (EditTextPreferenceWithValidation) findPreference(getString(R.string.pref_key_email));
        mPrimarySitePreference = (DetailListPreference) findPreference(getString(R.string.pref_key_primary_site));
        mWebAddressPreference = (EditTextPreferenceWithValidation) findPreference(getString(R.string.pref_key_web_address));
        mLanguagePreference = (DetailListPreference) findPreference(getString(R.string.pref_key_language));

        mEmailPreference.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        mEmailPreference.setValidationType(EditTextPreferenceWithValidation.ValidationType.EMAIL);
        mWebAddressPreference.getEditText().setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        mWebAddressPreference.setValidationType(EditTextPreferenceWithValidation.ValidationType.URL);

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

        updateVisualEditorSettings();
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
        if (NetworkUtils.isNetworkAvailable(getActivity())) {
            AccountHelper.getDefaultAccount().fetchAccountSettings();
        }

        getActivity().setTitle(R.string.account_settings);
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
            updateEmail(newValue.toString());
            showPendingEmailChangeSnackbar(newValue.toString());
            mEmailPreference.setEnabled(false);
            return false;
        } else if (preference == mPrimarySitePreference) {
            changePrimaryBlogPreference(newValue.toString());
            updatePrimaryBlog(newValue.toString());
            return false;
        } else if (preference == mWebAddressPreference) {
            mWebAddressPreference.setSummary(newValue.toString());
            updateWebAddress(newValue.toString());
            return false;
        } else if (preference == mLanguagePreference) {
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

    private void updateVisualEditorSettings() {
        if (!AppPrefs.isVisualEditorAvailable()) {
            PreferenceScreen preferenceScreen = (PreferenceScreen) findPreference(getActivity()
                    .getString(R.string.pref_key_settings_root));
            PreferenceCategory editor = (PreferenceCategory) findPreference(getActivity()
                    .getString(R.string.pref_key_editor));
            if (preferenceScreen != null && editor != null) {
                preferenceScreen.removePreference(editor);
            }
        } else {
            final CheckBoxPreference visualEditorCheckBox = (CheckBoxPreference) findPreference(getActivity()
                    .getString(R.string.pref_key_visual_editor_enabled));
            visualEditorCheckBox.setChecked(AppPrefs.isVisualEditorEnabled());
            visualEditorCheckBox.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(final Preference preference, final Object newValue) {
                    visualEditorCheckBox.setChecked(!visualEditorCheckBox.isChecked());
                    AppPrefs.setVisualEditorEnabled(visualEditorCheckBox.isChecked());
                    return false;
                }
            });
        }
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
            mPreferenceScreen.removePreference(mEmailPreference);
            mPreferenceScreen.removePreference(mPrimarySitePreference);
            mPreferenceScreen.removePreference(mWebAddressPreference);
        } else {
            // only load sites for WordPress.com accounts since primary site preference is hidden otherwise
            new LoadSitesTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        }
    }

    private void checkIfEmailChangeIsPending() {
        final Account account = AccountHelper.getDefaultAccount();
        if (account.getPendingEmailChange()) {
            showPendingEmailChangeSnackbar(account.getNewEmail());
        } else if (mEmailSnackbar != null && mEmailSnackbar.isShown()){
            mEmailSnackbar.dismiss();
        }
        mEmailPreference.setEnabled(!account.getPendingEmailChange());
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
        params.put(AccountModel.RestParam.EMAIL_CHANGE_PENDING.getDescription(), "false");
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

        if (Locale.getDefault().equals(newLocale)) {
            // remove custom locale key when original device locale is selected
            mSettings.edit().remove(LANGUAGE_PREF_KEY).apply();
        } else {
            mSettings.edit().putString(LANGUAGE_PREF_KEY, newLocale.toString()).apply();
        }

        // Track language change on Mixpanel because we have both the device language and app selected language
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

        mLanguagePreference.setValue(languageCode.toLowerCase());
        mLanguagePreference.setSummary(WPPrefUtils.getLanguageString(languageCode, languageLocale));
        mLanguagePreference.refreshAdapter();
    }

    private void changePrimaryBlogPreference(String blogId) {
        mPrimarySitePreference.setValue(blogId);
        Blog primaryBlog = WordPress.wpDB.getBlogForDotComBlogId(blogId);
        if (primaryBlog != null) {
            mPrimarySitePreference.setSummary(primaryBlog.getNameOrHostUrl());
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
        params.put(AccountModel.RestParam.EMAIL.getDescription(), newEmail);
        account.postAccountSettings(params);
    }

    private void updatePrimaryBlog(String blogId) {
        Account account = AccountHelper.getDefaultAccount();
        Map<String, String> params = new HashMap<>();
        params.put(AccountModel.RestParam.PRIMARY_BLOG.getDescription(), blogId);
        account.postAccountSettings(params);
    }

    public void updateWebAddress(String newWebAddress) {
        Account account = AccountHelper.getDefaultAccount();
        Map<String, String> params = new HashMap<>();
        params.put(AccountModel.RestParam.WEB_ADDRESS.getDescription(), newWebAddress);
        account.postAccountSettings(params);
    }

    public void onEventMainThread(PrefsEvents.AccountSettingsFetchSuccess event) {
        if (isAdded()) {
            refreshAccountDetails();
        }
    }

    public void onEventMainThread(PrefsEvents.AccountSettingsPostSuccess event) {
        if (isAdded()) {
            refreshAccountDetails();
        }
    }

    public void onEventMainThread(PrefsEvents.AccountSettingsFetchError event) {
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), R.string.error_fetch_account_settings, ToastUtils.Duration.LONG);
        }
    }

    public void onEventMainThread(PrefsEvents.AccountSettingsPostError event) {
        if (isAdded()) {
            ToastUtils.showToast(getActivity(), R.string.error_post_account_settings, ToastUtils.Duration.LONG);

            // we optimistically show the email change snackbar, if that request fails, we should remove the snackbar
            checkIfEmailChangeIsPending();
        }
    }

    /*
     * AsyncTask which loads sites from database for primary site preference
     */
    private class LoadSitesTask extends AsyncTask<Void, Void, Void> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected void onCancelled() {
            super.onCancelled();
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<Map<String, Object>> blogList = WordPress.wpDB.getBlogsBy("dotcomFlag=1", new String[]{"homeURL"});
            mPrimarySitePreference.setEntries(BlogUtils.getBlogNamesFromAccountMapList(blogList));
            mPrimarySitePreference.setEntryValues(BlogUtils.getBlogIdsFromAccountMapList(blogList));
            mPrimarySitePreference.setDetails(BlogUtils.getHomeURLOrHostNamesFromAccountMapList(blogList));

            return null;
        }

        @Override
        protected void onPostExecute(Void results) {
            super.onPostExecute(results);
            mPrimarySitePreference.refreshAdapter();
        }
    }
}
