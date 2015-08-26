package org.wordpress.android.ui.prefs;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.util.ToastUtils;

import de.greenrobot.event.EventBus;

/**
 * Activity for configuring blog specific settings.
 */
public class BlogPreferencesActivity extends AppCompatActivity {
    public static final String ARG_LOCAL_BLOG_ID = "local_blog_id";
    public static final int RESULT_BLOG_REMOVED = RESULT_FIRST_USER;

    private static final String KEY_SETTINGS_FRAGMENT = "settings-fragment";

    // The blog this activity is managing settings for.
    private Blog mBlog;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Integer id = getIntent().getIntExtra(ARG_LOCAL_BLOG_ID, -1);
        mBlog = WordPress.getBlog(id);

        if (mBlog == null) {
            Toast.makeText(this, getString(R.string.blog_not_found), Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayOptions(ActionBar.DISPLAY_SHOW_CUSTOM);
            actionBar.setElevation(getResources().getDimension(R.dimen.appbar_elevation));
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setCustomView(R.layout.site_settings_actionbar);
        }

        FragmentManager fragmentManager = getFragmentManager();
        Fragment siteSettingsFragment = fragmentManager.findFragmentByTag(KEY_SETTINGS_FRAGMENT);

        if (siteSettingsFragment == null) {
            siteSettingsFragment = new SiteSettingsFragment();
            siteSettingsFragment.setArguments(getIntent().getExtras());
            getFragmentManager().beginTransaction()
                    .replace(android.R.id.content, siteSettingsFragment, KEY_SETTINGS_FRAGMENT)
                    .commit();
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onPause() {
        super.onPause();
        WordPress.wpDB.saveBlog(mBlog);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemID = item.getItemId();
        if (itemID == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(ConnectionChangeReceiver.ConnectionChangeEvent event) {
        FragmentManager fragmentManager = getFragmentManager();
        SiteSettingsFragment siteSettingsFragment =
                (SiteSettingsFragment) fragmentManager.findFragmentByTag(KEY_SETTINGS_FRAGMENT);

        if (siteSettingsFragment != null) {
            siteSettingsFragment.allowEditing(event.isConnected());

            if (!event.isConnected()) {
                ToastUtils.showToast(this, getString(R.string.site_settings_disconnected_toast));
            }
        }
    }
}
