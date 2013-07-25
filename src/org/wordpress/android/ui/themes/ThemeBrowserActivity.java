
package org.wordpress.android.ui.themes;

import java.lang.ref.WeakReference;
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
import android.widget.ProgressBar;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
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
import org.wordpress.android.ui.themes.ThemeDetailsFragment.ThemeDetailsFragmentCallback;
import org.wordpress.android.ui.themes.ThemePreviewFragment.ThemePreviewFragmentCallback;
import org.wordpress.android.ui.themes.ThemeTabFragment.ThemeSortType;
import org.wordpress.android.ui.themes.ThemeTabFragment.ThemeTabFragmentCallback;
import org.wordpress.android.util.Utils;
import org.wordpress.android.util.WPAlertDialogFragment;

public class ThemeBrowserActivity extends WPActionBarActivity implements
        ThemeTabFragmentCallback, ThemeDetailsFragmentCallback, ThemePreviewFragmentCallback, TabListener {

    private HorizontalTabView mTabView;
    private ThemeTabFragment[] mTabFragments;
    private ThemePagerAdapter mThemePagerAdapter;
    private ViewPager mViewPager;
    private ThemeSearchFragment mSearchFragment;
    private ThemePreviewFragment mPreviewFragment;
    private ThemeDetailsFragment mDetailsFragment;
    private boolean mFetchingThemes = false;
    private boolean mIsRunning;
    private MenuItem refreshMenuItem;
    private ProgressBar mProgressBar;


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

        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
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
        setupBaseLayout();
        mPreviewFragment = (ThemePreviewFragment) fm.findFragmentByTag(ThemePreviewFragment.TAG);
        mDetailsFragment = (ThemeDetailsFragment) fm.findFragmentByTag(ThemeDetailsFragment.TAG);
        mSearchFragment = (ThemeSearchFragment) fm.findFragmentByTag(ThemeSearchFragment.TAG);
        
    }

    private FragmentManager.OnBackStackChangedListener mOnBackStackChangedListener = new FragmentManager.OnBackStackChangedListener() {
        public void onBackStackChanged() {
            setupBaseLayout();
        }
    };

    private void setupBaseLayout() {
        if (getSupportFragmentManager().getBackStackEntryCount() == 0) {
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
        mIsRunning = true;
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
        if (mFetchingThemes)
            return; 
        
        String siteId = getBlogId();

        mFetchingThemes = true;
        mProgressBar.setVisibility(View.VISIBLE);
        startAnimatingRefreshButton();

        WordPress.restClient.getThemes(siteId, 0, 0, new Listener() {

            @Override
            public void onResponse(JSONObject response) {
                new FetchThemesTask().execute(response);
            }
        }, new ErrorListener() {

            @Override
            public void onErrorResponse(VolleyError response) {
                
                if(response.toString().equals(AuthFailureError.class.getName())) {
                    String errorTitle = getString(R.string.theme_auth_error_title);
                    String errorMsg = getString(R.string.theme_auth_error_message);
                    
                    FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
                    WPAlertDialogFragment.newInstance(errorMsg, errorTitle, false).show(ft, "alert");
                    Log.d("WordPress", "Failed to fetch themes: failed authenticate user");
                } else {
                    Toast.makeText(ThemeBrowserActivity.this, R.string.theme_fetch_failed, Toast.LENGTH_LONG).show();
                    Log.d("WordPress", "Failed to fetch themes: " + response.toString());
                }
                
                mFetchingThemes = false;
                mProgressBar.setVisibility(View.GONE);
                stopAnimatingRefreshButton();
                refreshViewPager();
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getSupportMenuInflater();

        inflater.inflate(R.menu.theme, menu);
        refreshMenuItem = menu.findItem(R.id.menu_refresh);
        
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (itemId == android.R.id.home) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStack();
                setupBaseLayout();
                return true;
            }
        } else if (itemId == R.id.menu_refresh) {
            fetchThemes();
            return true;
        } else if (itemId == R.id.menu_search) {
            FragmentTransaction ft = getSupportFragmentManager().beginTransaction();
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
        FragmentManager fm = getSupportFragmentManager();
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
            mProgressBar.setVisibility(View.GONE);
            stopAnimatingRefreshButton();
            if (result == null) {
                Toast.makeText(ThemeBrowserActivity.this, R.string.theme_fetch_failed, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ThemeBrowserActivity.this, R.string.theme_fetch_success, Toast.LENGTH_SHORT).show();
            }
            refreshViewPager();
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

    private void refreshViewPager() {
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
            
            if (mSearchFragment != null && mSearchFragment.isVisible())
                fm.popBackStack();
            
            setupBaseLayout();
            mDetailsFragment = ThemeDetailsFragment.newInstance(themeId);
            ft.add(R.id.theme_browser_container, mDetailsFragment, ThemeDetailsFragment.TAG);
            ft.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_FADE);
            ft.addToBackStack(null);
            ft.commit();
        } else {
            mDetailsFragment = ThemeDetailsFragment.newInstance(themeId);
            mDetailsFragment.show(getSupportFragmentManager(), ThemeDetailsFragment.TAG);
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
        if (Utils.isXLarge(ThemeBrowserActivity.this) && mDetailsFragment != null) {
            mDetailsFragment.dismiss();
        }
        
        super.onSaveInstanceState(outState);
        
    }
    
    @Override
    public void onActivateThemeClicked(String themeId) {
        String siteId = getBlogId();
        if (themeId == null) {
            themeId = mPreviewFragment.getThemeId();            
        }
        
        final WeakReference<ThemeBrowserActivity> ref = new WeakReference<ThemeBrowserActivity>(this);
        WordPress.restClient.setTheme(siteId, themeId, 
                new Listener() {
                    
                    @Override
                    public void onResponse(JSONObject arg0) { 
                        Toast.makeText(ThemeBrowserActivity.this, R.string.theme_set_success, Toast.LENGTH_LONG).show();

                        if (mDetailsFragment != null) {
                            mDetailsFragment.onThemeActivated(true);
                        } 
                        
                        if (ref.get() != null && mIsRunning) {
                            
                            if (Utils.isXLarge(ThemeBrowserActivity.this)) {
                                mDetailsFragment.dismiss();
                            }
                            
                            FragmentManager fm = ref.get().getSupportFragmentManager();
                            if (fm.getBackStackEntryCount() > 0) {
                                fm.popBackStack();
                                setupBaseLayout();
                                invalidateOptionsMenu();
                            }      
                        }
                    }
                }, 
                new ErrorListener() {
            
                    @Override
                    public void onErrorResponse(VolleyError arg0) {
                        
                        if (mDetailsFragment.isVisible())
                            mDetailsFragment.onThemeActivated(false);
                        
                        Toast.makeText(ref.get(), R.string.theme_set_failed, Toast.LENGTH_LONG).show();
                }
        });
        
    }
    
    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        fetchThemes();
    };
    
    
    @Override
    public void onLivePreviewClicked(String themeId, String previewURL) {

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        
        if (mPreviewFragment == null) {
            mPreviewFragment = ThemePreviewFragment.newInstance(themeId, previewURL);
        } else {
            mPreviewFragment.load(themeId, previewURL);
        }

        if (mDetailsFragment != null) {
            if (Utils.isXLarge(ThemeBrowserActivity.this)) {
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

}
