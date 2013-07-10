
package org.wordpress.android.ui.themes;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.MenuItem.OnActionExpandListener;
import com.actionbarsherlock.widget.SearchView;
import com.actionbarsherlock.widget.SearchView.OnQueryTextListener;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest.ErrorListener;
import com.wordpress.rest.RestRequest.Listener;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Theme;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.themes.ThemeDetailsFragment.ThemeDetailsFragmentCallback;
import org.wordpress.android.ui.themes.ThemeTabFragment.ThemeSortType;
import org.wordpress.android.ui.themes.ThemeTabFragment.ThemeTabFragmentCallback;

public class ThemeBrowserActivity extends WPActionBarActivity implements ActionBar.TabListener,
        ThemeTabFragmentCallback, ThemeDetailsFragmentCallback, OnQueryTextListener,
        OnActionExpandListener {

    private ThemeTabFragment[] mTabFragments;
    private ThemePagerAdapter mThemePagerAdapter;
    private ViewPager mViewPager;
    private ThemeDetailsFragment mDetailsFragment;
    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;
    private ThemeTabFragment mSearchFragment;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            Log.e("WordPress", "ThemeActivity DB is null - finishing ThemeActivity");
            finish();
            return;
        }

        setTitle(R.string.themes);

        createMenuDrawer(R.layout.theme_browser_activity);

        mThemePagerAdapter = new ThemePagerAdapter(getSupportFragmentManager());
        mTabFragments = new ThemeTabFragment[mThemePagerAdapter.getCount()];

        final ActionBar actionBar = getSupportActionBar();
        actionBar.setDisplayShowTitleEnabled(true);
        actionBar.setHomeButtonEnabled(true);
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        mViewPager = (ViewPager) findViewById(R.id.theme_browser_pager);
        mViewPager.setAdapter(mThemePagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                actionBar.setSelectedNavigationItem(position);
            }
        });
        for (int i = 0; i < mThemePagerAdapter.getCount(); i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(mThemePagerAdapter.getPageTitle(i))
                            .setTabListener(this)
                            .setTag(i));
        }

        FragmentManager fm = getSupportFragmentManager();
        fm.addOnBackStackChangedListener(mOnBackStackChangedListener);

    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
                mMenuDrawer.setDrawerIndicatorEnabled(true);
            } else {
                mMenuDrawer.setDrawerIndicatorEnabled(false);
            }
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if (WordPress.getCurrentBlog() != null && WordPress.wpDB.getThemeCount(getBlogId()) == 0)
            fetchThemes();
    };

    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) {
    }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) {
    }

    public class ThemePagerAdapter extends FragmentPagerAdapter {

        public ThemePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            mTabFragments[i] = ThemeTabFragment.newInstance(ThemeSortType.getTheme(i));
            return mTabFragments[i];
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

    private void fetchThemes() {
        String siteId = getBlogId();

        WordPress.restClient.getThemes(siteId, 0, 0, new Listener() {

            @Override
            public void onResponse(JSONObject response) {
                new FetchThemesTask().execute(response);
            }
        }, new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError response) {
                Log.d("WordPress", "Failed to download themes: " + response.getMessage());
            }
        });
    }

    private String getBlogId() {
        return String.valueOf(WordPress.getCurrentBlog().getBlogId());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getSupportMenuInflater();

        if (mDetailsFragment != null && !mDetailsFragment.isInLayout()
                && mDetailsFragment.isVisible()) {
            inflater.inflate(R.menu.theme_details, menu);
        } else {
            inflater.inflate(R.menu.theme, menu);

        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popThemeDetailsFragment();
                return true;
            }
        } else if (itemId == R.id.menu_search) {
            mSearchMenuItem = item;
            mSearchMenuItem.setOnActionExpandListener(this);

            mSearchView = (SearchView) item.getActionView();
            mSearchView.setOnQueryTextListener(this);

            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            popThemeDetailsFragment();
        } else {
            super.onBackPressed();
        }
    }

    private void popThemeDetailsFragment() {
        FragmentManager fm = getSupportFragmentManager();
        try {
            fm.popBackStack();

            mViewPager.setVisibility(View.VISIBLE);
            ActionBar actionBar = getSupportActionBar();
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public class FetchThemesTask extends AsyncTask<JSONObject, Void, ArrayList<Theme>> {

        @Override
        protected ArrayList<Theme> doInBackground(JSONObject... args) {
            JSONObject response = args[0];

            final ArrayList<Theme> themes = new ArrayList<Theme>();

            if (response != null) {
                JSONArray array = null;
                try {
                    array = response.getJSONArray("themes");

                    if (array != null) {

                        for (int i = 0; i < array.length(); i++) {
                            JSONObject object = array.getJSONObject(i);
                            Theme theme = Theme.fromJSON(object);
                            theme.save();
                            themes.add(theme);
                        }
                    }
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            if (themes != null && themes.size() > 0) {
                return themes;
            }
            return null;
        }

        @Override
        protected void onPostExecute(ArrayList<Theme> result) {
            if (result == null) {
                Toast.makeText(ThemeBrowserActivity.this, "Failed to fetch themes",
                        Toast.LENGTH_SHORT).show();
                refreshFragments();
            }
        }

    }

    private void refreshFragments() {
        for (int i = 0; i < mTabFragments.length; i++) {
            ThemeTabFragment fragment = mTabFragments[i];
            if (fragment != null)
                fragment.refresh();
        }
    }

    @Override
    public void onThemeSelected(String themeId) {
        FragmentManager fm = getSupportFragmentManager();

        if (mDetailsFragment == null || !mDetailsFragment.isInLayout()) {
            FragmentTransaction ft = fm.beginTransaction();
            
            // determine if we are in regular view or search view
            if (fm.getBackStackEntryCount() > 0) {
                if (mSearchFragment != null && mSearchFragment.isVisible()) {
                    ft.hide(mSearchFragment);
                    ft.remove(mSearchFragment);
                    fm.popBackStack();
                }
            } else {
                mViewPager.setVisibility(View.GONE);
                ActionBar actionBar = getSupportActionBar();
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);    
            }
            
            mDetailsFragment = ThemeDetailsFragment.newInstance(themeId);
            ft.add(R.id.theme_browser_container, mDetailsFragment);
            ft.addToBackStack(null);
            ft.commit();
            mMenuDrawer.setDrawerIndicatorEnabled(false);
        } else {
            mDetailsFragment.loadTheme(themeId);
        }
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        if (item.getItemId() == R.id.menu_search) {
            if (mSearchFragment == null || !mSearchFragment.isInLayout()) {
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                mViewPager.setVisibility(View.GONE);
                ActionBar actionBar = getSupportActionBar();
                actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);

                mSearchFragment = ThemeTabFragment.newInstance(ThemeSortType.getTheme(0));
                ft.add(R.id.theme_browser_container, mSearchFragment);
                ft.addToBackStack(null);
                ft.commit();
            }

            return true;
        }
        return false;
    }

    @Override
    public boolean onMenuItemActionCollapse(MenuItem item) {
        if (item.getItemId() == R.id.menu_search) {
            mSearchFragment = null;
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popThemeDetailsFragment();
            }
        }
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {

        mSearchFragment.search(query);
        mSearchView.clearFocus();
        return true;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mSearchFragment.search(newText);
        return true;
    }

    @Override
    public void onResumeThemeDetailsFragment() {
        invalidateOptionsMenu();
    }

    @Override
    public void onPauseThemeDetailsFragment() {
        invalidateOptionsMenu();

    }
}
