package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.support.design.widget.Snackbar;
import android.util.DisplayMetrics;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;

import org.wordpress.android.R;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.models.Account;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.util.AnalyticsUtils;

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
    private SummaryEditTextPreference mEmailPreference;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        addPreferencesFromResource(R.xml.settings);

        mPreferenceScreen = (PreferenceScreen) findPreference(getActivity().getString(R.string.pref_key_settings_root));

        mUsernamePreference = findPreference(getString(R.string.pref_key_username));
        mEmailPreference = (SummaryEditTextPreference) findPreference(getString(R.string.pref_key_email));

        mEmailPreference.setOnPreferenceChangeListener(this);
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
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

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
        } else if (preferenceKey.equals(getString(R.string.pref_key_language))) {
            return handleLanguagePreferenceClick();
        }

        return false;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        if (newValue == null) return false;

        if (preference == mEmailPreference) {
            updateEmail(newValue.toString());
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
        Account account = AccountHelper.getDefaultAccount();
        if (account.getPendingEmailChange() && getView() != null) {
            View.OnClickListener clickListener = new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                }
            };

            Snackbar snackbar = Snackbar
                    .make(getView(), getString(R.string.pending_email_change_snackbar, account.getNewEmail()), Snackbar.LENGTH_INDEFINITE).setAction(getString(R.string.undo), clickListener);
            TextView textView = (TextView) snackbar.getView().findViewById(android.support.design.R.id.snackbar_text);
            textView.setMaxLines(4);
            snackbar.show();
        }
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
