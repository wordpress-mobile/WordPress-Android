package org.wordpress.android.ui.prefs;

import android.app.FragmentManager;
import android.os.Bundle;
import android.preference.Preference;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.passcodelock.PasscodePreferenceFragment;

public class SettingsActivity extends AppCompatActivity {
    private static final String KEY_SETTINGS_FRAGMENT = "settings-fragment";
    private static final String KEY_PASSCODE_FRAGMENT = "passcode-fragment";

    private SettingsFragment mSettingsFragment;
    private PasscodePreferenceFragment mPasscodePreferenceFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0.0f);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
        setContentView(R.layout.settings_activity);

        FragmentManager fragmentManager = getFragmentManager();
        if (savedInstanceState == null) {
            Bundle passcodeArgs = new Bundle();
            passcodeArgs.putBoolean(PasscodePreferenceFragment.KEY_SHOULD_INFLATE, false);
            mSettingsFragment = new SettingsFragment();
            mPasscodePreferenceFragment = new PasscodePreferenceFragment();
            mPasscodePreferenceFragment.setArguments(passcodeArgs);

            fragmentManager.beginTransaction()
                    .add(R.id.fragment_container, mSettingsFragment)
                    .add(R.id.fragment_container, mPasscodePreferenceFragment)
                    .commit();
        } else {
            mSettingsFragment = (SettingsFragment)
                    fragmentManager.getFragment(savedInstanceState, KEY_SETTINGS_FRAGMENT);
            mPasscodePreferenceFragment = (PasscodePreferenceFragment)
                    fragmentManager.getFragment(savedInstanceState, KEY_PASSCODE_FRAGMENT);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);

        getFragmentManager().putFragment(
                savedInstanceState, KEY_SETTINGS_FRAGMENT, mSettingsFragment);
        getFragmentManager().putFragment(
                savedInstanceState, KEY_PASSCODE_FRAGMENT, mPasscodePreferenceFragment);
    }

    @Override
    public void onStart() {
        super.onStart();

        Preference togglePref =
                mSettingsFragment.findPreference(getString(R.string.pref_key_passcode_toggle));
        Preference changePref =
                mSettingsFragment.findPreference(getString(R.string.pref_key_change_passcode));

        if (togglePref != null && changePref != null) {
            mPasscodePreferenceFragment.setPreferences(togglePref, changePref);
        }
    }

    @Override
    public void onBackPressed() {
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStack();
        } else {
            super.onBackPressed();
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
