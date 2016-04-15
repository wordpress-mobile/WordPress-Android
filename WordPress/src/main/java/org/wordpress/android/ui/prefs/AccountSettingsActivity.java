package org.wordpress.android.ui.prefs;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;

import org.wordpress.android.ui.ActivityLauncher;

public class AccountSettingsActivity extends AppCompatActivity {
    private static final String KEY_ACCOUNT_SETTINGS_FRAGMENT = "account-settings-fragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fragmentManager = getFragmentManager();
        AccountSettingsFragment accountSettingsFragment = (AccountSettingsFragment) fragmentManager.findFragmentByTag(KEY_ACCOUNT_SETTINGS_FRAGMENT);
        if (accountSettingsFragment == null) {
            accountSettingsFragment = new AccountSettingsFragment();

            fragmentManager.beginTransaction()
                    .add(android.R.id.content, accountSettingsFragment, KEY_ACCOUNT_SETTINGS_FRAGMENT)
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
