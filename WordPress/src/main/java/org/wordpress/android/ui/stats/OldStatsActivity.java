package org.wordpress.android.ui.stats;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;

import org.apache.commons.lang3.StringUtils;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.fluxc.Dispatcher;
import org.wordpress.android.fluxc.generated.SiteActionBuilder;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.AccountStore;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.fluxc.store.SiteStore.OnSiteChanged;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.RequestCodes;
import org.wordpress.android.ui.Shortcut;
import org.wordpress.android.ui.posts.PromoDialog;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.analytics.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.DeviceUtils;
import org.wordpress.android.util.LocaleManager;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.RateLimitedTask;
import org.wordpress.android.util.ShortcutUtils;
import org.wordpress.android.util.SiteUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

import static org.wordpress.android.ui.stats.service.StatsService.TASK_ID_GROUP_ALL;
import static org.wordpress.android.util.WPSwipeToRefreshHelper.buildSwipeToRefreshHelper;

/**
 * The native stats activity
 * <p>
 * By pressing a spinner on the action bar, the user can select which timeframe they wish to see.
 * </p>
 */
public class OldStatsActivity extends AppCompatActivity
        implements NestedScrollViewExt.ScrollViewListener,
        StatsVisitorsAndViewsFragment.OnDateChangeListener,
        StatsVisitorsAndViewsFragment.OnOverviewItemChangeListener,
        StatsInsightsTodayFragment.OnInsightsTodayClickListener {
    private static final String SAVED_WP_LOGIN_STATE = "SAVED_WP_LOGIN_STATE";
    private static final String SAVED_STATS_TIMEFRAME = "SAVED_STATS_TIMEFRAME";
    private static final String SAVED_STATS_REQUESTED_DATE = "SAVED_STATS_REQUESTED_DATE";
    private static final String SAVED_STATS_SCROLL_POSITION = "SAVED_STATS_SCROLL_POSITION";
    private static final String SAVED_THERE_WAS_AN_ERROR_LOADING_STATS = "SAVED_THERE_WAS_AN_ERROR_LOADING_STATS";
    private static final String TAG_PROMO_DIALOG = "promote_widget_dialog";

    private Spinner mSpinner;
    private NestedScrollViewExt mOuterScrollView;

    public static final String ARG_LOCAL_TABLE_SITE_ID = "ARG_LOCAL_TABLE_SITE_ID";
    public static final String ARG_LAUNCHED_FROM = "ARG_LAUNCHED_FROM";
    public static final String ARG_DESIRED_TIMEFRAME = "ARG_DESIRED_TIMEFRAME";
    public static final String LOGGED_INTO_JETPACK = "LOGGED_INTO_JETPACK";
    private static final int TOTAL_FRAGMENT_QUANTITY = 8;
    private int mCallsToWaitFor = TOTAL_FRAGMENT_QUANTITY;

    public enum StatsLaunchedFrom {
        STATS_WIDGET,
        NOTIFICATIONS
    }

    @Inject AccountStore mAccountStore;
    @Inject SiteStore mSiteStore;
    @Inject Dispatcher mDispatcher;
    @Inject ShortcutUtils mShortcutUtils;

    private int mResultCode = -1;
    private SiteModel mSite;
    private boolean mIsInFront;
    private StatsTimeframe mCurrentTimeframe = StatsTimeframe.INSIGHTS;
    private String mRequestedDate;
    private boolean mIsUpdatingStats;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private TimeframeSpinnerAdapter mTimeframeSpinnerAdapter;
    private final StatsTimeframe[] mTimeframes = {StatsTimeframe.INSIGHTS, StatsTimeframe.DAY, StatsTimeframe.WEEK,
            StatsTimeframe.MONTH, StatsTimeframe.YEAR};
    private StatsVisitorsAndViewsFragment.OverviewLabel mTabToSelectOnGraph =
            StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS;
    private boolean mThereWasAnErrorLoadingStats = false;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleManager.setLocale(newBase));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((WordPress) getApplication()).component().inject(this);

        if (WordPress.wpDB == null) {
            ToastUtils.showToast(this, R.string.fatal_db_error, ToastUtils.Duration.LONG);
            finish();
            return;
        }

        if (savedInstanceState == null) {
            mSite = (SiteModel) getIntent().getSerializableExtra(WordPress.SITE);
        } else {
            mSite = (SiteModel) savedInstanceState.getSerializable(WordPress.SITE);
        }

        if (mSite == null) {
            ToastUtils.showToast(this, R.string.blog_not_found, ToastUtils.Duration.SHORT);
            finish();
            return;
        }

        setContentView(R.layout.stats_activity);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
            actionBar.setTitle(R.string.stats);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mSwipeToRefreshHelper = buildSwipeToRefreshHelper(
                (CustomSwipeRefreshLayout) findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            return;
                        }

                        refreshStatsFromCurrentDate();
                    }
                }
        );

        setTitle(R.string.stats);

        mOuterScrollView = findViewById(R.id.scroll_view_stats);
        mOuterScrollView.setScrollViewListener(this);

        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(SAVED_WP_LOGIN_STATE);
            mCurrentTimeframe = (StatsTimeframe) savedInstanceState.getSerializable(SAVED_STATS_TIMEFRAME);
            mRequestedDate = savedInstanceState.getString(SAVED_STATS_REQUESTED_DATE);
            mThereWasAnErrorLoadingStats = savedInstanceState.getBoolean(SAVED_THERE_WAS_AN_ERROR_LOADING_STATS);
            final int yScrollPosition = savedInstanceState.getInt(SAVED_STATS_SCROLL_POSITION);
            if (yScrollPosition != 0) {
                mOuterScrollView.postDelayed(new Runnable() {
                    public void run() {
                        if (!isFinishing()) {
                            mOuterScrollView.scrollTo(0, yScrollPosition);
                        }
                    }
                }, StatsConstants.STATS_SCROLL_TO_DELAY);
            }
        } else if (getIntent() != null) {
            if (getIntent().hasExtra(SAVED_STATS_TIMEFRAME)) {
                mCurrentTimeframe = (StatsTimeframe) getIntent().getSerializableExtra(SAVED_STATS_TIMEFRAME);
            } else if (getIntent().hasExtra(ARG_DESIRED_TIMEFRAME)) {
                mCurrentTimeframe = (StatsTimeframe) getIntent().getSerializableExtra(ARG_DESIRED_TIMEFRAME);
            } else {
                // Read the value from app preferences here. Default to 0 - Insights
                mCurrentTimeframe = AppPrefs.getStatsTimeframe();
            }
            mRequestedDate = StatsUtils.getCurrentDateTZ(mSite);

            if (getIntent().hasExtra(ARG_LAUNCHED_FROM)) {
                StatsLaunchedFrom from = (StatsLaunchedFrom) getIntent().getSerializableExtra(ARG_LAUNCHED_FROM);
                if (from == StatsLaunchedFrom.STATS_WIDGET) {
                    AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.STATS_WIDGET_TAPPED, mSite);
                }
            }
        }

        checkIfSiteHasAccessibleStats(mSite);

        // create the fragments without forcing the re-creation. If the activity is restarted fragments can already
        // be there, and ready to be displayed without making any network connections. A fragment calls the
        // stats service if its internal datamodel is empty.
        createFragments(false);

        if (mSpinner == null) {
            mSpinner = findViewById(R.id.filter_spinner);

            mTimeframeSpinnerAdapter = new TimeframeSpinnerAdapter(this, mTimeframes);

            mSpinner.setAdapter(mTimeframeSpinnerAdapter);
            mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isFinishing()) {
                        return;
                    }
                    final StatsTimeframe selectedTimeframe =
                            (StatsTimeframe) mTimeframeSpinnerAdapter.getItem(position);

                    if (mCurrentTimeframe == selectedTimeframe) {
                        AppLog.d(T.STATS, "The selected TIME FRAME is already active: " + selectedTimeframe.getLabel());
                        return;
                    }

                    AppLog.d(T.STATS, "NEW TIME FRAME : " + selectedTimeframe.getLabel());
                    mCurrentTimeframe = selectedTimeframe;
                    AppPrefs.setStatsTimeframe(mCurrentTimeframe);
                    mRequestedDate = StatsUtils.getCurrentDateTZ(mSite);
                    createFragments(true); // Need to recreate fragment here, since a new timeline was selected.
                    mSpinner.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!isFinishing()) {
                                scrollToTop();
                            }
                        }
                    }, StatsConstants.STATS_SCROLL_TO_DELAY);

                    trackStatsAnalytics();
                }

                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // nop
                }
            });

            Toolbar spinnerToolbar = findViewById(R.id.toolbar_filter);
            spinnerToolbar.setBackgroundColor(getResources().getColor(R.color.blue_medium));
        }

        selectCurrentTimeframeInActionBar();

        TextView otherRecentStatsMovedLabel = findViewById(R.id.stats_other_recent_stats_moved);
        otherRecentStatsMovedLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < mTimeframes.length; i++) {
                    if (mTimeframes[i] == StatsTimeframe.INSIGHTS) {
                        mSpinner.setSelection(i);
                        break;
                    }
                }

                mSpinner.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        if (!isFinishing()) {
                            scrollToTop();
                        }
                    }
                }, StatsConstants.STATS_SCROLL_TO_DELAY);
            }
        });

        // Track usage here
        if (savedInstanceState == null) {
            AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.STATS_ACCESSED, mSite);
            trackStatsAnalytics();
            mShortcutUtils.reportShortcutUsed(Shortcut.OPEN_STATS);
        }
    }

    private boolean checkIfSiteHasAccessibleStats(SiteModel site) {
        if (!SiteUtils.isAccessedViaWPComRest(mSite) && !getIntent().getBooleanExtra(LOGGED_INTO_JETPACK, false)) {
            if (!site.isJetpackInstalled() || !site.isJetpackConnected()) {
                ActivityLauncher.viewConnectJetpackForStats(this, site);
                return false;
            }
        }
        return true;
    }

    private void trackStatsAnalytics() {
        // Track usage here
        switch (mCurrentTimeframe) {
            case INSIGHTS:
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.STATS_INSIGHTS_ACCESSED, mSite);
                break;
            case DAY:
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.STATS_PERIOD_DAYS_ACCESSED, mSite);
                break;
            case WEEK:
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.STATS_PERIOD_WEEKS_ACCESSED, mSite);
                break;
            case MONTH:
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.STATS_PERIOD_MONTHS_ACCESSED, mSite);
                break;
            case YEAR:
                AnalyticsUtils.trackWithSiteDetails(AnalyticsTracker.Stat.STATS_PERIOD_YEARS_ACCESSED, mSite);
                break;
        }
    }

    @Override
    protected void onStop() {
        mDispatcher.unregister(this);
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
        mDispatcher.register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsInFront = true;
        if (!NetworkUtils.checkConnection(this)) {
            mSwipeToRefreshHelper.setRefreshing(false);
        }
        ActivityId.trackLastActivity(ActivityId.STATS);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsInFront = false;
        mIsUpdatingStats = false;
        mSwipeToRefreshHelper.setRefreshing(false);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_WP_LOGIN_STATE, mResultCode);
        outState.putSerializable(WordPress.SITE, mSite);
        outState.putSerializable(SAVED_STATS_TIMEFRAME, mCurrentTimeframe);
        outState.putString(SAVED_STATS_REQUESTED_DATE, mRequestedDate);
        outState.putBoolean(SAVED_THERE_WAS_AN_ERROR_LOADING_STATS, mThereWasAnErrorLoadingStats);
        if (mOuterScrollView.getScrollY() != 0) {
            outState.putInt(SAVED_STATS_SCROLL_POSITION, mOuterScrollView.getScrollY());
        }
        super.onSaveInstanceState(outState);
    }

    private void createFragments(boolean forceRecreationOfFragments) {
        if (isFinishing()) {
            return;
        }

        // Make the labels invisible see: https://github.com/wordpress-mobile/WordPress-Android/issues/3279
        findViewById(R.id.stats_other_recent_stats_label_insights).setVisibility(View.INVISIBLE);
        findViewById(R.id.stats_other_recent_stats_label_timeline).setVisibility(View.INVISIBLE);
        findViewById(R.id.stats_other_recent_stats_moved).setVisibility(View.INVISIBLE);

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        StatsAbstractFragment fragment;

        if (mCurrentTimeframe != StatsTimeframe.INSIGHTS) {
            findViewById(R.id.stats_timeline_fragments_container).setVisibility(View.VISIBLE);
            findViewById(R.id.stats_insights_fragments_container).setVisibility(View.GONE);

            if (fm.findFragmentByTag(StatsVisitorsAndViewsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newVisitorsAndViewsInstance(StatsViewType.GRAPH_AND_SUMMARY,
                                                                             mSite.getId(), mCurrentTimeframe,
                                                                             mRequestedDate, mTabToSelectOnGraph);
                ft.replace(R.id.stats_visitors_and_views_container, fragment, StatsVisitorsAndViewsFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsTopPostsAndPagesFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.TOP_POSTS_AND_PAGES, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_top_posts_container, fragment, StatsTopPostsAndPagesFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsReferrersFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.REFERRERS, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_referrers_container, fragment, StatsReferrersFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsClicksFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.CLICKS, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_clicks_container, fragment, StatsClicksFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsGeoviewsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.GEOVIEWS, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_geoviews_container, fragment, StatsGeoviewsFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsAuthorsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.AUTHORS, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_top_authors_container, fragment, StatsAuthorsFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsVideoplaysFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.VIDEO_PLAYS, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_video_container, fragment, StatsVideoplaysFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsSearchTermsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.SEARCH_TERMS, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_search_terms_container, fragment, StatsSearchTermsFragment.TAG);
            }
        } else {
            findViewById(R.id.stats_timeline_fragments_container).setVisibility(View.GONE);
            findViewById(R.id.stats_insights_fragments_container).setVisibility(View.VISIBLE);

            if (fm.findFragmentByTag(StatsInsightsMostPopularFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.INSIGHTS_MOST_POPULAR, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_insights_most_popular_container, fragment, StatsInsightsMostPopularFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsInsightsAllTimeFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.INSIGHTS_ALL_TIME, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_insights_all_time_container, fragment, StatsInsightsAllTimeFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsInsightsTodayFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.INSIGHTS_TODAY, mSite.getId(),
                                                             StatsTimeframe.DAY, mRequestedDate);
                ft.replace(R.id.stats_insights_today_container, fragment, StatsInsightsTodayFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsInsightsLatestPostSummaryFragment.TAG) == null
                || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.INSIGHTS_LATEST_POST_SUMMARY, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_insights_latest_post_summary_container, fragment,
                           StatsInsightsLatestPostSummaryFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsCommentsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.COMMENTS, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_comments_container, fragment, StatsCommentsFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsTagsAndCategoriesFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.TAGS_AND_CATEGORIES, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_tags_and_categories_container, fragment, StatsTagsAndCategoriesFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsPublicizeFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.PUBLICIZE, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_publicize_container, fragment, StatsPublicizeFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsFollowersFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.FOLLOWERS, mSite.getId(),
                                                             mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_followers_container, fragment, StatsFollowersFragment.TAG);
            }
        }

        ft.commitAllowingStateLoss();

        // Slightly delayed labels setup: see https://github.com/wordpress-mobile/WordPress-Android/issues/3279
        mOuterScrollView.postDelayed(new Runnable() {
            @Override
            public void run() {
                if (isFinishing()) {
                    return;
                }
                boolean isInsights = mCurrentTimeframe == StatsTimeframe.INSIGHTS;
                findViewById(R.id.stats_other_recent_stats_label_insights)
                        .setVisibility(isInsights ? View.VISIBLE : View.GONE);
                findViewById(R.id.stats_other_recent_stats_label_timeline)
                        .setVisibility(isInsights ? View.GONE : View.VISIBLE);
                findViewById(R.id.stats_other_recent_stats_moved).setVisibility(isInsights ? View.GONE : View.VISIBLE);
            }
        }, StatsConstants.STATS_LABELS_SETUP_DELAY);
    }

    private void updateTimeframeAndDateAndStartRefreshOfFragments(boolean includeGraph) {
        if (isFinishing()) {
            return;
        }
        FragmentManager fm = getSupportFragmentManager();

        if (mCurrentTimeframe != StatsTimeframe.INSIGHTS) {
            mCallsToWaitFor = TOTAL_FRAGMENT_QUANTITY;
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsTopPostsAndPagesFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsReferrersFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsClicksFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsGeoviewsFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsAuthorsFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsVideoplaysFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsSearchTermsFragment.TAG);
            if (includeGraph) {
                updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsVisitorsAndViewsFragment.TAG);
            } else {
                mCallsToWaitFor--;
            }
        } else {
            mCallsToWaitFor = TOTAL_FRAGMENT_QUANTITY;
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsInsightsTodayFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsInsightsAllTimeFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsInsightsMostPopularFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsInsightsLatestPostSummaryFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsCommentsFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsTagsAndCategoriesFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsPublicizeFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsFollowersFragment.TAG);
        }
    }

    private boolean updateTimeframeAndDateAndStartRefreshInFragment(FragmentManager fm, String fragmentTAG) {
        StatsAbstractFragment fragment = (StatsAbstractFragment) fm.findFragmentByTag(fragmentTAG);
        if (fragment != null) {
            fragment.setDate(mRequestedDate);
            fragment.setTimeframe(mCurrentTimeframe);
            fragment.refreshStats();
            return true;
        }
        return false;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCodes.REQUEST_JETPACK) {
            // Refresh the site in case we're back from Jetpack install Webview
            mDispatcher.dispatch(SiteActionBuilder.newFetchSiteAction(mSite));
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int i = item.getItemId();
        if (i == android.R.id.home) {
            finish();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void scrollToTop() {
        mOuterScrollView.fullScroll(ScrollView.FOCUS_UP);
    }

    // StatsInsightsTodayFragment calls this when the user taps on a item in Today's Stats
    @Override
    public void onInsightsTodayClicked(final StatsVisitorsAndViewsFragment.OverviewLabel item) {
        mTabToSelectOnGraph = item;
        for (int i = 0; i < mTimeframes.length; i++) {
            if (mTimeframes[i] == StatsTimeframe.DAY) {
                mSpinner.setSelection(i);
                break;
            }
        }
    }

    private void refreshStatsFromCurrentDate() {
        if (mIsUpdatingStats) {
            AppLog.w(T.STATS, "stats are already updating, refresh cancelled");
            return;
        }

        mRequestedDate = StatsUtils.getCurrentDateTZ(mSite);
        updateTimeframeAndDateAndStartRefreshOfFragments(true);
    }

    // StatsVisitorsAndViewsFragment calls this when the user taps on a bar in the graph
    @Override
    public void onDateChanged(long siteId, StatsTimeframe timeframe, String date) {
        if (isFinishing()) {
            return;
        }
        mRequestedDate = date;
        updateTimeframeAndDateAndStartRefreshOfFragments(false);
        if (NetworkUtils.checkConnection(OldStatsActivity.this)) {
            mSwipeToRefreshHelper.setRefreshing(true);
        } else {
            mSwipeToRefreshHelper.setRefreshing(false);
        }
    }

    // StatsVisitorsAndViewsFragment calls this when the user taps on the tab bar to change the type of the graph
    @Override
    public void onOverviewItemChanged(StatsVisitorsAndViewsFragment.OverviewLabel newItem) {
        mTabToSelectOnGraph = newItem;
    }

    private void bumpPromoAnalyticsAndShowPromoDialogIfNecessary() {
        if (mIsUpdatingStats || mThereWasAnErrorLoadingStats) {
            // Do nothing in case of errors or when it's still loading
            return;
        }

        if (!StringUtils.isEmpty(AppPrefs.getStatsWidgetsKeys())) {
            // Stats widgets already used!!
            return;
        }

        // Bump analytics that drives the Promo widget when the loading is completed without errors.
        AppPrefs.bumpAnalyticsForStatsWidgetPromo();

        // Should we display the widget promo?

        if (shouldShowWidgetPromo()) {
            PromoDialog newFragment = new PromoDialog();
            newFragment.initialize(
                    TAG_PROMO_DIALOG,
                    getString(R.string.stats_widget_promo_title),
                    getString(R.string.stats_widget_promo_desc),
                    getString(R.string.stats_widget_promo_ok_btn_label),
                    R.drawable.img_promo_stats_widget);
            newFragment.show(getSupportFragmentManager(), TAG_PROMO_DIALOG);
        }
    }

    private boolean shouldShowWidgetPromo() {
        int counter = AppPrefs.getAnalyticsForStatsWidgetPromo();
        boolean isCountAPromoMilestone = counter == 3 || counter == 1000 || counter == 10000;

        return isCountAPromoMilestone && !DeviceUtils.getInstance().isChromebook(this);
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.UpdateStatusStarted event) {
        if (isFinishing() || !mIsInFront) {
            return;
        }
        mSwipeToRefreshHelper.setRefreshing(true);
        mIsUpdatingStats = true;
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.UpdateStatusFinished event) {
        if (isFinishing()) {
            return;
        }

        // in legacy StatsService we just need this signal to know one taskID grouped all concurrent requests
        if (event.mTaskId == TASK_ID_GROUP_ALL) {
            mCallsToWaitFor = 0;
        } else {
            if (mCallsToWaitFor > 0) {
                mCallsToWaitFor--;
            }
        }

        if (mCallsToWaitFor == 0) {
            mSwipeToRefreshHelper.setRefreshing(false);
        }
        mIsUpdatingStats = false;

        if (!mIsUpdatingStats && !mThereWasAnErrorLoadingStats) {
            // Do not bump promo analytics or show the dialog in case of errors or when it's still loading
            bumpPromoAnalyticsAndShowPromoDialogIfNecessary();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.SectionUpdateError event) {
        // There was an error loading Stats. Don't bump stats for promo widget.
        if (isFinishing() || !mIsInFront) {
            return;
        }

        // There was an error loading Stats. Don't bump stats for promo widget.
        mThereWasAnErrorLoadingStats = true;
    }

    /*
    * make sure the passed timeframe is the one selected in the actionbar
    */
    private void selectCurrentTimeframeInActionBar() {
        if (isFinishing()) {
            return;
        }

        if (mTimeframeSpinnerAdapter == null || mSpinner == null) {
            return;
        }

        int position = mTimeframeSpinnerAdapter.getIndexOfTimeframe(mCurrentTimeframe);

        if (position > -1 && position != mSpinner.getSelectedItemPosition()) {
            mSpinner.setSelection(position);
        }
    }

    /*
     * adapter used by the timeframe spinner
     */
    private class TimeframeSpinnerAdapter extends BaseAdapter {
        private final StatsTimeframe[] mTimeframes;
        private final LayoutInflater mInflater;

        TimeframeSpinnerAdapter(Context context, StatsTimeframe[] timeframeNames) {
            super();
            mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mTimeframes = timeframeNames;
        }

        @Override
        public int getCount() {
            return (mTimeframes != null ? mTimeframes.length : 0);
        }

        @Override
        public Object getItem(int position) {
            if (position < 0 || position >= getCount()) {
                return "";
            }
            return mTimeframes[position];
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final View view;
            if (convertView == null) {
                view = mInflater.inflate(R.layout.filter_spinner_item, parent, false);
            } else {
                view = convertView;
            }

            final TextView text = view.findViewById(R.id.text);
            StatsTimeframe selectedTimeframe = (StatsTimeframe) getItem(position);
            text.setText(selectedTimeframe.getLabel());
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            StatsTimeframe selectedTimeframe = (StatsTimeframe) getItem(position);
            final TagViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.toolbar_spinner_dropdown_item, parent, false);
                holder = new TagViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (TagViewHolder) convertView.getTag();
            }

            holder.mTextView.setText(selectedTimeframe.getLabel());
            return convertView;
        }

        private class TagViewHolder {
            private final TextView mTextView;

            TagViewHolder(View view) {
                mTextView = view.findViewById(R.id.text);
            }
        }

        public int getIndexOfTimeframe(StatsTimeframe tm) {
            int pos = 0;
            for (int i = 0; i < mTimeframes.length; i++) {
                if (mTimeframes[i] == tm) {
                    pos = i;
                    return pos;
                }
            }
            return pos;
        }
    }

    @Override
    public void onScrollChanged(NestedScrollViewExt scrollView, int x, int y, int oldx, int oldy) {
        // We take the last son in the scrollview
        View view = scrollView.getChildAt(scrollView.getChildCount() - 1);
        if (view == null) {
            return;
        }
        int diff = (view.getBottom() - (scrollView.getHeight() + scrollView.getScrollY() + view.getTop()));

        // if diff is zero, then the bottom has been reached
        if (diff == 0) {
            sTrackBottomReachedStats.runIfNotLimited();
        }
    }

    private static RateLimitedTask sTrackBottomReachedStats = new RateLimitedTask(2) {
        protected boolean run() {
            AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_SCROLLED_TO_BOTTOM);
            return true;
        }
    };

    // FluxC events

    @SuppressWarnings("unused")
    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onSiteChanged(OnSiteChanged event) {
        // "Reload" current site from the db, would be smarter if the OnSiteChanged provided the list of changed sites.
        SiteModel site = mSiteStore.getSiteByLocalId(mSite.getId());
        if (site != null) {
            mSite = site;
        }

        // Make sure the update site is accessible
        checkIfSiteHasAccessibleStats(mSite);

        // Refresh Stats
        refreshStatsFromCurrentDate();
    }
}
