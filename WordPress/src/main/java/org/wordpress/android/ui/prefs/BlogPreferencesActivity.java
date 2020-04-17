package org.wordpress.android.ui.prefs;

import android.app.FragmentManager;
import android.os.Bundle;
import android.view.MenuItem;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;

import org.apache.commons.text.StringEscapeUtils;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteDeleted;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;

import javax.inject.Inject;

/**
 * Activity for configuring blog specific settings.
 */
public class BlogPreferencesActivity extends LocaleAwareActivity {
    private static final String KEY_SETTINGS_FRAGMENT = "settings-fragment";

    private SiteModel mSite;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        setContentView(R.layout.site_settings_activity);

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setHomeButtonEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(StringEscapeUtils.unescapeHtml4(SiteUtils.getSiteNameOrHomeURL(mSite)));
        }

        // TODO: android.app.Fragment  is deprecated since Android P.
        // Needs to be replaced with android.support.v4.app.Fragment
        // See https://developer.android.com/reference/android/app/Fragment
        FragmentManager fragmentManager = getFragmentManager();
        android.app.Fragment siteSettingsFragment = fragmentManager.findFragmentByTag(KEY_SETTINGS_FRAGMENT);

        if (siteSettingsFragment == null) {
            siteSettingsFragment = new SiteSettingsFragment();
            siteSettingsFragment.setArguments(getIntent().getExtras());
            fragmentManager.beginTransaction()
                           .replace(R.id.fragment_container, siteSettingsFragment, KEY_SETTINGS_FRAGMENT)
                           .commit();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        mDispatcher.register(this);
    }

    @Override
    protected void onStop() {
        mDispatcher.unregister(this);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
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
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEventMainThread(ConnectionChangeReceiver.ConnectionChangeEvent event) {
        SiteSettingsFragment siteSettingsFragment = getSettingsFragment();
        if (siteSettingsFragment != null) {
            if (!event.isConnected()) {
                ToastUtils.showToast(this, getString(R.string.site_settings_disconnected_toast), Duration.LONG);
            }
            siteSettingsFragment.setEditingEnabled(event.isConnected());

            // TODO: add this back when delete blog is back
            // https://github.com/wordpress-mobile/WordPress-Android/commit/6a90e3fe46e24ee40abdc4a7f8f0db06f157900c
            // Checks for stats widgets that were synched with a blog that could be gone now.
            // StatsWidgetProvider.updateWidgetsOnLogout(this);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteRemoved(OnSiteRemoved event) {
        if (!event.isError()) {
            setResult(SiteSettingsFragment.RESULT_BLOG_REMOVED);
            finish();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteDeleted(OnSiteDeleted event) {
        SiteSettingsFragment siteSettingsFragment = getSettingsFragment();
        if (siteSettingsFragment != null) {
            if (event.isError()) {
                siteSettingsFragment.handleDeleteSiteError(event.error);
                return;
            }

            siteSettingsFragment.handleSiteDeleted();
            setResult(SiteSettingsFragment.RESULT_BLOG_REMOVED);
            finish();
        }
    }

    private SiteSettingsFragment getSettingsFragment() {
        FragmentManager fragmentManager = getFragmentManager();
        return (SiteSettingsFragment) fragmentManager.findFragmentByTag(KEY_SETTINGS_FRAGMENT);
    }
}
