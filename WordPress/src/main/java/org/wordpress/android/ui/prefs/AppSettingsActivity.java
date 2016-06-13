package org.wordpress.android.ui.prefs;

import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.passcodelock.AppLockManager;
import org.wordpress.passcodelock.PasscodePreferenceFragment;

public class AppSettingsActivity extends AppCompatActivity {
    private static final String KEY_APP_SETTINGS_FRAGMENT = "app-settings-fragment";
    private static final String KEY_PASSCODE_FRAGMENT = "passcode-fragment";

    private AppSettingsFragment mAppSettingsFragment;
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
        mAppSettingsFragment = (AppSettingsFragment) fragmentManager.findFragmentByTag(KEY_APP_SETTINGS_FRAGMENT);
        mPasscodePreferenceFragment = (PasscodePreferenceFragment) fragmentManager.findFragmentByTag(KEY_PASSCODE_FRAGMENT);
        if (mAppSettingsFragment == null || mPasscodePreferenceFragment == null) {
            Bundle passcodeArgs = new Bundle();
            passcodeArgs.putBoolean(PasscodePreferenceFragment.KEY_SHOULD_INFLATE, false);
            mAppSettingsFragment = new AppSettingsFragment();
            mPasscodePreferenceFragment = new PasscodePreferenceFragment();
            mPasscodePreferenceFragment.setArguments(passcodeArgs);

            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, mPasscodePreferenceFragment, KEY_PASSCODE_FRAGMENT)
                    .add(android.R.id.content, mAppSettingsFragment, KEY_APP_SETTINGS_FRAGMENT)
                    .commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        Preference togglePref =
                mAppSettingsFragment.findPreference(getString(org.wordpress.passcodelock.R.string
                        .pref_key_passcode_toggle));
        Preference changePref =
                mAppSettingsFragment.findPreference(getString(org.wordpress.passcodelock.R.string
                        .pref_key_change_passcode));

        if (togglePref != null && changePref != null) {
            mPasscodePreferenceFragment.setPreferences(togglePref, changePref);
            ((SwitchPreference) togglePref).setChecked(
                    AppLockManager.getInstance().getAppLock().isPasswordLocked());
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
