package org.wordpress.android.ui.menus;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.ui.ActivityLauncher;

import android.content.Intent;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.util.AppLog;

public class MenusActivity extends AppCompatActivity {

    private static final String MENUS_FRAGMENT_KEY = "menusFragment";

    private MenusFragment mMenusFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.menu_activity);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        FragmentManager fragmentManager = getFragmentManager();
        mMenusFragment = (MenusFragment) fragmentManager.findFragmentByTag(MENUS_FRAGMENT_KEY);

        if (mMenusFragment == null) {
            mMenusFragment = new MenusFragment();
            mMenusFragment.setArguments(getIntent().getExtras());
            fragmentManager.beginTransaction()
                    .replace(android.R.id.content, mMenusFragment, MENUS_FRAGMENT_KEY)
                    .commit();
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.MENUS);
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    public void onBackPressed() {
        if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppLog.d(AppLog.T.MENUS, "menus activity new intent");
    }

}
