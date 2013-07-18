
package org.wordpress.android.ui.themes;

import java.util.ArrayList;

import android.content.Context;
import android.content.res.Configuration;
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
import org.wordpress.android.ui.HorizontalTabView;
import org.wordpress.android.ui.HorizontalTabView.TabListener;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.themes.ThemeDetailsFragment.ThemeDetailsFragmentCallback;
import org.wordpress.android.ui.themes.ThemePreviewFragment.ThemePreviewFragmentCallback;
import org.wordpress.android.ui.themes.ThemeTabFragment.ThemeSortType;
import org.wordpress.android.ui.themes.ThemeTabFragment.ThemeTabFragmentCallback;
import org.wordpress.android.util.Utils;

public class ThemeBrowserActivity extends WPActionBarActivity implements
        ThemeTabFragmentCallback, ThemeDetailsFragmentCallback, ThemePreviewFragmentCallback,
        OnQueryTextListener, OnActionExpandListener, TabListener {

    private HorizontalTabView mTabView;
    private ThemeTabFragment[] mTabFragments;
    private ThemePagerAdapter mThemePagerAdapter;
    private ViewPager mViewPager;
    private MenuItem mSearchMenuItem;
    private SearchView mSearchView;
    private ThemeTabFragment mSearchFragment;
    private ThemePreviewFragment mPreviewFragment;
    private ThemeDetailsFragment mDetailsFragment;
    private boolean mFetchingThemes = false;
    
    private MenuItem refreshMenuItem;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
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
        for (int i = 0; i < ThemeSortType.values().length; i++) {
            String title = ThemeSortType.values()[i].getTitle();

            mTabView.addTab(mTabView.newTab().setText(title));
        }
        mTabView.setSelectedTab(0);

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
    public void onTabSelected(HorizontalTabView.Tab tab) {
        mViewPager.setCurrentItem(tab.getPosition());
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

        mFetchingThemes = true;
        startAnimatingRefreshButton();

        WordPress.restClient.getThemes(siteId, 0, 0, new Listener() {

            @Override
            public void onResponse(JSONObject response) {
                new FetchThemesTask().execute(response);
            }
        }, new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError response) {
                Toast.makeText(ThemeBrowserActivity.this, R.string.theme_fetch_failed, Toast.LENGTH_LONG).show();
                Log.d("WordPress", "Failed to download themes: " + response.getMessage());
               
                mFetchingThemes = false;
                stopAnimatingRefreshButton();
               refreshFragments();
               
                
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

        MenuInflater inflater = getSupportMenuInflater();

        if (mDetailsFragment != null && !mDetailsFragment.isInLayout() && mDetailsFragment.isVisible()) {
            inflater.inflate(R.menu.theme_details, menu);
        } else if (mPreviewFragment != null && !mPreviewFragment.isInLayout() && mPreviewFragment.isVisible()) {
            inflater.inflate(R.menu.theme_preview, menu);
        } else {
            inflater.inflate(R.menu.theme, menu);
            refreshMenuItem = menu.findItem(R.id.menu_refresh);

        }
        


        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                popThemeFragment();
                return true;
            }
        } else if (itemId == R.id.menu_search) {
            mSearchMenuItem = item;
            mSearchMenuItem.setOnActionExpandListener(this);

            mSearchView = (SearchView) item.getActionView();
            mSearchView.setOnQueryTextListener(this);

            return true;
        } else if (itemId == R.id.menu_activate) {
            handleMenuActivateTheme(null);
        } else if (itemId == R.id.menu_refresh) {
            
            fetchThemes();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void handleMenuActivateTheme(String themeId) {
        String siteId = getBlogId();
        if (themeId == null) {
            themeId = mPreviewFragment.getThemeId();            
        }
        
        WordPress.restClient.setTheme(siteId, themeId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject arg0) { 
                        Toast.makeText(ThemeBrowserActivity.this, R.string.theme_set_success, Toast.LENGTH_LONG).show();
                        
                        if (Utils.isXLarge(ThemeBrowserActivity.this)) {
                            mDetailsFragment.dismiss();
                        }
                        
                        FragmentManager fm = getSupportFragmentManager();
                        
                        if (fm.getBackStackEntryCount() > 0) {
                            popThemeFragment();
                            invalidateOptionsMenu();
                        }
                    }
                }, 
                new ErrorListener() {
            
                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        Toast.makeText(ThemeBrowserActivity.this, R.string.theme_set_failed, Toast.LENGTH_LONG).show();
                }
        });
        
    }

    @Override
    public void onBackPressed() {
        FragmentManager fm = getSupportFragmentManager();
        if (fm.getBackStackEntryCount() > 0) {
            popThemeFragment();
        } else {
            super.onBackPressed();
        }
    }

    private void popThemeFragment() {
        FragmentManager fm = getSupportFragmentManager();
        try {
            fm.popBackStack();
            if (fm.getBackStackEntryCount() == 1) {
                mViewPager.setVisibility(View.VISIBLE);
                mTabView.setVisibility(View.VISIBLE);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private String getBlogId() {
        return String.valueOf(WordPress.getCurrentBlog().getBlogId());
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
            mFetchingThemes = false;
            stopAnimatingRefreshButton();
            if (result == null) {
                Toast.makeText(ThemeBrowserActivity.this, R.string.theme_fetch_failed, Toast.LENGTH_SHORT).show();
            } 
            refreshFragments();
        }

    }
    
    private void startAnimatingRefreshButton() {
        if (refreshMenuItem != null && mFetchingThemes)
            startAnimatingRefreshButton(refreshMenuItem);
    }
    
    private void stopAnimatingRefreshButton() {
        if (refreshMenuItem != null && !mFetchingThemes)
            stopAnimatingRefreshButton(refreshMenuItem);
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

        if (!Utils.isXLarge(ThemeBrowserActivity.this)) {
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
                mTabView.setVisibility(View.GONE);
            }
            
            mDetailsFragment = ThemeDetailsFragment.newInstance(themeId);
            ft.add(R.id.theme_browser_container, mDetailsFragment);
            ft.addToBackStack(null);
            ft.commit();
            mMenuDrawer.setDrawerIndicatorEnabled(false);
        } else {
            mDetailsFragment = ThemeDetailsFragment.newInstance(themeId);
            mDetailsFragment.show(getSupportFragmentManager(), "ThemeDetails");
        }
    }

    @Override
    public boolean onMenuItemActionExpand(MenuItem item) {
        if (item.getItemId() == R.id.menu_search) {
            if (mSearchFragment == null || !mSearchFragment.isInLayout()) {
                FragmentManager fm = getSupportFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                mViewPager.setVisibility(View.GONE);
                mTabView.setVisibility(View.GONE);
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
                popThemeFragment();
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
    public void onResume(Fragment fragment) {
        invalidateOptionsMenu();
    }

    @Override
    public void onPause(Fragment fragment) {
        invalidateOptionsMenu();
    }
    
    @Override
    public void onActivateThemeClicked(String themeId) {
        handleMenuActivateTheme(themeId);
        
    }
    
    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        fetchThemes();
    };
    
    
    @Override
    public void onLivePreviewClicked(String themeId, String previewURL) {
        if (mPreviewFragment == null || !mPreviewFragment.isInLayout()) {
            FragmentManager fm = getSupportFragmentManager();
            FragmentTransaction ft = fm.beginTransaction();
            mViewPager.setVisibility(View.GONE);
            mTabView.setVisibility(View.GONE);
            mPreviewFragment = ThemePreviewFragment.newInstance(themeId, previewURL);
            
            if (Utils.isXLarge(ThemeBrowserActivity.this)) {
                mDetailsFragment.dismiss();
            } else {
                ft.hide(mDetailsFragment);
            }
            ft.add(R.id.theme_browser_container, mPreviewFragment);
            ft.addToBackStack(null);
            ft.commit();
        }        
    }

}
