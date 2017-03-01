package org.wordpress.android.ui.prefs;

import android.app.AlertDialog;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteDeleted;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteRemoved;
import org.wordpress.android.networking.ConnectionChangeReceiver;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

/**
 * Activity for configuring blog specific settings.
 */
public class BlogPreferencesActivity extends AppCompatActivity {
    public static final int RESULT_BLOG_REMOVED = RESULT_FIRST_USER;

    private static final String KEY_SETTINGS_FRAGMENT = "settings-fragment";

    // The blog this activity is managing settings for.
    private boolean mBlogDeleted;
    private EditText mUsernameET;
    private EditText mPasswordET;

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;

    private SiteModel mSite;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

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

        if (SiteUtils.isAccessibleViaWPComAPI(mSite)) {
            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setHomeButtonEnabled(true);
                actionBar.setDisplayHomeAsUpEnabled(true);
                actionBar.setTitle(StringUtils.unescapeHTML(SiteUtils.getSiteNameOrHomeURL(mSite)));
            }

            FragmentManager fragmentManager = getFragmentManager();
            Fragment siteSettingsFragment = fragmentManager.findFragmentByTag(KEY_SETTINGS_FRAGMENT);

            if (siteSettingsFragment == null) {
                siteSettingsFragment = new SiteSettingsFragment();
                siteSettingsFragment.setArguments(getIntent().getExtras());
                fragmentManager.beginTransaction()
                        .replace(android.R.id.content, siteSettingsFragment, KEY_SETTINGS_FRAGMENT)
                        .commit();
            }
        } else {
            setContentView(R.layout.blog_preferences);

            ActionBar actionBar = getSupportActionBar();
            if (actionBar != null) {
                actionBar.setTitle(StringUtils.unescapeHTML(SiteUtils.getSiteNameOrHomeURL(mSite)));
                actionBar.setDisplayHomeAsUpEnabled(true);
            }

            mUsernameET = (EditText) findViewById(R.id.username);
            mPasswordET = (EditText) findViewById(R.id.password);
            Button removeBlogButton = (Button) findViewById(R.id.remove_account);

            // remove blog & credentials apply only to dot org
            if (SiteUtils.isAccessibleViaWPComAPI(mSite)) {
                View credentialsRL = findViewById(R.id.sectionContent);
                credentialsRL.setVisibility(View.GONE);
                removeBlogButton.setVisibility(View.GONE);
            } else {
                removeBlogButton.setVisibility(View.VISIBLE);
                removeBlogButton.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        removeBlogWithConfirmation();
                    }
                });
            }

            loadSettingsForBlog();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (SiteUtils.isAccessibleViaWPComAPI(mSite) || mBlogDeleted) {
            return;
        }

        mSite.setUsername(mUsernameET.getText().toString());
        mSite.setPassword(mPasswordET.getText().toString());
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
            if (!event.isConnected()) {
                ToastUtils.showToast(this, getString(R.string.site_settings_disconnected_toast));
            }
            siteSettingsFragment.setEditingEnabled(event.isConnected());

            // TODO: add this back when delete blog is back
            //https://github.com/wordpress-mobile/WordPress-Android/commit/6a90e3fe46e24ee40abdc4a7f8f0db06f157900c
            // Checks for stats widgets that were synched with a blog that could be gone now.
            //            StatsWidgetProvider.updateWidgetsOnLogout(this);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteRemoved(OnSiteRemoved event) {
        setResult(RESULT_BLOG_REMOVED);
        finish();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteDeleted(OnSiteDeleted event) {
        FragmentManager fragmentManager = getFragmentManager();
        SiteSettingsFragment siteSettingsFragment =
                (SiteSettingsFragment) fragmentManager.findFragmentByTag(KEY_SETTINGS_FRAGMENT);

        if (siteSettingsFragment != null) {
            if (event.isError()) {
                siteSettingsFragment.handleDeleteSiteError(event.error);
                return;
            }

            siteSettingsFragment.handleSiteDeleted();
            setResult(RESULT_BLOG_REMOVED);
            finish();
        }
    }

    private void loadSettingsForBlog() {
        mUsernameET.setText(mSite.getUsername());
        mPasswordET.setText(mSite.getPassword());
    }

    /**
     * Remove the blog this activity is managing settings for.
     */
    private void removeBlogWithConfirmation() {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setTitle(getResources().getText(R.string.remove_account));
        dialogBuilder.setMessage(getResources().getText(R.string.sure_to_remove_account));
        dialogBuilder.setPositiveButton(getResources().getText(R.string.yes), new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                mDispatcher.dispatch(SiteActionBuilder.newRemoveSiteAction(mSite));
                mBlogDeleted = true;
            }
        });
        dialogBuilder.setNegativeButton(getResources().getText(R.string.no), null);
        dialogBuilder.setCancelable(false);
        dialogBuilder.create().show();
    }
}
