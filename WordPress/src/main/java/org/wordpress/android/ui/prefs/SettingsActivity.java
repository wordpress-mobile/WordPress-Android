package org.wordpress.android.ui.prefs;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.passcodelock.PasscodePreferenceFragment;

public class SettingsActivity extends ActionBarActivity {
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

        Bundle passcodeArgs = new Bundle();
        passcodeArgs.putBoolean(PasscodePreferenceFragment.KEY_SHOULD_INFLATE, false);

        mSettingsFragment = new SettingsFragment();
        mPasscodePreferenceFragment = new PasscodePreferenceFragment();
        mPasscodePreferenceFragment.setArguments(passcodeArgs);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, mSettingsFragment)
                    .add(R.id.fragment_container, mPasscodePreferenceFragment)
                    .commit();
        }
    }

    @Override
    public void onStart() {
        super.onStart();

        mPasscodePreferenceFragment.setPreferences(
                mSettingsFragment.findPreference(getString(R.string.pref_key_passcode_toggle)),
                mSettingsFragment.findPreference(getString(R.string.pref_key_change_passcode)));
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
