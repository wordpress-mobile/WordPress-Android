package org.wordpress.android.ui.prefs;

import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.passcodelock.PasscodePreferenceFragment;

public class AccountSettingsActivity extends AppCompatActivity {
    private static final String KEY_ACCOUNT_SETTINGS_FRAGMENT = "account-settings-fragment";
    private static final String KEY_PASSCODE_FRAGMENT = "passcode-fragment";

    private AccountSettingsFragment mAccountSettingsFragment;
    private PasscodePreferenceFragment mPasscodePreferenceFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fragmentManager = getFragmentManager();
        mAccountSettingsFragment = (AccountSettingsFragment) fragmentManager.findFragmentByTag(KEY_ACCOUNT_SETTINGS_FRAGMENT);
        mPasscodePreferenceFragment = (PasscodePreferenceFragment) fragmentManager.findFragmentByTag(KEY_PASSCODE_FRAGMENT);
        if (mAccountSettingsFragment == null || mPasscodePreferenceFragment == null) {
            Bundle passcodeArgs = new Bundle();
            passcodeArgs.putBoolean(PasscodePreferenceFragment.KEY_SHOULD_INFLATE, false);
            mAccountSettingsFragment = new AccountSettingsFragment();
            mPasscodePreferenceFragment = new PasscodePreferenceFragment();
            mPasscodePreferenceFragment.setArguments(passcodeArgs);

            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, mPasscodePreferenceFragment, KEY_PASSCODE_FRAGMENT)
                    .add(android.R.id.content, mAccountSettingsFragment, KEY_ACCOUNT_SETTINGS_FRAGMENT)
                    .commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Preference togglePref =
                mAccountSettingsFragment.findPreference(getString(org.wordpress.passcodelock.R.string
                        .pref_key_passcode_toggle));
        Preference changePref =
                mAccountSettingsFragment.findPreference(getString(org.wordpress.passcodelock.R.string
                        .pref_key_change_passcode));

        if (togglePref != null && changePref != null) {
            mPasscodePreferenceFragment.setPreferences(togglePref, changePref);
        }
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }
}
