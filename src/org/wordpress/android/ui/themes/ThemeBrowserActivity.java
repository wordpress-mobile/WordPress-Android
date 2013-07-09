
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
import android.view.ViewGroup;
import android.widget.Toast;

import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.app.ActionBar.Tab;
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
import org.wordpress.android.ui.themes.ThemeTabFragment.ThemeSortType;

public class ThemeBrowserActivity extends WPActionBarActivity implements ActionBar.TabListener {

    private ThemeTabFragment[] mFragments;
    private ThemePagerAdapter mThemePagerAdapter;
    private ViewPager mViewPager;

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
        mFragments = new ThemeTabFragment[mThemePagerAdapter.getCount()];

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
            if (getSupportFragmentManager().getBackStackEntryCount() == 0)
                mMenuDrawer.setDrawerIndicatorEnabled(true);
        }
    };

    @Override
    protected void onResume() {
        super.onResume();

        if(WordPress.getCurrentBlog() != null && WordPress.wpDB.getThemeCount(getBlogId()) == 0)
            fetchThemes();
    };
    
    @Override
    public void onTabSelected(Tab tab, FragmentTransaction ft) {
        mViewPager.setCurrentItem(tab.getPosition());
    }

    @Override
    public void onTabUnselected(Tab tab, FragmentTransaction ft) { }

    @Override
    public void onTabReselected(Tab tab, FragmentTransaction ft) { }

    public class ThemePagerAdapter extends FragmentPagerAdapter {

        public ThemePagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            mFragments[i] = ThemeTabFragment.newInstance(ThemeSortType.getTheme(i)); 
            return mFragments[i];
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
                Toast.makeText(ThemeBrowserActivity.this, "Failed to fetch themes", Toast.LENGTH_SHORT).show();
                refreshFragments();
            }
        }
       
    }

    private void refreshFragments() {
        for (int i = 0; i < mFragments.length; i++) {
            ThemeTabFragment fragment = mFragments[i];
            if (fragment != null)
                fragment.refresh();
        }
    }
}
