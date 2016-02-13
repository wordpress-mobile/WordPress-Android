package org.wordpress.android.ui.plans;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.plans.models.SitePlan;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.widgets.WPViewPager;

import java.util.ArrayList;
import java.util.List;

public class PlansActivity extends AppCompatActivity {

    public static final String ARG_LOCAL_TABLE_BLOG_ID = "ARG_LOCAL_TABLE_BLOG_ID";
    private static final String ARG_LOCAL_AVAILABLE_PLANS = "ARG_LOCAL_AVAILABLE_PLANS";
    private static final String SAVED_VIEWPAGER_POS = "SAVED_VIEWPAGER_POS";

    private static final int NO_PREV_POS_SELECTED_VIEWPAGER = -1;

    private int mLocalBlogID = -1;
    private SitePlan[] mAvailablePlans;
    private int mViewpagerPosSelected = NO_PREV_POS_SELECTED_VIEWPAGER;

    private WPViewPager mViewPager;
    private PlansPageAdapter mPageAdapter;
    private TabLayout mTabLayout;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.plans_activity);

        if (savedInstanceState != null) {
            mLocalBlogID = savedInstanceState.getInt(ARG_LOCAL_TABLE_BLOG_ID);
            if (savedInstanceState.getSerializable(ARG_LOCAL_AVAILABLE_PLANS) instanceof SitePlan[]) {
                mAvailablePlans = (SitePlan[]) savedInstanceState.getSerializable(ARG_LOCAL_AVAILABLE_PLANS);
            }
            mViewpagerPosSelected = savedInstanceState.getInt(SAVED_VIEWPAGER_POS, NO_PREV_POS_SELECTED_VIEWPAGER);
        } else if (getIntent() != null) {
            mLocalBlogID = getIntent().getIntExtra(ARG_LOCAL_TABLE_BLOG_ID, -1);
        }

        //Make sure the blog_id passed to this activity is valid and the blog is available within the app
        if (WordPress.getBlog(mLocalBlogID) == null) {
            AppLog.e(AppLog.T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
            Toast.makeText(this, R.string.plans_loading_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mViewPager = (WPViewPager) findViewById(R.id.viewpager);
        mTabLayout = (TabLayout) findViewById(R.id.tab_layout);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Shadow removed on Activities with a tab toolbar
            actionBar.setTitle(getString(R.string.plans));
            actionBar.setElevation(0.0f);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    private void setupPlansUI() {
        if (mAvailablePlans == null || mAvailablePlans.length == 0)  {
            // This should never be called with empty plans.
            Toast.makeText(PlansActivity.this, R.string.plans_loading_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading_plans);
        progress.setVisibility(View.GONE);

        mViewPager.setVisibility(View.VISIBLE);
        mViewPager.setOffscreenPageLimit(mAvailablePlans.length - 1);
        mViewPager.setAdapter(getPageAdapter());

        mTabLayout.setVisibility(View.VISIBLE);
        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
        int normalColor = getResources().getColor(R.color.blue_light);
        int selectedColor = getResources().getColor(R.color.white);
        mTabLayout.setTabTextColors(normalColor, selectedColor);
        mTabLayout.setupWithViewPager(mViewPager);

        // Move the viewpager on the blog plan if no prev position is available
        if (mViewpagerPosSelected == NO_PREV_POS_SELECTED_VIEWPAGER) {
            for (SitePlan currentSitePlan : mAvailablePlans) {
                if (currentSitePlan.isCurrentPlan()) {
                    mViewpagerPosSelected = getPageAdapter().getPositionOfPlan(currentSitePlan.getProductID());
                }
            }
        }
        if (getPageAdapter().isValidPosition(mViewpagerPosSelected)) {
            mViewPager.setCurrentItem(mViewpagerPosSelected);
        }
    }

    private void setupLoadingUI() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading_plans);
        progress.setVisibility(View.VISIBLE);
    }

    private class PlansPageAdapter extends FragmentPagerAdapter {
        private final List<Fragment> mFragments;

        PlansPageAdapter(FragmentManager fm, List<Fragment> fragments) {
            super(fm);
            mFragments = fragments;
        }

        @Override
        public CharSequence getPageTitle(int position) {
            if (mFragments != null && isValidPosition(position)) {
                return ((PlanFragment)mFragments.get(position)).getTitle();
            }
            return super.getPageTitle(position);
        }

        @Override
        public Fragment getItem(int position) {
            return mFragments.get(position);
        }

        @Override
        public int getCount() {
            return mFragments.size();
        }

        public boolean isValidPosition(int position) {
            return (position >= 0 && position < getCount());
        }

        public int getPositionOfPlan(long planID) {
            for (int i = 0; i < getCount(); i++) {
                PlanFragment fragment = (PlanFragment) getItem(i);
                if (fragment.getSitePlan().getProductID() == planID) {
                    return  i;
                }
            }
            return -1;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        // Download plans if not already available
        if (mAvailablePlans == null) {
            if (!NetworkUtils.checkConnection(this)) {
                finish();
                return;
            }
            boolean enqueued = PlansUtils.downloadAvailablePlansForSite(mLocalBlogID, mPlansDownloadListener);
            if (!enqueued) {
                Toast.makeText(PlansActivity.this, R.string.plans_loading_error, Toast.LENGTH_LONG).show();
                finish();
            }
            setupLoadingUI();
        } else {
            setupPlansUI();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        outState.putSerializable(ARG_LOCAL_AVAILABLE_PLANS, mAvailablePlans);
        if (mViewPager != null) {
            outState.putInt(SAVED_VIEWPAGER_POS, mViewPager.getCurrentItem());
            // trick to restore the correct pos of the view pager without using a listener when the activity is not restarted.
            mViewpagerPosSelected = mViewPager.getCurrentItem();
        }
    }

    private PlansPageAdapter getPageAdapter() {
        if (mPageAdapter == null) {
            List<Fragment> fragments = new ArrayList<>();
            if (mAvailablePlans != null) {
                for(SitePlan current : mAvailablePlans) {
                    PlanFragment fg = PlanFragment.newInstance();
                    fg.setSitePlan(current);
                    fragments.add(fg);
                }
            }

            FragmentManager fm = getFragmentManager();
            mPageAdapter = new PlansPageAdapter(fm, fragments);
        }
        return mPageAdapter;
    }

    private boolean hasPageAdapter() {
        return mPageAdapter != null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    private final PlansUtils.AvailablePlansListener mPlansDownloadListener = new PlansUtils.AvailablePlansListener() {
        public void onResponse(List<SitePlan> plans) {
            mAvailablePlans = new SitePlan[plans.size()];
            plans.toArray(mAvailablePlans);
            setupPlansUI();
        }
        public void onError(Exception error) {
            AppLog.e(AppLog.T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
            Toast.makeText(PlansActivity.this, R.string.plans_loading_error, Toast.LENGTH_LONG).show();
            finish();
        }
    };
}
