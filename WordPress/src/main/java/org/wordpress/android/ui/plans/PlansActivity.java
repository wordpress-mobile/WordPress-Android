package org.wordpress.android.ui.plans;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.annotation.TargetApi;
import android.graphics.Point;
import android.os.Build;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AccelerateInterpolator;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.plans.adapters.PlansPagerAdapter;
import org.wordpress.android.ui.plans.models.Plan;
import org.wordpress.android.ui.reader.ReaderActivityLauncher;
import org.wordpress.android.ui.reader.ReaderActivityLauncher.OpenUrlType;
import org.wordpress.android.util.AniUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.DisplayUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.UrlUtils;
import org.wordpress.android.widgets.WPViewPager;

import java.io.Serializable;
import java.util.List;

import de.greenrobot.event.EventBus;

public class PlansActivity extends AppCompatActivity {

    public static final String ARG_LOCAL_TABLE_BLOG_ID = "ARG_LOCAL_TABLE_BLOG_ID";
    private static final String ARG_LOCAL_AVAILABLE_PLANS = "ARG_LOCAL_AVAILABLE_PLANS";

    private int mLocalBlogID = -1;
    private Plan[] mAvailablePlans;

    private WPViewPager mViewPager;
    private PlansPagerAdapter mPageAdapter;
    private TabLayout mTabLayout;
    private ViewGroup mManageBar;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.plans_activity);

        if (savedInstanceState != null) {
            mLocalBlogID = savedInstanceState.getInt(ARG_LOCAL_TABLE_BLOG_ID);
            Serializable serializable = savedInstanceState.getSerializable(ARG_LOCAL_AVAILABLE_PLANS);
            if (serializable instanceof Plan[]) {
                mAvailablePlans = (Plan[]) serializable;
            }
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
        mManageBar = (ViewGroup) findViewById(R.id.frame_manage);

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

        // Download plans if not already available
        if (mAvailablePlans == null) {
            if (!NetworkUtils.checkConnection(this)) {
                finish();
                return;
            }
            showProgress();
            PlanUpdateService.startService(this, mLocalBlogID);
        } else {
            setupPlansUI();
        }

        // navigate to the "manage plans" page for this blog when the user clicks the manage bar
        mManageBar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Blog blog = WordPress.getBlog(mLocalBlogID);
                if (blog == null) return;
                String domain = UrlUtils.getHost(blog.getUrl());
                String managePlansUrl = "https://wordpress.com/plans/" + domain;
                ReaderActivityLauncher.openUrl(view.getContext(), managePlansUrl, OpenUrlType.EXTERNAL);
            }
        });
    }

    @Override
    protected void onDestroy() {
        PlanUpdateService.stopService(this);
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        EventBus.getDefault().register(this);
    }

    @Override
    public void onPause() {
        super.onPause();
        EventBus.getDefault().unregister(this);
    }

    private void setupPlansUI() {
        if (mAvailablePlans == null || mAvailablePlans.length == 0) {
            // This should never be called with empty plans.
            Toast.makeText(PlansActivity.this, R.string.plans_loading_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        hideProgress();

        mViewPager.setAdapter(getPageAdapter());

        int normalColor = ContextCompat.getColor(this, R.color.blue_light);
        int selectedColor = ContextCompat.getColor(this, R.color.white);
        mTabLayout.setTabTextColors(normalColor, selectedColor);
        mTabLayout.setupWithViewPager(mViewPager);

        // tabMode is set to scrollable in layout, set to fixed if there's enough space to show them all
        mTabLayout.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mTabLayout.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                if (mTabLayout.getChildCount() > 0) {
                    int tabLayoutWidth = 0;
                    LinearLayout tabFirstChild = (LinearLayout) mTabLayout.getChildAt(0);
                    for (int i = 0; i < mTabLayout.getTabCount(); i++) {
                        LinearLayout tabView = (LinearLayout) (tabFirstChild.getChildAt(i));
                        tabLayoutWidth += (tabView.getMeasuredWidth() + tabView.getPaddingLeft() + tabView.getPaddingRight());
                    }

                    int displayWidth = DisplayUtils.getDisplayPixelWidth(PlansActivity.this);
                    if (tabLayoutWidth < displayWidth) {
                        mTabLayout.setTabMode(TabLayout.MODE_FIXED);
                        mTabLayout.setTabGravity(TabLayout.GRAVITY_FILL);
                    }
                }
            }
        });

        if (mViewPager.getVisibility() != View.VISIBLE) {
            // use a circular reveal on API 21+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                revealViewPager();
            } else {
                mViewPager.setVisibility(View.VISIBLE);
                mTabLayout.setVisibility(View.VISIBLE);
                showManageBar();
            }
        }
    }

    private void showManageBar() {
        if (mManageBar.getVisibility() != View.VISIBLE) {
            AniUtils.animateBottomBar(mManageBar, true);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void revealViewPager() {
        mViewPager.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mViewPager.getViewTreeObserver().removeOnGlobalLayoutListener(this);

                Point pt = DisplayUtils.getDisplayPixelSize(PlansActivity.this);
                float startRadius = 0f;
                float endRadius = (float) Math.hypot(pt.x, pt.y);
                int centerX = pt.x / 2;
                int centerY = pt.y / 2;

                Animator anim = ViewAnimationUtils.createCircularReveal(mViewPager, centerX, centerY, startRadius, endRadius);
                anim.setDuration(getResources().getInteger(android.R.integer.config_longAnimTime));
                anim.setInterpolator(new AccelerateInterpolator());

                anim.addListener(new AnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(Animator animation) {
                        super.onAnimationEnd(animation);
                        showManageBar();
                    }
                });

                mViewPager.setVisibility(View.VISIBLE);
                mTabLayout.setVisibility(View.VISIBLE);

                anim.start();
            }
        });
    }

    private void hideProgress() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading_plans);
        progress.setVisibility(View.GONE);
    }

    private void showProgress() {
        final ProgressBar progress = (ProgressBar) findViewById(R.id.progress_loading_plans);
        progress.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        outState.putSerializable(ARG_LOCAL_AVAILABLE_PLANS, mAvailablePlans);
        super.onSaveInstanceState(outState);
    }

    private PlansPagerAdapter getPageAdapter() {
        if (mPageAdapter == null) {
            mPageAdapter = new PlansPagerAdapter(getFragmentManager(), mAvailablePlans);
        }
        return mPageAdapter;
    }

    /*
     * move the ViewPager to the plan for the current blog
     */
    private void selectCurrentPlan() {
        int position = -1;
        for (Plan currentSitePlan : mAvailablePlans) {
            if (currentSitePlan.isCurrentPlan()) {
                position = getPageAdapter().getPositionOfPlan(currentSitePlan.getProductID());
                break;
            }
        }
        if (getPageAdapter().isValidPosition(position)) {
            mViewPager.setCurrentItem(position);
        }
    }

    /*
     * called by the service when plan data is successfully updated
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PlanEvents.PlansUpdated event) {
        // make sure the update is for this blog
        if (event.getLocalBlogId() != this.mLocalBlogID) {
            AppLog.w(AppLog.T.PLANS, "plans updated for different blog");
            return;
        }

        List<Plan> plans = event.getPlans();
        mAvailablePlans = new Plan[plans.size()];
        plans.toArray(mAvailablePlans);

        setupPlansUI();
        selectCurrentPlan();
    }

    /*
     * called by the service when plan data fails to update
     */
    @SuppressWarnings("unused")
    public void onEventMainThread(PlanEvents.PlansUpdateFailed event) {
        Toast.makeText(PlansActivity.this, R.string.plans_loading_error, Toast.LENGTH_LONG).show();
        finish();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

}
