package org.wordpress.android.ui.menus;

import android.app.Fragment;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.ui.ActivityLauncher;

import android.content.Intent;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.util.AppLog;

public class MenusActivity extends AppCompatActivity {

    private static final String MENUS_FRAGMENT_KEY = "menusFragment";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.menu_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setTitle(R.string.menus);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState == null) {
            MenusFragment menusFragment = new MenusFragment();
            getFragmentManager().beginTransaction()
                    .add(R.id.layout_fragment_container, menusFragment, MENUS_FRAGMENT_KEY)
                    .commitAllowingStateLoss();
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
    public boolean onOptionsItemSelected(final MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        Fragment fragment =  getFragmentManager()
                .findFragmentByTag(MENUS_FRAGMENT_KEY);
        if (fragment != null && fragment.isVisible()) {
            ((MenusFragment) fragment).dismissFragment();
        } else {
            if (getFragmentManager().getBackStackEntryCount() > 0) {
                getFragmentManager().popBackStack();
            } else {
                super.onBackPressed();
            }
        }
    }

    public void forceBackPressed() {
        super.onBackPressed();
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        AppLog.d(AppLog.T.MENUS, "menus activity new intent");
    }

}
