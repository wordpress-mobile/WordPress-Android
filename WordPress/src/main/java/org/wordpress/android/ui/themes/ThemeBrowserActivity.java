package org.wordpress.android.ui.themes;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;

import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker.Stat;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.action.ThemeAction;
import org.wordpress.android.fluxc.generated.ThemeActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.model.ThemeModel;
import org.wordpress.android.fluxc.store.ThemeStore;
import org.wordpress.android.fluxc.store.ThemeStore.SearchThemesPayload;
import org.wordpress.android.fluxc.store.ThemeStore.ActivateThemePayload;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.ui.themes.ThemeBrowserFragment.ThemeBrowserFragmentCallback;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPAlertDialogFragment;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

public class ThemeBrowserActivity extends AppCompatActivity implements ThemeBrowserFragmentCallback {
    public static boolean isAccessible(SiteModel site) {
        // themes are only accessible to admin wordpress.com users
        return site != null && site.isUsingWpComRestApi() && site.getHasCapabilityEditThemeOptions();
    }

    public static final int ACTIVATE_THEME = 1;
    public static final String THEME_ID = "theme_id";

    // refresh WP.com themes every 3 days
    private static final long WP_COM_THEMES_SYNC_TIMEOUT = 1000 * 60 * 60 * 24 * 3;

    private static final String IS_IN_SEARCH_MODE = "is_in_search_mode";
    private static final String ALERT_TAB = "alert";

