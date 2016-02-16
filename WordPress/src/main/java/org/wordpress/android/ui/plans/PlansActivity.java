package org.wordpress.android.ui.plans;

import android.app.Fragment;
import android.app.FragmentManager;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.plans.models.SitePlan;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.widgets.WPViewPager;

import java.io.Serializable;
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
            Serializable serializable = savedInstanceState.getSerializable(ARG_LOCAL_AVAILABLE_PLANS);
            if (serializable instanceof SitePlan[]) {
                mAvailablePlans = (SitePlan[]) serializable;
            }
            mViewpagerPosSelected = savedInstanceState.getInt(SAVED_VIEWPAGER_POS, NO_PREV_POS_SELECTED_VIEWPAGER);
        } else if (getIntent() != null) {
            mLocalBlogID = getIntent().getIntExtra(ARG_LOCAL_TABLE_BLOG_ID, -1);
        }

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

        mViewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                updatePurchaseUI(position);
            }
        });
    }

    private void updatePurchaseUI(int position) {
        Fragment fragment = getPageAdapter().getItem(position);
        if (!(fragment instanceof PlanFragment)) {
            return;
        }

        SitePlan sitePlan = ((PlanFragment) fragment).getSitePlan();
        Plan planDetails = ((PlanFragment) fragment).getPlanDetails();

        boolean showPurchaseButton;
        if (sitePlan.isCurrentPlan()) {
            showPurchaseButton = false;
        } else {
            long currentPlanId = WordPress.wpDB.getPlanIdForLocalTableBlogId(mLocalBlogID);
            long thisPlanId = sitePlan.getProductID();
            if (currentPlanId == PlansConstants.FREE_PLAN_ID) {
                showPurchaseButton = true;
            } else if (currentPlanId == PlansConstants.PREMIUM_PLAN_ID) {
                showPurchaseButton = (thisPlanId == PlansConstants.FREE_PLAN_ID);
            } else if (currentPlanId == PlansConstants.BUSINESS_PLAN_ID) {
                showPurchaseButton = false;
            } else if (currentPlanId == PlansConstants.JETPACK_FREE_PLAN_ID) {
                showPurchaseButton = true;
            } else if (currentPlanId == PlansConstants.JETPACK_PREMIUM_PLAN_ID) {
                showPurchaseButton = (thisPlanId == PlansConstants.JETPACK_BUSINESS_PLAN_ID);
            } else if (currentPlanId == PlansConstants.JETPACK_BUSINESS_PLAN_ID) {
                showPurchaseButton = false;
            } else {
                showPurchaseButton = true;
            }
        }

        ViewGroup framePurchase = (ViewGroup) findViewById(R.id.frame_purchase);
        if (showPurchaseButton) {
            String purchase = planDetails.getFormattedPrice()
                    + " | <b>"
                    + getString(R.string.plan_purchase_now)
                    + "</b>";
            TextView txtPurchase = (TextView) framePurchase.findViewById(R.id.text_purchase);
            txtPurchase.setText(Html.fromHtml(purchase));
            txtPurchase.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    ToastUtils.showToast(v.getContext(), "Not implemented yet");
                }
            });
        }

        if (showPurchaseButton && framePurchase.getVisibility() != View.VISIBLE) {
            AniUtils.animateBottomBar(framePurchase, true);
        } else if (!showPurchaseButton && framePurchase.getVisibility() == View.VISIBLE) {
            AniUtils.animateBottomBar(framePurchase, false);
        }
    }

    private void setupPlansUI() {
        if (mAvailablePlans == null || mAvailablePlans.length == 0)  {
            // This should never be called with empty plans.
            Toast.makeText(PlansActivity.this, R.string.plans_loading_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        hideProgress();

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

    private void hideProgress() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading_plans);
        progress.setVisibility(View.GONE);
    }

    private void showProgress() {
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
            showProgress();
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
                for (SitePlan plan : mAvailablePlans) {
                    fragments.add(PlanFragment.newInstance(plan));
                }
            }

            FragmentManager fm = getFragmentManager();
            mPageAdapter = new PlansPageAdapter(fm, fragments);
        }
        return mPageAdapter;
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
            if (!isFinishing()) {
                mAvailablePlans = new SitePlan[plans.size()];
                plans.toArray(mAvailablePlans);
                setupPlansUI();
            }
        }
        public void onError() {
            if (!isFinishing()) {
                Toast.makeText(PlansActivity.this, R.string.plans_loading_error, Toast.LENGTH_LONG).show();
                finish();
            }
        }
    };
}
