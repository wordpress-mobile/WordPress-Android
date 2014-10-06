package org.wordpress.android.ui.themes;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
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
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.HorizontalTabView;
import org.wordpress.android.ui.HorizontalTabView.TabListener;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.posts.PostsActivity;
import org.wordpress.android.ui.themes.ThemeDetailsFragment.ThemeDetailsFragmentCallback;
import org.wordpress.android.ui.themes.ThemePreviewFragment.ThemePreviewFragmentCallback;
import org.wordpress.android.ui.themes.ThemeTabFragment.ThemeSortType;
import org.wordpress.android.ui.themes.ThemeTabFragment.ThemeTabFragmentCallback;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.widgets.WPAlertDialogFragment;
import org.wordpress.android.analytics.AnalyticsTracker;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

/**
 * The theme browser. Accessible via side menu drawer.
 */
public class ThemeBrowserActivity extends WPActionBarActivity implements
        ThemeTabFragmentCallback, ThemeDetailsFragmentCallback, ThemePreviewFragmentCallback,
        TabListener {
    private HorizontalTabView mTabView;
    private ThemePagerAdapter mThemePagerAdapter;
    private ViewPager mViewPager;
    private ThemeSearchFragment mSearchFragment;
    private ThemePreviewFragment mPreviewFragment;
    private ThemeDetailsFragment mDetailsFragment;
    private boolean mFetchingThemes = false;
    private boolean mIsRunning;

    private boolean mIsActivatingTheme = false;
    private static final String KEY_IS_ACTIVATING_THEME = "is_activating_theme";

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

        setTitle(R.string.themes);

        createMenuDrawer(R.layout.theme_browser_activity);

        mThemePagerAdapter = new ThemePagerAdapter(getFragmentManager());

        final ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setHomeButtonEnabled(true);
        }

        mViewPager = (ViewPager) findViewById(R.id.theme_browser_pager);
        mViewPager.setAdapter(mThemePagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                mTabView.setSelectedTab(position);
            }
        });

        mTabView = (HorizontalTabView) findViewById(R.id.horizontalTabView1);
        mTabView.setTabListener(this);

        int count = ThemeSortType.values().length;
        for (int i = 0; i < count; i++) {
            String title = ThemeSortType.values()[i].getTitle();

            mTabView.addTab(mTabView.newTab().setText(title));
        }
        mTabView.setSelectedTab(0);

        FragmentManager fm = getFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);
        setupBaseLayout();
        mPreviewFragment = (ThemePreviewFragment) fm.findFragmentByTag(ThemePreviewFragment.TAG);
        mDetailsFragment = (ThemeDetailsFragment) fm.findFragmentByTag(ThemeDetailsFragment.TAG);
        mSearchFragment = (ThemeSearchFragment) fm.findFragmentByTag(ThemeSearchFragment.TAG);
    }

    private boolean areThemesAccessible() {
        // themes are only accessible to admin wordpress.com users
        if (WordPress.getCurrentBlog() != null && !WordPress.getCurrentBlog().isDotcomFlag()) {
            Intent intent = new Intent(ThemeBrowserActivity.this, PostsActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
            return false;
        }
        return true;
    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            setupBaseLayout();
        }
    };

    private void setupBaseLayout() {
        if (getFragmentManager().getBackStackEntryCount() == 0) {
            mMenuDrawer.setDrawerIndicatorEnabled(true);
            mViewPager.setVisibility(View.VISIBLE);
            mTabView.setVisibility(View.VISIBLE);
        } else {
            mMenuDrawer.setDrawerIndicatorEnabled(false);
            mViewPager.setVisibility(View.GONE);
            mTabView.setVisibility(View.GONE);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (areThemesAccessible()) {
            mIsRunning = true;

            // fetch themes if we don't have any
            if (NetworkUtils.isNetworkAvailable(this) && WordPress.getCurrentBlog() != null
                    && WordPress.wpDB.getThemeCount(getBlogId()) == 0) {
                fetchThemes(mViewPager.getCurrentItem());
                setRefreshing(true, mViewPager.getCurrentItem());
            }
        }
    }

    @Override
    public void onTabSelected(HorizontalTabView.Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    public class ThemePagerAdapter extends FragmentStatePagerAdapter {
        ThemeTabFragment[] mTabFragment = new ThemeTabFragment[ThemeSortType.values().length];

        public ThemePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public ThemeTabFragment getItem(int i) {
            if (mTabFragment[i] == null) {
                mTabFragment[i] = ThemeTabFragment.newInstance(ThemeSortType.getTheme(i), i);
            }
            return mTabFragment[i];
        }

        @Override
        public int getCount() {
            return ThemeSortType.values().length;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            return ThemeSortType.getTheme(position).getTitle();
        }
    }

    public void fetchThemes(final int page) {
        if (mFetchingThemes) {
            return;
        }
        String siteId = getBlogId();
        mFetchingThemes = true;
        WordPress.getRestClientUtils().getThemes(siteId, 0, 0, new Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        new FetchThemesTask(page).execute(response);
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
                        setRefreshing(false, page);
                    }
                }
        );
    }

    private void fetchCurrentTheme(final int page) {
        final String siteId = getBlogId();

        WordPress.getRestClientUtils().getCurrentTheme(siteId, new Listener() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            Theme theme = Theme.fromJSON(response);
                            if (theme != null) {
                                WordPress.wpDB.setCurrentTheme(siteId, theme.getThemeId());
                                setRefreshing(false, page);
                            }
                        } catch (JSONException e) {
                            AppLog.e(T.THEMES, e);
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError response) {
                    }
                }
        );
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.theme, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            FragmentManager fm = getFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
                setupBaseLayout();
                return true;
            }
        } else if (itemId == R.id.menu_search) {
            FragmentTransaction ft = getFragmentManager().beginTransaction();
            if (mSearchFragment == null) {
                mSearchFragment = ThemeSearchFragment.newInstance();
            }
            ft.add(R.id.theme_browser_container, mSearchFragment, ThemeSearchFragment.TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(null);
            ft.commit();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getFragmentManager();
        if (mMenuDrawer.isMenuVisible()) {
            super.onBackPressed();
        } else if (fm.getBackStackEntryCount() > 0) {
            fm.popBackStack();
            setupBaseLayout();
        } else {
            super.onBackPressed();
        }
    }

    private String getBlogId() {
        if (WordPress.getCurrentBlog() == null)
            return "0";
        return String.valueOf(WordPress.getCurrentBlog().getRemoteBlogId());
    }

    @Override
    public void onThemeSelected(String themeId) {
        FragmentManager fm = getFragmentManager();

        if (!DisplayUtils.isXLarge(ThemeBrowserActivity.this)) {
            // show details as a fragment on top
            FragmentTransaction ft = fm.beginTransaction();

            if (mSearchFragment != null && mSearchFragment.isVisible()) {
                fm.popBackStack();
            }

            setupBaseLayout();
            mDetailsFragment = ThemeDetailsFragment.newInstance(themeId);
            ft.add(R.id.theme_browser_container, mDetailsFragment, ThemeDetailsFragment.TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(null);
            ft.commit();
        } else {
            // show details as a dialog
            mDetailsFragment = ThemeDetailsFragment.newInstance(themeId);
            mDetailsFragment.show(getFragmentManager(), ThemeDetailsFragment.TAG);
            getFragmentManager().executePendingTransactions();
            int minWidth = getResources().getDimensionPixelSize(R.dimen.theme_details_dialog_min_width);
            int height = getResources().getDimensionPixelSize(R.dimen.theme_details_dialog_height);
            int width = Math.max((int) (DisplayUtils.getDisplayPixelWidth(this) * 0.6), minWidth);
            mDetailsFragment.getDialog().getWindow().setLayout(width, height);
        }
    }

    public class FetchThemesTask extends AsyncTask<JSONObject, Void, ArrayList<Theme>> {
        private int mFetchPage;

        public FetchThemesTask(int page) {
            mFetchPage = page;
        }

        @Override
        protected ArrayList<Theme> doInBackground(JSONObject... args) {
            JSONObject response = args[0];
            final ArrayList<Theme> themes = new ArrayList<Theme>();

            if (response != null) {
                JSONArray array = null;
                try {
                    array = response.getJSONArray("themes");

                    if (array != null) {
                        int count = array.length();
                        for (int i = 0; i < count; i++) {
                            JSONObject object = array.getJSONObject(i);
                            Theme theme = Theme.fromJSON(object);
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

            fetchCurrentTheme(mFetchPage);

            if (themes != null && themes.size() > 0) {
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
                    if (result == null) {
                        Toast.makeText(ThemeBrowserActivity.this, R.string.theme_fetch_failed, Toast.LENGTH_SHORT)
                             .show();
                    }
                    setRefreshing(false, mFetchPage);
                }
            });
        }
    }

    @Override
    public void onResume(Fragment fragment) {
        invalidateOptionsMenu();
    }

    @Override
    public void onPause(Fragment fragment) {
        invalidateOptionsMenu();
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsRunning = false;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        if (DisplayUtils.isXLarge(ThemeBrowserActivity.this) && mDetailsFragment != null) {
            mDetailsFragment.dismiss();
        }
        super.onSaveInstanceState(outState);
        outState.putBoolean(KEY_IS_ACTIVATING_THEME, mIsActivatingTheme);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if (savedInstanceState.getBoolean(KEY_IS_ACTIVATING_THEME) && mDetailsFragment!=null)
            mDetailsFragment.setIsActivatingTheme(true);
    }

    @Override
    public void onActivateThemeClicked(String themeId, final Fragment fragment) {
        final String siteId = getBlogId();
        if (themeId == null) {
            themeId = mPreviewFragment.getThemeId();
        }

        final String newThemeId = themeId;
        final WeakReference<ThemeBrowserActivity> ref = new WeakReference<ThemeBrowserActivity>(this);
        mIsActivatingTheme = true;
        final int page = mViewPager.getCurrentItem();
        WordPress.getRestClientUtils().setTheme(siteId, themeId, new Listener() {
                    @Override
                    public void onResponse(JSONObject arg0) {
                        mIsActivatingTheme = false;
                        Toast.makeText(ThemeBrowserActivity.this, R.string.theme_set_success, Toast.LENGTH_LONG).show();

                        WordPress.wpDB.setCurrentTheme(siteId, newThemeId);
                        if (mDetailsFragment != null) {
                            mDetailsFragment.onThemeActivated(true);
                        }
                        setRefreshing(false, page);

                        if (ref.get() != null && mIsRunning && fragment instanceof ThemePreviewFragment) {
                            FragmentManager fm = ref.get().getFragmentManager();

                            if (fm.getBackStackEntryCount() > 0) {
                                fm.popBackStack();
                                setupBaseLayout();
                                invalidateOptionsMenu();
                            }
                        }
                    }
                }, new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        mIsActivatingTheme = false;
                        if (mDetailsFragment != null && mDetailsFragment.isVisible()) mDetailsFragment.onThemeActivated(
                                false);
                        if (ref.get() != null) Toast.makeText(ref.get(), R.string.theme_set_failed, Toast.LENGTH_LONG)
                                                    .show();
                    }
                }
        );

    }

    @Override
    public void onBlogChanged() {
        if (areThemesAccessible()) {
            fetchThemes(mViewPager.getCurrentItem());
            setRefreshing(true, mViewPager.getCurrentItem());
        }
    }

    @Override
    public void onLivePreviewClicked(String themeId, String previewURL) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        if (mPreviewFragment == null) {
            mPreviewFragment = ThemePreviewFragment.newInstance(themeId, previewURL);
        } else {
            mPreviewFragment.load(themeId, previewURL);
        }

        if (mDetailsFragment != null) {
            if (DisplayUtils.isXLarge(ThemeBrowserActivity.this)) {
                mDetailsFragment.dismiss();
            } else {
                ft.hide(mDetailsFragment);
            }
        }
        ft.add(R.id.theme_browser_container, mPreviewFragment, ThemePreviewFragment.TAG);
        ft.addToBackStack(null);
        ft.commit();
        setupBaseLayout();
    }

    private void setRefreshing(boolean refreshing, int page) {
        // We have to nofify all contiguous fragments since the ViewPager cache them
        for (int i = Math.max(page - 1, 0); i <= Math.min(page + 1, mThemePagerAdapter.getCount() - 1); ++i) {
            mThemePagerAdapter.getItem(i).setRefreshing(refreshing);
        }
    }
}