    private boolean mIsRunning;
    private ThemeBrowserFragment mThemeBrowserFragment;
    private ThemeSearchFragment mThemeSearchFragment;
    private ThemeModel mCurrentTheme;
    private boolean mIsInSearchMode;
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
            if (mSite != null) {
                AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_ACCESSED_THEMES_BROWSER, mSite);
                mThemeBrowserFragment = ThemeBrowserFragment.newInstance(mSite);
                mThemeSearchFragment = ThemeSearchFragment.newInstance(mSite);
                addBrowserFragment();
            }
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
            setIsInSearchMode(savedInstanceState.getBoolean(IS_IN_SEARCH_MODE));
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.theme_browser_activity);

        // fetch most recent themes data
        if (!mIsInSearchMode) {
            fetchInstalledThemesIfJetpackSite();
            fetchWpComThemesIfSyncTimedOut(false);
        }

        showToolbar();
    }

    @Override
    protected void onResume() {
        super.onResume();
        showCorrectToolbar();
        mIsRunning = true;
        ActivityId.trackLastActivity(ActivityId.THEMES);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsRunning = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(IS_IN_SEARCH_MODE, mIsInSearchMode);
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
        FragmentManager fm = getFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
        } else {
            super.onBackPressed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
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
    public void onSearchClicked() {
        setIsInSearchMode(true);
        AnalyticsUtils.trackWithSiteDetails(Stat.THEMES_ACCESSED_SEARCH, mSite);
        addSearchFragment();
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemesChanged(ThemeStore.OnThemesChanged event) {
        // always unset refreshing status to remove progress indicator
        if (mThemeBrowserFragment != null) {
            mThemeBrowserFragment.setRefreshing(false);
            mThemeBrowserFragment.refreshView();
        }

        if (event.origin == ThemeAction.FETCH_INSTALLED_THEMES) {
            mIsFetchingInstalledThemes = false;
        }
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error fetching themes: " + event.error.message);
            ToastUtils.showToast(this, R.string.theme_fetch_failed, ToastUtils.Duration.SHORT);
        } else {
            if (event.origin == ThemeAction.FETCH_WP_COM_THEMES) {
                AppLog.d(T.THEMES, "WordPress.com Theme fetch successful!");
                AppPrefs.setLastWpComThemeSync(System.currentTimeMillis());
            } else if (event.origin == ThemeAction.FETCH_INSTALLED_THEMES) {
                AppLog.d(T.THEMES, "Installed themes fetch successful!");
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onCurrentThemeFetched(ThemeStore.OnCurrentThemeFetched event) {
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
            AppLog.d(T.THEMES, "Current theme is " + mCurrentTheme.getName());
            updateCurrentThemeView();

            if (mThemeSearchFragment != null && mThemeSearchFragment.isVisible()) {
                mThemeSearchFragment.setRefreshing(false);
            }
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemesSearched(ThemeStore.OnThemesSearched event) {
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error searching themes: " + event.error.message);
            if (event.error.type == ThemeStore.ThemeErrorType.UNAUTHORIZED) {
                AppLog.d(T.THEMES, getString(R.string.theme_auth_error_authenticate));
                String errorTitle = getString(R.string.theme_auth_error_title);
                String errorMsg = getString(R.string.theme_auth_error_message);

                if (mIsRunning) {
                    FragmentTransaction ft = getFragmentManager().beginTransaction();
                    WPAlertDialogFragment fragment = WPAlertDialogFragment.newAlertDialog(errorMsg,
                            errorTitle);
                    ft.add(fragment, ALERT_TAB);
                    ft.commitAllowingStateLoss();
                }
            }
        } else {
            AppLog.d(T.THEMES, "Themes search successful!");
            mThemeSearchFragment.setSearchResults(event.searchResults);
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeInstalled(ThemeStore.OnThemeInstalled event) {
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error installing theme: " + event.error.message);
        } else {
            AppLog.d(T.THEMES, "Theme installation successful! Installed theme: " + event.theme.getName());
            activateTheme(event.theme.getThemeId());
        }
    }

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onThemeActivated(ThemeStore.OnThemeActivated event) {
        if (event.isError()) {
            AppLog.e(T.THEMES, "Error activating theme: " + event.error.message);
            ToastUtils.showToast(this, R.string.theme_activation_error, ToastUtils.Duration.SHORT);
        } else {
            AppLog.d(T.THEMES, "Theme activation successful! New theme: " + event.theme.getName());

            mCurrentTheme = mThemeStore.getActiveThemeForSite(event.site);
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
        if (mCurrentTheme != null && mThemeBrowserFragment != null && mThemeBrowserFragment.getCurrentThemeTextView() != null) {
            String text = TextUtils.isEmpty(mCurrentTheme.getName()) ? getString(R.string.unknown) : mCurrentTheme.getName();
            mThemeBrowserFragment.getCurrentThemeTextView().setText(text);
            mThemeBrowserFragment.setCurrentThemeId(mCurrentTheme.getThemeId());
        }
    }

    public void setIsInSearchMode(boolean isInSearchMode) {
        mIsInSearchMode = isInSearchMode;
    }

    public void searchThemes(String searchTerm) {
        if (TextUtils.isEmpty(searchTerm)) {
            return;
        }
        SearchThemesPayload payload = new SearchThemesPayload(searchTerm);
        mDispatcher.dispatch(ThemeActionBuilder.newSearchThemesAction(payload));
    }

    public void fetchCurrentTheme() {
        mDispatcher.dispatch(ThemeActionBuilder.newFetchCurrentThemeAction(mSite));
    }

    public void fetchWpComThemesIfSyncTimedOut(boolean force) {
        long currentTime = System.currentTimeMillis();
        if (force || currentTime - AppPrefs.getLastWpComThemeSync() > WP_COM_THEMES_SYNC_TIMEOUT) {
            mDispatcher.dispatch(ThemeActionBuilder.newFetchWpComThemesAction());
        }
    }

    public void fetchInstalledThemesIfJetpackSite() {
        if (mSite.isJetpackConnected() && mSite.isUsingWpComRestApi() && !mIsFetchingInstalledThemes) {
            mDispatcher.dispatch(ThemeActionBuilder.newFetchInstalledThemesAction(mSite));
            mIsFetchingInstalledThemes = true;
        }
    }

    public void activateTheme(String themeId) {
        if (!mSite.isUsingWpComRestApi()) {
            AppLog.i(T.THEMES, "Theme activation requires a site using WP.com REST API. Aborting request.");
            return;
        }

        ThemeModel theme = mThemeStore.getInstalledThemeByThemeId(themeId);
        if (theme == null) {
            theme = mThemeStore.getWpComThemeByThemeId(themeId);
            if (theme == null) {
                AppLog.w(T.THEMES, "Theme unavailable to activate. Fetch it and try again.");
                return;
            }

            if (mSite.isJetpackConnected()) {
                // first install the theme, then activate it
                mDispatcher.dispatch(ThemeActionBuilder.newInstallThemeAction(new ActivateThemePayload(mSite, theme)));
                return;
            }
        }

        mDispatcher.dispatch(ThemeActionBuilder.newActivateThemeAction(new ActivateThemePayload(mSite, theme)));
    }

    protected void setThemeBrowserFragment(ThemeBrowserFragment themeBrowserFragment) {
        mThemeBrowserFragment = themeBrowserFragment;
    }

    protected void setThemeSearchFragment(ThemeSearchFragment themeSearchFragment) {
        mThemeSearchFragment = themeSearchFragment;
    }

    protected void showToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setTitle(R.string.themes);
            findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
            findViewById(R.id.toolbar_search).setVisibility(View.GONE);
        }
    }

    private void showCorrectToolbar() {
        if (mIsInSearchMode) {
            showSearchToolbar();
        } else {
            hideSearchToolbar();
        }
    }

    private void showSearchToolbar() {
        Toolbar toolbarSearch = (Toolbar) findViewById(R.id.toolbar_search);
        setSupportActionBar(toolbarSearch);
        toolbarSearch.setTitle("");
        findViewById(R.id.toolbar).setVisibility(View.GONE);
        findViewById(R.id.toolbar_search).setVisibility(View.VISIBLE);
    }

    private void hideSearchToolbar() {
        findViewById(R.id.toolbar).setVisibility(View.VISIBLE);
        findViewById(R.id.toolbar_search).setVisibility(View.GONE);
    }

    private void addBrowserFragment() {
        if (mThemeBrowserFragment == null) {
            mThemeBrowserFragment = new ThemeBrowserFragment();
        }
        showToolbar();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.theme_browser_container, mThemeBrowserFragment);
        fragmentTransaction.commit();
    }

    private void addSearchFragment() {
        if (mThemeSearchFragment == null) {
            mThemeSearchFragment = ThemeSearchFragment.newInstance(mSite);
        }
        showSearchToolbar();
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.theme_browser_container, mThemeSearchFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }

    private void showAlertDialogOnNewSettingNewTheme(ThemeModel newTheme) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);

        String thanksMessage = String.format(getString(R.string.theme_prompt), newTheme.getName());
        if (!TextUtils.isEmpty(newTheme.getAuthorName())) {
            String append = String.format(getString(R.string.theme_by_author_prompt_append), newTheme.getAuthorName());
            thanksMessage = thanksMessage + " " + append;
        }

        dialogBuilder.setMessage(thanksMessage);
        dialogBuilder.setNegativeButton(R.string.theme_done, null);
        dialogBuilder.setPositiveButton(R.string.theme_manage_site, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                finish();
            }
        });

        AlertDialog alertDialog = dialogBuilder.create();
        alertDialog.show();
    }

    private void startWebActivity(String themeId, ThemeWebActivity.ThemeWebActivityType type) {
        String toastText = getString(R.string.no_network_message);

        if (NetworkUtils.isNetworkAvailable(this)) {
            ThemeModel theme = TextUtils.isEmpty(themeId) ? null : mThemeStore.getWpComThemeByThemeId(themeId.replace("-wpcom", ""));
            if (theme != null) {
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
                return;
            } else {
                toastText = getString(R.string.could_not_load_theme);
            }
        }

        ToastUtils.showToast(this, toastText, ToastUtils.Duration.SHORT);
    }

    private boolean isActiveThemeForSite(@NonNull String themeId) {
        final ThemeModel storedActiveTheme = mThemeStore.getActiveThemeForSite(mSite);
        return storedActiveTheme != null && themeId.equals(storedActiveTheme.getThemeId().replace("-wpcom", ""));
    }
}
