package org.wordpress.android.ui.themes;

import android.app.AlertDialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.themes.ThemeBrowserFragment.ThemeBrowserFragmentCallback;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.widgets.WPAlertDialogFragment;

import java.util.ArrayList;

/**
 * The theme browser.
 */
public class ThemeBrowserActivity extends AppCompatActivity implements ThemeBrowserFragmentCallback {
    public static final int THEME_FETCH_MAX = 100;
    public static final int ACTIVATE_THEME = 1;
    public static final String THEME_ID = "theme_id";
    public static final String BLOG_ID = "blog_id";
    public static final String THEME_WEB_MODE = "theme_web_mode";

    private boolean mFetchingThemes = false;
    private boolean mIsRunning;
    private ThemeBrowserFragment mThemeBrowserFragment;
    private ThemeSearchFragment mThemeSearchFragment;
    private Theme mCurrentTheme;
    private boolean mIsInSearchMode;

    public static boolean isAccessible() {
        // themes are only accessible to admin wordpress.com users
        Blog blog = WordPress.getCurrentBlog();
        return (blog != null && blog.isAdmin() && blog.isDotcomFlag());
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.THEMES_ACCESSED_THEMES_BROWSER);
        }

        setContentView(R.layout.theme_browser_activity);
        configureToolbar();
        mThemeBrowserFragment = new ThemeBrowserFragment();
        mThemeSearchFragment = new ThemeSearchFragment();
        addFragment();
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsRunning = true;
        ActivityId.trackLastActivity(ActivityId.THEMES);

        fetchThemesIfNoneAvailable();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsRunning = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
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
        if (requestCode == ACTIVATE_THEME) {
            if (resultCode == RESULT_OK) {
                String themeId = data.getStringExtra(THEME_ID);
                activateTheme(themeId);
            }
        }
    }

    public void setIsInSearchMode(boolean isInSearchMode) {
        mIsInSearchMode = isInSearchMode;
    }

    public void fetchThemes() {
        if (mFetchingThemes) {
            return;
        }
        String siteId = getBlogId();
        mFetchingThemes = true;
        int page = 1;
        if (mThemeBrowserFragment != null) {
            page = mThemeBrowserFragment.getPage();
        }
        WordPress.getRestClientUtilsV1_2().getThemes(siteId, THEME_FETCH_MAX, page, new Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        new FetchThemesTask().execute(response);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError response) {
                        if (response.toString().equals(AuthFailureError.class.getName())) {
                            String errorTitle = getString(R.string.theme_auth_error_title);
                            String errorMsg = getString(R.string.theme_auth_error_message);

                            if (mIsRunning) {
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                WPAlertDialogFragment fragment = WPAlertDialogFragment.newAlertDialog(errorMsg,
                                        errorTitle);
                                ft.add(fragment, "alert");
                                ft.commitAllowingStateLoss();
                            }
                            AppLog.d(T.THEMES, "Failed to fetch themes: failed authenticate user");
                        } else {
                            Toast.makeText(ThemeBrowserActivity.this, R.string.theme_fetch_failed, Toast.LENGTH_LONG)
                                    .show();
                            AppLog.d(T.THEMES, "Failed to fetch themes: " + response.toString());
                        }
                        mFetchingThemes = false;
                    }
                }
        );
    }

    public void searchThemes(String searchTerm) {
        String siteId = getBlogId();
        mFetchingThemes = true;
        int page = 1;
        if (mThemeSearchFragment != null) {
            page = mThemeSearchFragment.getPage();
        }

        WordPress.getRestClientUtilsV1_2().getSearchThemes(siteId, THEME_FETCH_MAX, page, searchTerm, new Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        new FetchThemesTask().execute(response);
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError response) {
                        if (response.toString().equals(AuthFailureError.class.getName())) {
                            String errorTitle = getString(R.string.theme_auth_error_title);
                            String errorMsg = getString(R.string.theme_auth_error_message);

                            if (mIsRunning) {
                                FragmentTransaction ft = getFragmentManager().beginTransaction();
                                WPAlertDialogFragment fragment = WPAlertDialogFragment.newAlertDialog(errorMsg,
                                        errorTitle);
                                ft.add(fragment, "alert");
                                ft.commitAllowingStateLoss();
                            }
                            AppLog.d(T.THEMES, "Failed to fetch themes: failed authenticate user");
                        }
                        mFetchingThemes = false;
                    }
                }
        );
    }

    public void fetchCurrentTheme() {
        final String siteId = getBlogId();

        WordPress.getRestClientUtilsV1_1().getCurrentTheme(siteId, new Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            mCurrentTheme = Theme.fromJSONV1_1(response);
                            if (mCurrentTheme != null) {
                                WordPress.wpDB.setCurrentTheme(siteId, mCurrentTheme.getId());
                                mThemeBrowserFragment.setRefreshing(false);
                                if (mThemeBrowserFragment.mCurrentThemeTextView != null) {
                                    mThemeBrowserFragment.mCurrentThemeTextView.setText(mCurrentTheme.getName());
                                    mThemeBrowserFragment.mCurrentThemeId = mCurrentTheme.getId();
                                }
                            }
                        } catch (JSONException e) {
                            AppLog.e(T.THEMES, e);
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError response) {
                        String themeId = WordPress.wpDB.getCurrentThemeId(siteId);
                        mCurrentTheme = WordPress.wpDB.getTheme(siteId, themeId);
                        if (mCurrentTheme != null) {
                            mThemeBrowserFragment.mCurrentThemeTextView.setText(mCurrentTheme.getName());
                            mThemeBrowserFragment.mCurrentThemeId = mCurrentTheme.getId();
                        }
                    }
                }
        );
    }

    private String getBlogId() {
        if (WordPress.getCurrentBlog() == null)
            return "0";
        return String.valueOf(WordPress.getCurrentBlog().getRemoteBlogId());
    }

    private void fetchThemesIfNoneAvailable() {
        if (NetworkUtils.isNetworkAvailable(this) && WordPress.getCurrentBlog() != null
                && WordPress.wpDB.getThemeCount(getBlogId()) == 0) {
            fetchThemes();
            mThemeBrowserFragment.setRefreshing(true);
        }
    }

    private void configureToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setDisplayHomeAsUpEnabled(true);
        actionBar.setTitle(R.string.themes);
    }

    private void addFragment() {
        if (mIsInSearchMode) {
            addBrowserFragment();
            addSearchFragment();
        } else {
            addBrowserFragment();
        }
    }

    private void addBrowserFragment() {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.add(R.id.theme_browser_container, mThemeBrowserFragment);
        fragmentTransaction.commit();
    }

    private void addSearchFragment() {
        FragmentTransaction fragmentTransaction = getFragmentManager().beginTransaction();
        fragmentTransaction.replace(R.id.theme_browser_container, mThemeSearchFragment);
        fragmentTransaction.addToBackStack(null);
        fragmentTransaction.commit();
    }


    private void activateTheme(String themeId) {
        final String siteId = getBlogId();
        final String newThemeId = themeId;

        WordPress.getRestClientUtils().setTheme(siteId, themeId, new Listener() {
            @Override
            public void onResponse(JSONObject response) {
                WordPress.wpDB.setCurrentTheme(siteId, newThemeId);
                Theme newTheme = WordPress.wpDB.getTheme(siteId, newThemeId);
                showAlertDialogOnNewSettingNewTheme(newTheme);
                fetchCurrentTheme();
            }
        }, new ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(getApplicationContext(), R.string.theme_activation_error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAlertDialogOnNewSettingNewTheme(Theme newTheme) {
        AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(this);
        dialogBuilder.setMessage(String.format(getString(R.string.theme_prompt), newTheme.getName(), newTheme.getAuthor()));
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
        boolean isCurrentTheme = mCurrentTheme.getId().equals(mCurrentTheme.getId());
        ThemeWebActivity.openTheme(this, themeId, type, isCurrentTheme);
    }

    @Override
    public void onActivateSelected(String themeId) {
        Theme selectedTheme = WordPress.wpDB.getTheme(getBlogId(), themeId);
        if (selectedTheme.isPremium()) {

        } else {
            activateTheme(themeId);
        }
    }

    @Override
    public void onPreviewSelected(String themeId) {
        startWebActivity(themeId, ThemeWebActivity.ThemeWebActivityType.PREVIEW);
    }

    @Override
    public void onDemoSelected(String themeId) {
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
    public void onCustomizeSelected(String themeId) {
        startWebActivity(themeId, ThemeWebActivity.ThemeWebActivityType.CUSTOMIZE);
    }

    @Override
    public void onSearchClicked() {
        mIsInSearchMode = true;
        addSearchFragment();
    }

    public class FetchThemesTask extends AsyncTask<JSONObject, Void, ArrayList<Theme>> {
        @Override
        protected ArrayList<Theme> doInBackground(JSONObject... args) {
            JSONObject response = args[0];
            final ArrayList<Theme> themes = new ArrayList<>();

            if (response != null) {
                JSONArray array;
                try {
                    array = response.getJSONArray("themes");

                    if (array != null) {
                        int count = array.length();
                        for (int i = 0; i < count; i++) {
                            JSONObject object = array.getJSONObject(i);
                            Theme theme = Theme.fromJSONV1_2(object);
                            if (theme != null) {
                                theme.save();
                                themes.add(theme);
                            }
                        }
                    }
                } catch (JSONException e) {
                    AppLog.e(T.THEMES, e);
                }
            }

            fetchCurrentTheme();

            if (themes.size() > 0) {
                return themes;
            }

            return null;
        }

        @Override
        protected void onPostExecute(final ArrayList<Theme> result) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    mFetchingThemes = false;
                    if (mThemeBrowserFragment.isVisible()) {
                        mThemeBrowserFragment.mEmptyView.setText("No themes");
                        mThemeBrowserFragment.setRefreshing(false);
                    } else if (mThemeSearchFragment.isVisible()) {
                        mThemeSearchFragment.mEmptyView.setText("No themes");
                        mThemeSearchFragment.setRefreshing(false);
                    }
                }
            });
        }
    }
}
