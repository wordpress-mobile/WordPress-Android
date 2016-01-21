package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.content.DialogInterface;
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
import android.util.DisplayMetrics;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.apache.commons.lang.ArrayUtils;
import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.WPPrefUtils;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import de.greenrobot.event.EventBus;

@SuppressWarnings("deprecation")
public class SettingsFragment extends PreferenceFragment implements OnPreferenceClickListener, Preference.OnPreferenceChangeListener {
    public static final String LANGUAGE_PREF_KEY = "language-pref";
    public static final int LANGUAGE_CHANGED = 1000;

    private PreferenceScreen mPreferenceScreen;
    private AlertDialog mDialog;
    private SharedPreferences mSettings;
    private Preference mUsernamePreference;
    private EditTextPreference mEmailPreference;
    private DetailListPreference mLanguagePreference;
    private Snackbar mEmailSnackbar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        mPreferenceScreen = (PreferenceScreen) findPreference(getActivity().getString(R.string.pref_key_settings_root));

        mUsernamePreference = findPreference(getString(R.string.pref_key_username));
        mEmailPreference = (EditTextPreference) findPreference(getString(R.string.pref_key_email));
        mLanguagePreference = (DetailListPreference) findPreference(getString(R.string.pref_key_language));

        mEmailPreference.setOnPreferenceChangeListener(this);
        mLanguagePreference.setOnPreferenceChangeListener(this);
        findPreference(getString(R.string.pref_key_language))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_app_about))
                .setOnPreferenceClickListener(this);
        findPreference(getString(R.string.pref_key_oss_licenses))
                .setOnPreferenceClickListener(this);

        mSettings = PreferenceManager.getDefaultSharedPreferences(getActivity());

        checkWordPressComOnlyFields();
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

        setInitialLanguagePreference();
        refreshAccountDetails();
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
        } else if (preference == mLanguagePreference) {
            mLanguagePreference.setValue(newValue.toString());
            String summary = WPPrefUtils.getLanguageString(newValue.toString(), WPPrefUtils.languageLocale(newValue.toString()));
            mLanguagePreference.setSummary(summary);

            // update details to display in selected locale
            CharSequence[] languageCodes = mLanguagePreference.getEntryValues();
            mLanguagePreference.setEntries(WPPrefUtils.createLanguageDisplayStrings(languageCodes));
            mLanguagePreference.setDetails(WPPrefUtils.createLanguageDetailDisplayStrings(languageCodes, newValue.toString()));
            mLanguagePreference.refreshAdapter();
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

    private void setInitialLanguagePreference() {
        if (mLanguagePreference == null) return;

        Resources res = getResources();
        Configuration conf = res.getConfiguration();

        String[] availableLocales = getResources().getStringArray(R.array.available_languages);
        Arrays.sort(availableLocales);
        mLanguagePreference.setEntryValues(availableLocales);

        if (TextUtils.isEmpty(mLanguagePreference.getSummary())) {
            mLanguagePreference.setValue(conf.locale.toString());
            mLanguagePreference.setSummary(WPPrefUtils.getLanguageString(conf.locale.toString(), conf.locale));
        }

        // update details to display in selected locale
        CharSequence[] languageCodes = mLanguagePreference.getEntryValues();
        mLanguagePreference.setEntries(WPPrefUtils.createLanguageDisplayStrings(languageCodes));
        mLanguagePreference.setDetails(WPPrefUtils.createLanguageDetailDisplayStrings(languageCodes, conf.locale.getLanguage()));
        mLanguagePreference.refreshAdapter();
    }

    private boolean handleLanguagePreferenceClick() {
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
                mSettings.edit().putString(LANGUAGE_PREF_KEY, localeMap.get(values[position])).apply();

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

    private void updateEmail(String newEmail) {
        Account account = AccountHelper.getDefaultAccount();
        Map<String, String> params = new HashMap<>();
        params.put(Account.RestParam.toString(Account.RestParam.EMAIL), newEmail);
        account.postAccountSettings(params);
    }

    public void onEventMainThread(PrefsEvents.AccountSettingsChanged event) {
        if (isAdded()) {
            refreshAccountDetails();
        }
    }
}
