package org.wordpress.android.ui.themes;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.Toolbar;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.jetbrains.annotations.NotNull;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.ThemeAction;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.fluxc.store.ThemeStore.OnCurrentThemeFetched;
import org.wordpress.android.fluxc.store.ThemeStore.OnSiteThemesChanged;
import org.wordpress.android.fluxc.store.ThemeStore.OnThemeActivated;
import org.wordpress.android.fluxc.store.ThemeStore.OnThemeInstalled;
import org.wordpress.android.fluxc.store.ThemeStore.OnWpComThemesChanged;
import org.wordpress.android.fluxc.store.ThemeStore.SiteThemePayload;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.LocaleAwareActivity;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.themes.ThemeBrowserFragment.ThemeBrowserFragmentCallback;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.CrashLoggingUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.analytics.AnalyticsUtils;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ThemeBrowserActivity extends LocaleAwareActivity implements ThemeBrowserFragmentCallback {
    public static boolean isAccessible(SiteModel site) {
        // themes are only accessible to admin wordpress.com users
        return site != null && site.isUsingWpComRestApi() && site.getHasCapabilityEditThemeOptions();
    }

    public static final int ACTIVATE_THEME = 1;
    public static final String THEME_ID = "theme_id";

    // refresh WP.com themes every 3 days
    private static final long WP_COM_THEMES_SYNC_TIMEOUT = 1000 * 60 * 60 * 24 * 3;

    private ThemeBrowserFragment mThemeBrowserFragment;
    private ThemeModel mCurrentTheme;
    private boolean mIsFetchingInstalledThemes;
    private SiteModel mSite;

    @Inject ThemeStore mThemeStore;
    @Inject Dispatcher mDispatcher;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);
        mDispatcher.register(this);

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

        setContentView(R.layout.theme_browser_activity);

        if (savedInstanceState == null) {
            addBrowserFragment();
            fetchInstalledThemesIfJetpackSite();
            fetchWpComThemesIfSyncTimedOut(false);
        } else {
            mThemeBrowserFragment =
                    (ThemeBrowserFragment) getSupportFragmentManager().findFragmentByTag(ThemeBrowserFragment.TAG);
        }

        Toolbar toolbar = findViewById(R.id.toolbar_main);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        ActivityId.trackLastActivity(ActivityId.THEMES);
        fetchCurrentTheme();
    }

    @Override
    protected void onSaveInstanceState(@NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putSerializable(WordPress.SITE, mSite);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == ACTIVATE_THEME && resultCode == RESULT_OK && data != null) {
            String themeId = data.getStringExtra(THEME_ID);
            if (!TextUtils.isEmpty(themeId)) {
                activateTheme(themeId);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mDispatcher.unregister(this);
    }

    @Override
    public void onActivateSelected(String themeId) {
        activateTheme(themeId);
    }

    @Override
    public void onTryAndCustomizeSelected(String themeId) {
        if (mThemeBrowserFragment != null) {
            mThemeBrowserFragment.completeQuickStartCustomizeTask();
        }
        startWebActivity(themeId, ThemeWebActivity.ThemeWebActivityType.PREVIEW);
    }

    @Override
    public void onViewSelected(String themeId) {
        startWebActivity(themeId, ThemeWebActivity.ThemeWebActivityType.DEMO);
    }

    @Override
    public void onDetailsSelected(String themeId) {
        startWebActivity(themeId, ThemeWebActivity.ThemeWebActivityType.DETAILS);
    }

    @Override
    public void onSupportSelected(String themeId) {
        startWebActivity(themeId, ThemeWebActivity.ThemeWebActivityType.SUPPORT);
    }

    @Override
    public void onSwipeToRefresh() {
        fetchInstalledThemesIfJetpackSite();
        fetchWpComThemesIfSyncTimedOut(true);
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onWpComThemesChanged(OnWpComThemesChanged event) {
        // always unset refreshing status to remove progress indicator
        if (mThemeBrowserFragment != null) {
            mThemeBrowserFragment.setRefreshing(false);
            mThemeBrowserFragment.refreshView();
        }

        if (event.isError()) {
            AppLog.e(T.THEMES, "Error fetching themes: " + event.error.message);
            ToastUtils.showToast(this, R.string.theme_fetch_failed, ToastUtils.Duration.SHORT);
        } else {
            AppLog.d(T.THEMES, "WordPress.com Theme fetch successful!");
        }
        AppPrefs.setLastWpComThemeSync(System.currentTimeMillis());
    }


    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteThemesChanged(OnSiteThemesChanged event) {
        if (event.site.getId() != mSite.getId()) {
            // ignore this event as it's not related to the currently selected site
            return;
        }
        if (event.origin == ThemeAction.FETCH_INSTALLED_THEMES) {
            // always unset refreshing status to remove progress indicator
            if (mThemeBrowserFragment != null) {
                mThemeBrowserFragment.setRefreshing(false);
                mThemeBrowserFragment.refreshView();
            }

            mIsFetchingInstalledThemes = false;

            if (event.isError()) {
                AppLog.e(T.THEMES, "Error fetching themes: " + event.error.message);
                ToastUtils.showToast(this, R.string.theme_fetch_failed, ToastUtils.Duration.SHORT);
            } else {
                AppLog.d(T.THEMES, "Installed themes fetch successful!");
            }
        } else if (event.origin == ThemeAction.REMOVE_SITE_THEMES) {
            // Since this is a logout event, we don't need to do anything
            AppLog.d(T.THEMES, "Site themes removed for site: " + event.site.getDisplayName());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentThemeFetched(OnCurrentThemeFetched event) {
        if (event.site.getId() != mSite.getId()) {
            // ignore this event as it's not related to the currently selected site
            return;
        }
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error fetching current theme: " + event.error.message);
            ToastUtils.showToast(this, R.string.theme_fetch_failed, ToastUtils.Duration.SHORT);

            // set the new current theme to update header
            if (mCurrentTheme != null && mThemeBrowserFragment != null) {
                if (mThemeBrowserFragment.getCurrentThemeTextView() != null) {
                    mThemeBrowserFragment.getCurrentThemeTextView().setText(mCurrentTheme.getName());
                    mThemeBrowserFragment.setCurrentThemeId(mCurrentTheme.getThemeId());
                }
            }
        } else {
            AppLog.d(T.THEMES, "Current Theme fetch successful!");
            mCurrentTheme = mThemeStore.getActiveThemeForSite(event.site);
            AppLog.d(T.THEMES, "Current theme is " + (mCurrentTheme == null ? "(null)" : mCurrentTheme.getName()));
            updateCurrentThemeView();
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeInstalled(OnThemeInstalled event) {
        if (event.site.getId() != mSite.getId()) {
            // ignore this event as it's not related to the currently selected site
            return;
        }
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error installing theme: " + event.error.message);
        } else {
            AppLog.d(T.THEMES, "Theme installation successful! Installed theme: " + event.theme.getName());
            activateTheme(event.theme.getThemeId());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeActivated(OnThemeActivated event) {
        if (event.site.getId() != mSite.getId()) {
            // ignore this event as it's not related to the currently selected site
            return;
        }
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error activating theme: " + event.error.message);
            ToastUtils.showToast(this, R.string.theme_activation_error, ToastUtils.Duration.SHORT);
        } else {
            AppLog.d(T.THEMES, "Theme activation successful! New theme: " + event.theme.getName());

            mCurrentTheme = mThemeStore.getActiveThemeForSite(event.site);
            if (mCurrentTheme == null) {
                CrashLoggingUtils.log("NOT A CRASH: OnThemeActivated event is ignored as `getActiveThemeForSite` "
                                      + "returned null.");
                return;
            }
            updateCurrentThemeView();

            Map<String, Object> themeProperties = new HashMap<>();
            themeProperties.put(THEME_ID, mCurrentTheme.getThemeId());
            AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_CHANGED_THEME, mSite, themeProperties);

            if (!isFinishing()) {
                showAlertDialogOnNewSettingNewTheme(mCurrentTheme);
            }
        }
    }

    private void updateCurrentThemeView() {
        if (mCurrentTheme != null && mThemeBrowserFragment != null
            && mThemeBrowserFragment.getCurrentThemeTextView() != null) {
            String text =
                    TextUtils.isEmpty(mCurrentTheme.getName()) ? getString(R.string.unknown) : mCurrentTheme.getName();
            mThemeBrowserFragment.getCurrentThemeTextView().setText(text);
            mThemeBrowserFragment.setCurrentThemeId(mCurrentTheme.getThemeId());
        }
    }

    private void fetchCurrentTheme() {
        mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(mSite));
    }

    private void fetchWpComThemesIfSyncTimedOut(boolean force) {
        long currentTime = System.currentTimeMillis();
        if (force || currentTime - AppPrefs.getLastWpComThemeSync() > WP_COM_THEMES_SYNC_TIMEOUT) {
            mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction());
        }
    }

    private void fetchInstalledThemesIfJetpackSite() {
        if (mSite.isJetpackConnected() && mSite.isUsingWpComRestApi() && !mIsFetchingInstalledThemes) {
            mDispatcher.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(mSite));
            mIsFetchingInstalledThemes = true;
        }
    }

    private void activateTheme(String themeId) {
        if (!mSite.isUsingWpComRestApi()) {
            AppLog.i(T.THEMES, "Theme activation requires a site using WP.com REST API. Aborting request.");
            return;
        }

        ThemeModel theme = mThemeStore.getInstalledThemeByThemeId(mSite, themeId);
        if (theme == null) {
            theme = mThemeStore.getWpComThemeByThemeId(themeId);
            if (theme == null) {
                AppLog.w(T.THEMES, "Theme unavailable to activate. Fetch it and try again.");
                return;
            }

            if (mSite.isJetpackConnected()) {
                // first install the theme, then activate it
                mDispatcher.dispatch(ThemeActionBuilder.newInstallThemeAction(new SiteThemePayload(mSite, theme)));
                return;
            }
        }

        mDispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(new SiteThemePayload(mSite, theme)));
    }

    private void addBrowserFragment() {
        mThemeBrowserFragment = ThemeBrowserFragment.newInstance(mSite);
        getSupportFragmentManager().beginTransaction()
                                   .add(R.id.theme_browser_container, mThemeBrowserFragment, ThemeBrowserFragment.TAG)
                                   .commit();
    }

    private void showAlertDialogOnNewSettingNewTheme(ThemeModel newTheme) {
        AlertDialog.Builder dialogBuilder = new MaterialAlertDialogBuilder(this);

        String thanksMessage = String.format(getString(R.string.theme_prompt), newTheme.getName());
        if (!TextUtils.isEmpty(newTheme.getAuthorName())) {
            String append = String.format(getString(R.string.theme_by_author_prompt_append), newTheme.getAuthorName());
            thanksMessage = thanksMessage + " " + append;
        }

        dialogBuilder.setMessage(thanksMessage);
        dialogBuilder.setNegativeButton(R.string.theme_done, null);
        dialogBuilder.setPositiveButton(R.string.theme_manage_site, (dialog, which) -> finish());

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void startWebActivity(String themeId, ThemeWebActivity.ThemeWebActivityType type) {
        ThemeModel theme =
                TextUtils.isEmpty(themeId) ? null : mThemeStore.getWpComThemeByThemeId(themeId.replace("-wpcom", ""));
        if (theme == null) {
            theme = mThemeStore.getInstalledThemeByThemeId(mSite, themeId);
            if (theme == null) {
                ToastUtils.showToast(this, R.string.could_not_load_theme);
                return;
            }
        }

        Map<String, Object> themeProperties = new HashMap<>();
        themeProperties.put(THEME_ID, themeId);
        theme.setActive(isActiveThemeForSite(theme.getThemeId()));

        switch (type) {
            case PREVIEW:
                AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_PREVIEWED_SITE, mSite, themeProperties);
                break;
            case DEMO:
                AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_DEMO_ACCESSED, mSite, themeProperties);
                break;
            case DETAILS:
                AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_DETAILS_ACCESSED, mSite, themeProperties);
                break;
            case SUPPORT:
                AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_SUPPORT_ACCESSED, mSite, themeProperties);
                break;
        }
        ThemeWebActivity.openTheme(this, mSite, theme, type);
    }

    private boolean isActiveThemeForSite(@NonNull String themeId) {
        final ThemeModel storedActiveTheme = mThemeStore.getActiveThemeForSite(mSite);
        return storedActiveTheme != null && themeId.equals(storedActiveTheme.getThemeId().replace("-wpcom", ""));
    }
}
