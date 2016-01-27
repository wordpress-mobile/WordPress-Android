package org.wordpress.android.ui.prefs;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.passcodelock.PasscodePreferenceFragment;

public class SettingsActivity extends AppCompatActivity {
    private static final String KEY_SETTINGS_FRAGMENT = "settings-fragment";
    private static final String KEY_PASSCODE_FRAGMENT = "passcode-fragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fragmentManager = getFragmentManager();
        Fragment settingsFragment = fragmentManager.findFragmentByTag(KEY_SETTINGS_FRAGMENT);
        Fragment passcodeFragment = fragmentManager.findFragmentByTag(KEY_PASSCODE_FRAGMENT);
        if (settingsFragment == null || passcodeFragment == null) {
            Bundle passcodeArgs = new Bundle();
            passcodeArgs.putBoolean(PasscodePreferenceFragment.KEY_SHOULD_INFLATE, false);
            settingsFragment = new SettingsFragment();
            passcodeFragment = new PasscodePreferenceFragment();
            passcodeFragment.setArguments(passcodeArgs);

            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, passcodeFragment, KEY_PASSCODE_FRAGMENT)
                    .add(android.R.id.content, settingsFragment, KEY_SETTINGS_FRAGMENT)
                    .commit();
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
