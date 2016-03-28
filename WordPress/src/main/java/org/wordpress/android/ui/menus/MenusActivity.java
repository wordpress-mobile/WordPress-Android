package org.wordpress.android.ui.menus;

import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;

import org.wordpress.android.R;
import org.wordpress.android.ui.ActivityLauncher;

public class MenusActivity extends AppCompatActivity {
    private static final String MENUS_FRAGMENT_KEY = "menusFragment";

    private MenusFragment mMenusFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setCustomView(R.layout.preferences_actionbar);
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
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }
}
