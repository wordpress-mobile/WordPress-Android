package org.wordpress.android.ui.stats;

import android.app.AlertDialog;
import android.app.DialogFragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import org.apache.commons.lang.StringUtils;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.AccountHelper;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.ui.ActivityLauncher;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.posts.PromoDialog;
import org.wordpress.android.ui.prefs.AppPrefs;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.RateLimitedTask;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper.RefreshListener;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.ApiHelper.Method;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCFactory;

import java.util.HashMap;
import java.util.Map;

import de.greenrobot.event.EventBus;

/**
 * The native stats activity
 * <p>
 * By pressing a spinner on the action bar, the user can select which timeframe they wish to see.
 * </p>
 */
public class StatsActivity extends AppCompatActivity
        implements NestedScrollViewExt.ScrollViewListener,
                StatsVisitorsAndViewsFragment.OnDateChangeListener,
                StatsVisitorsAndViewsFragment.OnOverviewItemChangeListener,
                StatsInsightsTodayFragment.OnInsightsTodayClickListener {

    private static final String SAVED_WP_LOGIN_STATE = "SAVED_WP_LOGIN_STATE";
    private static final String SAVED_STATS_TIMEFRAME = "SAVED_STATS_TIMEFRAME";
    private static final String SAVED_STATS_REQUESTED_DATE = "SAVED_STATS_REQUESTED_DATE";
    private static final String SAVED_STATS_SCROLL_POSITION = "SAVED_STATS_SCROLL_POSITION";
    private static final String SAVED_THERE_WAS_AN_ERROR_LOADING_STATS = "SAVED_THERE_WAS_AN_ERROR_LOADING_STATS";

    private Spinner mSpinner;
    private NestedScrollViewExt mOuterScrollView;

    private static final int REQUEST_JETPACK = 7000;
    public static final String ARG_LOCAL_TABLE_BLOG_ID = "ARG_LOCAL_TABLE_BLOG_ID";
    public static final String ARG_LAUNCHED_FROM = "ARG_LAUNCHED_FROM";
    public static final String ARG_DESIRED_TIMEFRAME = "ARG_DESIRED_TIMEFRAME";

    public enum StatsLaunchedFrom {
        STATS_WIDGET,
        NOTIFICATIONS
    }

    private int mResultCode = -1;
    private boolean mIsInFront;
    private int mLocalBlogID = -1;
    private StatsTimeframe mCurrentTimeframe = StatsTimeframe.INSIGHTS;
    private String mRequestedDate;
    private boolean mIsUpdatingStats;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private TimeframeSpinnerAdapter mTimeframeSpinnerAdapter;
    private final StatsTimeframe[] timeframes = {StatsTimeframe.INSIGHTS, StatsTimeframe.DAY, StatsTimeframe.WEEK,
            StatsTimeframe.MONTH, StatsTimeframe.YEAR};
    private StatsVisitorsAndViewsFragment.OverviewLabel mTabToSelectOnGraph = StatsVisitorsAndViewsFragment.OverviewLabel.VIEWS;

    private boolean mThereWasAnErrorLoadingStats = false;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        setContentView(R.layout.stats_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setElevation(0);
            actionBar.setTitle(R.string.stats);
            actionBar.setDisplayShowTitleEnabled(true);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        mSwipeToRefreshHelper = new SwipeToRefreshHelper(this, (CustomSwipeRefreshLayout) findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted() {

                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            return;
                        }

                        if (mIsUpdatingStats) {
                            AppLog.w(T.STATS, "stats are already updating, refresh cancelled");
                            return;
                        }

                        mRequestedDate = StatsUtils.getCurrentDateTZ(mLocalBlogID);
                        if (checkCredentials()) {
                            updateTimeframeAndDateAndStartRefreshOfFragments(true);
                        }
                    }
                });

        setTitle(R.string.stats);

        mOuterScrollView = (NestedScrollViewExt) findViewById(R.id.scroll_view_stats);
        mOuterScrollView.setScrollViewListener(this);

        if (savedInstanceState != null) {
            mResultCode = savedInstanceState.getInt(SAVED_WP_LOGIN_STATE);
            mLocalBlogID = savedInstanceState.getInt(ARG_LOCAL_TABLE_BLOG_ID);
            mCurrentTimeframe = (StatsTimeframe) savedInstanceState.getSerializable(SAVED_STATS_TIMEFRAME);
            mRequestedDate = savedInstanceState.getString(SAVED_STATS_REQUESTED_DATE);
            mThereWasAnErrorLoadingStats = savedInstanceState.getBoolean(SAVED_THERE_WAS_AN_ERROR_LOADING_STATS);
            final int yScrollPosition = savedInstanceState.getInt(SAVED_STATS_SCROLL_POSITION);
            if(yScrollPosition != 0) {
                mOuterScrollView.postDelayed(new Runnable() {
                    public void run() {
                        if (!isFinishing()) {
                            mOuterScrollView.scrollTo(0, yScrollPosition);
                        }
                    }
                }, StatsConstants.STATS_SCROLL_TO_DELAY);
            }
        } else if (getIntent() != null) {
            mLocalBlogID = getIntent().getIntExtra(ARG_LOCAL_TABLE_BLOG_ID, -1);
            if (getIntent().hasExtra(SAVED_STATS_TIMEFRAME)) {
                mCurrentTimeframe = (StatsTimeframe) getIntent().getSerializableExtra(SAVED_STATS_TIMEFRAME);
            } else if (getIntent().hasExtra(ARG_DESIRED_TIMEFRAME)) {
                mCurrentTimeframe = (StatsTimeframe) getIntent().getSerializableExtra(ARG_DESIRED_TIMEFRAME);
            } else {
                // Read the value from app preferences here. Default to 0 - Insights
                mCurrentTimeframe = AppPrefs.getStatsTimeframe();
            }
            mRequestedDate = StatsUtils.getCurrentDateTZ(mLocalBlogID);

            if (getIntent().hasExtra(ARG_LAUNCHED_FROM)) {
                StatsLaunchedFrom from = (StatsLaunchedFrom) getIntent().getSerializableExtra(ARG_LAUNCHED_FROM);
                if (from == StatsLaunchedFrom.STATS_WIDGET) {
                    AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.STATS_WIDGET_TAPPED, WordPress.getBlog(mLocalBlogID));
                }
            }

        }

        //Make sure the blog_id passed to this activity is valid and the blog is available within the app
        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);

        if (currentBlog == null) {
            AppLog.e(T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
            Toast.makeText(this, R.string.stats_no_blog, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // create the fragments without forcing the re-creation. If the activity is restarted fragments can already
        // be there, and ready to be displayed without making any network connections. A fragment calls the stats service
        // if its internal datamodel is empty.
        createFragments(false);

        if (mSpinner == null) {
            mSpinner = (Spinner) findViewById(R.id.filter_spinner);

            mTimeframeSpinnerAdapter = new TimeframeSpinnerAdapter(this, timeframes);

            mSpinner.setAdapter(mTimeframeSpinnerAdapter);
            mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                @Override
                public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                    if (isFinishing()) {
                        return;
                    }
                    final StatsTimeframe selectedTimeframe =  (StatsTimeframe) mTimeframeSpinnerAdapter.getItem(position);

                    if (mCurrentTimeframe == selectedTimeframe) {
                        AppLog.d(T.STATS, "The selected TIME FRAME is already active: " + selectedTimeframe.getLabel());
                        return;
                    }

                    AppLog.d(T.STATS, "NEW TIME FRAME : " + selectedTimeframe.getLabel());
                    mCurrentTimeframe = selectedTimeframe;
                    AppPrefs.setStatsTimeframe(mCurrentTimeframe);
                    mRequestedDate = StatsUtils.getCurrentDateTZ(mLocalBlogID);
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

            Toolbar spinnerToolbar = (Toolbar) findViewById(R.id.toolbar_filter);
            spinnerToolbar.setBackgroundColor(getResources().getColor(R.color.blue_medium));

        }

        selectCurrentTimeframeInActionBar();

        TextView otherRecentStatsMovedLabel = (TextView) findViewById(R.id.stats_other_recent_stats_moved);
        otherRecentStatsMovedLabel.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                for (int i = 0; i < timeframes.length; i++) {
                    if (timeframes[i] ==  StatsTimeframe.INSIGHTS) {
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
            AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.STATS_ACCESSED, currentBlog);
            trackStatsAnalytics();
        }
    }

    private void trackStatsAnalytics() {
        // Track usage here
        Blog currentBlog = WordPress.getBlog(mLocalBlogID);
        switch (mCurrentTimeframe) {
            case INSIGHTS:
                AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.STATS_INSIGHTS_ACCESSED, currentBlog);
                break;
            case DAY:
                AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.STATS_PERIOD_DAYS_ACCESSED, currentBlog);
                break;
            case WEEK:
                AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.STATS_PERIOD_WEEKS_ACCESSED, currentBlog);
                break;
            case MONTH:
                AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.STATS_PERIOD_MONTHS_ACCESSED, currentBlog);
                break;
            case YEAR:
                AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.STATS_PERIOD_YEARS_ACCESSED, currentBlog);
                break;
        }
    }

    @Override
    public void finish() {
        super.finish();
        ActivityLauncher.slideOutToRight(this);
    }

    @Override
    protected void onStop() {
        EventBus.getDefault().unregister(this);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        EventBus.getDefault().register(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsInFront = true;
        if (NetworkUtils.checkConnection(this)) {
            checkCredentials();
        } else {
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
        outState.putInt(ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
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

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        StatsAbstractFragment fragment;

        if (mCurrentTimeframe != StatsTimeframe.INSIGHTS) {
            findViewById(R.id.stats_timeline_fragments_container).setVisibility(View.VISIBLE);
            findViewById(R.id.stats_insights_fragments_container).setVisibility(View.GONE);

            if (fm.findFragmentByTag(StatsVisitorsAndViewsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newVisitorsAndViewsInstance(StatsViewType.GRAPH_AND_SUMMARY, mLocalBlogID, mCurrentTimeframe, mRequestedDate,
                        mTabToSelectOnGraph);
                ft.replace(R.id.stats_visitors_and_views_container, fragment, StatsVisitorsAndViewsFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsTopPostsAndPagesFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.TOP_POSTS_AND_PAGES, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_top_posts_container, fragment, StatsTopPostsAndPagesFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsReferrersFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.REFERRERS, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_referrers_container, fragment, StatsReferrersFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsClicksFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.CLICKS, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_clicks_container, fragment, StatsClicksFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsGeoviewsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.GEOVIEWS, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_geoviews_container, fragment, StatsGeoviewsFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsAuthorsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.AUTHORS, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_top_authors_container, fragment, StatsAuthorsFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsVideoplaysFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.VIDEO_PLAYS, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_video_container, fragment, StatsVideoplaysFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsSearchTermsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.SEARCH_TERMS, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_search_terms_container, fragment, StatsSearchTermsFragment.TAG);
            }

        } else {
            findViewById(R.id.stats_timeline_fragments_container).setVisibility(View.GONE);
            findViewById(R.id.stats_insights_fragments_container).setVisibility(View.VISIBLE);

            if (fm.findFragmentByTag(StatsInsightsMostPopularFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.INSIGHTS_MOST_POPULAR, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_insights_most_popular_container, fragment, StatsInsightsMostPopularFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsInsightsAllTimeFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.INSIGHTS_ALL_TIME, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_insights_all_time_container, fragment, StatsInsightsAllTimeFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsInsightsTodayFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.INSIGHTS_TODAY, mLocalBlogID, StatsTimeframe.DAY, mRequestedDate);
                ft.replace(R.id.stats_insights_today_container, fragment, StatsInsightsTodayFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsInsightsLatestPostSummaryFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.INSIGHTS_LATEST_POST_SUMMARY, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_insights_latest_post_summary_container, fragment, StatsInsightsLatestPostSummaryFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsCommentsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.COMMENTS, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_comments_container, fragment, StatsCommentsFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsTagsAndCategoriesFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.TAGS_AND_CATEGORIES, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_tags_and_categories_container, fragment, StatsTagsAndCategoriesFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsPublicizeFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.PUBLICIZE, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
                ft.replace(R.id.stats_publicize_container, fragment, StatsPublicizeFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsFollowersFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.FOLLOWERS, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
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
                findViewById(R.id.stats_other_recent_stats_label_insights).setVisibility(isInsights ? View.VISIBLE : View.GONE);
                findViewById(R.id.stats_other_recent_stats_label_timeline).setVisibility(isInsights ? View.GONE : View.VISIBLE);
                findViewById(R.id.stats_other_recent_stats_moved).setVisibility(isInsights ? View.GONE : View.VISIBLE);
            }
        }, StatsConstants.STATS_LABELS_SETUP_DELAY);
    }

    private void updateTimeframeAndDateAndStartRefreshOfFragments(boolean includeGraph) {
        if (isFinishing()) {
            return;
        }
        FragmentManager fm = getFragmentManager();

        if (mCurrentTimeframe != StatsTimeframe.INSIGHTS) {
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsTopPostsAndPagesFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsReferrersFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsClicksFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsGeoviewsFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsAuthorsFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsVideoplaysFragment.TAG);
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsSearchTermsFragment.TAG);
            if (includeGraph) {
                updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsVisitorsAndViewsFragment.TAG);
            }
        } else {
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

    private boolean updateTimeframeAndDateAndStartRefreshInFragment(FragmentManager fm , String fragmentTAG) {
        StatsAbstractFragment fragment = (StatsAbstractFragment) fm.findFragmentByTag(fragmentTAG);
        if (fragment != null) {
            fragment.setDate(mRequestedDate);
            fragment.setTimeframe(mCurrentTimeframe);
            fragment.refreshStats();
            return true;
        }
        return false;
    }

    private void startWPComLoginActivity() {
        mResultCode = RESULT_CANCELED;
        Intent signInIntent = new Intent(this, SignInActivity.class);
        signInIntent.putExtra(SignInActivity.EXTRA_JETPACK_SITE_AUTH, mLocalBlogID);
        signInIntent.putExtra(
                SignInActivity.EXTRA_JETPACK_MESSAGE_AUTH,
                getString(R.string.stats_sign_in_jetpack_different_com_account)
        );
        startActivityForResult(signInIntent, SignInActivity.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SignInActivity.REQUEST_CODE) {
            if (resultCode == RESULT_CANCELED) {
                finish();
            }
            mResultCode = resultCode;
            final Blog currentBlog = WordPress.getBlog(mLocalBlogID);
            if (resultCode == RESULT_OK && currentBlog != null && !currentBlog.isDotcomFlag()) {
                if (currentBlog.getDotComBlogId() == null) {
                    final Handler handler = new Handler();
                    // Attempt to get the Jetpack blog ID
                    XMLRPCClientInterface xmlrpcClient = XMLRPCFactory.instantiate(currentBlog.getUri(), "", "");
                    Map<String, String> args = ApiHelper.blogOptionsXMLRPCParameters;
                    Object[] params = {
                            currentBlog.getRemoteBlogId(), currentBlog.getUsername(), currentBlog.getPassword(), args
                    };
                    xmlrpcClient.callAsync(new XMLRPCCallback() {
                        @Override
                        public void onSuccess(long id, Object result) {
                            if (result != null && (result instanceof HashMap)) {
                                Map<?, ?> blogOptions = (HashMap<?, ?>) result;
                                ApiHelper.updateBlogOptions(currentBlog, blogOptions);
                                AnalyticsUtils.refreshMetadata();
                                AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.SIGNED_INTO_JETPACK, currentBlog);
                                AnalyticsUtils.trackWithBlogDetails(
                                        AnalyticsTracker.Stat.PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN, currentBlog);
                                if (isFinishing()) {
                                    return;
                                }
                                // We have the blogID now, but we need to re-check if the network connection is available
                                if (NetworkUtils.checkConnection(StatsActivity.this)) {
                                    handler.post(new Runnable() {
                                        @Override
                                        public void run() {
                                            mSwipeToRefreshHelper.setRefreshing(true);
                                            mRequestedDate = StatsUtils.getCurrentDateTZ(mLocalBlogID);
                                            createFragments(true); // Recreate the fragment and start a refresh of Stats
                                        }
                                    });
                                }
                            }
                        }
                        @Override
                        public void onFailure(long id, Exception error) {
                            AppLog.e(T.STATS,
                                    "Cannot load blog options (wp.getOptions failed) "
                                    + "and no jetpack_client_id is then available",
                                    error);
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    mSwipeToRefreshHelper.setRefreshing(false);
                                    ToastUtils.showToast(StatsActivity.this,
                                            StatsActivity.this.getString(R.string.error_refresh_stats),
                                            Duration.LONG);
                                }
                            });
                        }
                    }, Method.GET_OPTIONS, params);
                } else {
                    mRequestedDate =  StatsUtils.getCurrentDateTZ(mLocalBlogID);
                    createFragments(true); // Recreate the fragment and start a refresh of Stats
                }
                mSwipeToRefreshHelper.setRefreshing(true);
            }
        }
    }

    private class VerifyJetpackSettingsCallback implements ApiHelper.GenericCallback {
        // AsyncTasks are bound to the Activity that launched it. If the user rotate the device StatsActivity is restarted.
        // Use the event bus to fix this issue.

        @Override
        public void onSuccess() {
            EventBus.getDefault().post(new StatsEvents.JetpackSettingsCompleted(false));
        }

        @Override
        public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
            EventBus.getDefault().post(new StatsEvents.JetpackSettingsCompleted(true));
        }
    }

    private void showJetpackNonConnectedAlert() {
        if (isFinishing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);
        if (currentBlog == null) {
            AppLog.e(T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
            Toast.makeText(this, R.string.stats_no_blog, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (currentBlog.isAdmin()) {
            builder.setMessage(getString(R.string.jetpack_not_connected_message))
                    .setTitle(getString(R.string.jetpack_not_connected));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String stringToLoad = currentBlog.getAdminUrl();
                    String jetpackConnectPageAdminPath = "admin.php?page=jetpack";
                    stringToLoad = stringToLoad.endsWith("/") ? stringToLoad + jetpackConnectPageAdminPath :
                            stringToLoad + "/" + jetpackConnectPageAdminPath;
                    String authURL = WPWebViewActivity.getBlogLoginUrl(currentBlog);
                    Intent jetpackIntent = new Intent(StatsActivity.this, WPWebViewActivity.class);
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, currentBlog.getUsername());
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, currentBlog.getPassword());
                    jetpackIntent.putExtra(WPWebViewActivity.URL_TO_LOAD, stringToLoad);
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
                    startActivityForResult(jetpackIntent, REQUEST_JETPACK);
                    AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_SELECTED_CONNECT_JETPACK);
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog. Hide Stats.
                    finish();
                }
            });
        } else {
            builder.setMessage(getString(R.string.jetpack_message_not_admin))
                    .setTitle(getString(R.string.jetpack_not_found));
            builder.setPositiveButton(R.string.yes, null);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // User pressed the back key Hide Stats.
                finish();
            }
        });
        dialog.show();
    }

    private void showJetpackMissingAlert() {
        if (isFinishing()) {
            return;
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);
        if (currentBlog == null) {
            AppLog.e(T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
            Toast.makeText(this, R.string.stats_no_blog, Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        if (currentBlog.isAdmin()) {
            builder.setMessage(getString(R.string.jetpack_message))
                    .setTitle(getString(R.string.jetpack_not_found));
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    String stringToLoad = currentBlog.getAdminUrl()
                            + "plugin-install.php?tab=search&s=jetpack+by+wordpress.com"
                            + "&plugin-search-input=Search+Plugins";
                    String authURL = WPWebViewActivity.getBlogLoginUrl(currentBlog);
                    Intent jetpackIntent = new Intent(StatsActivity.this, WPWebViewActivity.class);
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_USER, currentBlog.getUsername());
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_PASSWD, currentBlog.getPassword());
                    jetpackIntent.putExtra(WPWebViewActivity.URL_TO_LOAD, stringToLoad);
                    jetpackIntent.putExtra(WPWebViewActivity.AUTHENTICATION_URL, authURL);
                    startActivityForResult(jetpackIntent, REQUEST_JETPACK);
                    AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_SELECTED_INSTALL_JETPACK);
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int id) {
                    // User cancelled the dialog. Hide Stats.
                    finish();
                }
            });
        } else {
            builder.setMessage(getString(R.string.jetpack_message_not_admin))
                    .setTitle(getString(R.string.jetpack_not_found));
            builder.setPositiveButton(R.string.yes, null);
        }

        AlertDialog dialog = builder.create();
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                // User pressed the back key Hide Stats.
                finish();
            }
        });
        dialog.show();
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

    private void scrollToTop() {
        mOuterScrollView.fullScroll(ScrollView.FOCUS_UP);
    }

    // StatsInsightsTodayFragment calls this when the user taps on a item in Today's Stats
    @Override
    public void onInsightsTodayClicked(final StatsVisitorsAndViewsFragment.OverviewLabel item) {
        mTabToSelectOnGraph = item;
        for (int i = 0; i < timeframes.length; i++) {
            if (timeframes[i] ==  StatsTimeframe.DAY) {
                mSpinner.setSelection(i);
                break;
            }
        }
    }

    // StatsVisitorsAndViewsFragment calls this when the user taps on a bar in the graph
    @Override
    public void onDateChanged(String blogID, StatsTimeframe timeframe, String date) {
        if (isFinishing()) {
            return;
        }
        mRequestedDate = date;
        updateTimeframeAndDateAndStartRefreshOfFragments(false);
        if (NetworkUtils.checkConnection(StatsActivity.this)) {
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

    private boolean checkCredentials() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AppLog.w(AppLog.T.STATS, "StatsActivity > cannot check credentials since no internet connection available");
            return false;
        }

        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);
        if (currentBlog == null) {
            AppLog.e(T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
            return false;
        }

        final String blogId = currentBlog.getDotComBlogId();

        // blogId is always available for dotcom blogs. It could be null on Jetpack blogs...
        if (blogId != null) {
            // for self-hosted sites; launch the user into an activity where they can provide their credentials
            if (!currentBlog.isDotcomFlag()
                    && !currentBlog.hasValidJetpackCredentials() && mResultCode != RESULT_CANCELED) {
                if (AccountHelper.isSignedInWordPressDotCom()) {
                    // Let's try the global wpcom credentials them first
                    String username = AccountHelper.getDefaultAccount().getUserName();
                    currentBlog.setDotcom_username(username);
                    WordPress.wpDB.saveBlog(currentBlog);
                    createFragments(true);
                } else {
                    startWPComLoginActivity();
                    return false;
                }
            }
        } else {
            // blogId is null at this point.
            if (!currentBlog.isDotcomFlag()) {
                // Refresh blog settings/options that includes 'jetpack_client_id' needed here
                mSwipeToRefreshHelper.setRefreshing(true);
                new ApiHelper.RefreshBlogContentTask(currentBlog,
                        new VerifyJetpackSettingsCallback()).execute(false);
                return false;
            } else {
                // blodID cannot be null on dotcom blogs.
                Toast.makeText(this, R.string.error_refresh_stats, Toast.LENGTH_LONG).show();
                AppLog.e(T.STATS, "blogID is null for a wpcom blog!! " + currentBlog.getHomeURL());
                finish();
            }
        }

        return true;
    }

    private void bumpPromoAnaylticsAndShowPromoDialogIfNecessary() {
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
        int counter = AppPrefs.getAnalyticsForStatsWidgetPromo();
        if (counter == 3 || counter == 1000 || counter == 10000) {
            DialogFragment newFragment = PromoDialog.newInstance(R.drawable.stats_widget_promo_header,
                    R.string.stats_widget_promo_title, R.string.stats_widget_promo_desc,
                    R.string.stats_widget_promo_ok_btn_label);
            newFragment.show(getFragmentManager(), "promote_widget_dialog");
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.UpdateStatusChanged event) {
        if (isFinishing() || !mIsInFront) {
            return;
        }
        mSwipeToRefreshHelper.setRefreshing(event.mUpdating);
        mIsUpdatingStats = event.mUpdating;

        if (!mIsUpdatingStats && !mThereWasAnErrorLoadingStats) {
            // Do not bump promo analytics or show the dialog in case of errors or when it's still loading
            bumpPromoAnaylticsAndShowPromoDialogIfNecessary();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.JetpackSettingsCompleted event) {
        if (isFinishing() || !mIsInFront) {
            return;
        }
        mSwipeToRefreshHelper.setRefreshing(false);

        if (!event.isError) {
            final Blog currentBlog = WordPress.getBlog(mLocalBlogID);
            if (currentBlog == null) {
                AppLog.e(T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
                Toast.makeText(this, R.string.stats_no_blog, Toast.LENGTH_LONG).show();
                finish();
                return;
            }
            if (currentBlog.getDotComBlogId() == null) {
                if (TextUtils.isEmpty(currentBlog.getJetpackVersion())) {
                    // jetpack_version option is available, but not the jetpack_client_id ----> Jetpack available but not connected.
                    showJetpackNonConnectedAlert();
                } else {
                    // Blog has not returned jetpack_version/jetpack_client_id.
                    showJetpackMissingAlert();
                }
            } else {
                checkCredentials();
            }
        } else {
            Toast.makeText(StatsActivity.this, R.string.error_refresh_stats, Toast.LENGTH_LONG).show();
            finish();
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

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.JetpackAuthError event) {
        if (isFinishing() || !mIsInFront) {
            return;
        }

        // There was an error loading Stats. Don't bump stats for promo widget.
        mThereWasAnErrorLoadingStats = true;

        if (event.mLocalBlogId != mLocalBlogID) {
            // The user has changed blog
            return;
        }

        mSwipeToRefreshHelper.setRefreshing(false);
        startWPComLoginActivity();
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
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            mTimeframes = timeframeNames;
        }

        @Override
        public int getCount() {
            return (mTimeframes != null ? mTimeframes.length : 0);
        }

        @Override
        public Object getItem(int position) {
            if (position < 0 || position >= getCount())
                return "";
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

            final TextView text = (TextView) view.findViewById(R.id.text);
            StatsTimeframe selectedTimeframe = (StatsTimeframe)getItem(position);
            text.setText(selectedTimeframe.getLabel());
            return view;
        }

        @Override
        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            StatsTimeframe selectedTimeframe = (StatsTimeframe)getItem(position);
            final TagViewHolder holder;

            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.toolbar_spinner_dropdown_item, parent, false);
                holder = new TagViewHolder(convertView);
                convertView.setTag(holder);
            } else {
                holder = (TagViewHolder) convertView.getTag();
            }

            holder.textView.setText(selectedTimeframe.getLabel());
            return convertView;
        }

        private class TagViewHolder {
            private final TextView textView;
            TagViewHolder(View view) {
                textView = (TextView) view.findViewById(R.id.text);
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

    private static final RateLimitedTask sTrackBottomReachedStats = new RateLimitedTask(2) {
        protected boolean run() {
            AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_SCROLLED_TO_BOTTOM);
            return true;
        }
    };
}
