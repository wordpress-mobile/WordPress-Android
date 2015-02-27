package org.wordpress.android.ui.stats;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.widget.Toolbar;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.VolleyError;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.WPDrawerActivity;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.RateLimitedTask;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper;
import org.wordpress.android.util.ptr.SwipeToRefreshHelper.RefreshListener;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCFactory;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * The native stats activity, accessible via the menu drawer.
 * <p>
 * By pressing a spinner on the action bar, the user can select which timeframe they wish to see.
 * </p>
 */
public class StatsActivity extends WPDrawerActivity implements ScrollViewExt.ScrollViewListener,
        StatsAuthorsFragment.OnAuthorsSectionChangeListener,
        StatsVisitorsAndViewsFragment.OnDateChangeListener,
        StatsAbstractListFragment.OnRequestDataListener,
        StatsAbstractFragment.TimeframeDateProvider {
    private static final String SAVED_NAV_POSITION = "SAVED_NAV_POSITION";
    private static final String SAVED_WP_LOGIN_STATE = "SAVED_WP_LOGIN_STATE";
    private static final String SAVED_STATS_TIMEFRAME = "SAVED_STATS_TIMEFRAME";
    private static final String SAVED_STATS_REQUESTED_DATE= "SAVED_STATS_REQUESTED_DATE";

    private Spinner mSpinner;

    private static final int REQUEST_JETPACK = 7000;

    public static final String ARG_NO_MENU_DRAWER = "no_menu_drawer";
    public static final String ARG_LOCAL_TABLE_BLOG_ID = "ARG_LOCAL_TABLE_BLOG_ID";

    private Dialog mSignInDialog;
    private int mNavPosition = 0;

    private int mResultCode = -1;
    private boolean mIsInFront;
    private boolean mNoMenuDrawer = false;
    private int mLocalBlogID = -1;
    private StatsTimeframe mCurrentTimeframe = StatsTimeframe.DAY;
    private String mRequestedDate;
    private boolean mIsUpdatingStats;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private TimeframeSpinnerAdapter mTimeframeSpinnerAdapter;

    private ArrayList<StatsService.StatsEndpointsEnum> fragmentsRefreshList = new ArrayList<>();
    private final Object fragmentsRefreshListSynchObj = new Object();
    private final Handler mUpdateStatsHandler = new Handler();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        if (savedInstanceState == null) {
            AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_ACCESSED);
        }

        mNoMenuDrawer = getIntent().getBooleanExtra(ARG_NO_MENU_DRAWER, false);
        ActionBar actionBar = getSupportActionBar();
        createMenuDrawer(R.layout.stats_activity);
        if (mNoMenuDrawer) {
            getDrawerToggle().setDrawerIndicatorEnabled(false);
            // Override the default NavigationOnClickListener
            getToolbar().setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    onBackPressed();
                }
            });
        }

        mSwipeToRefreshHelper = new SwipeToRefreshHelper(this, (SwipeRefreshLayout) findViewById(R.id.ptr_layout),
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
                        loadStatsFragments(false, true, true); // This is here just for a security check
                        emptyDataModelInFragments(true, true);
                        refreshStats(mCurrentTimeframe, mRequestedDate, true, true);
                    }
                });

        setTitle(R.string.stats);

        if (savedInstanceState != null) {
            mNavPosition = savedInstanceState.getInt(SAVED_NAV_POSITION);
            mResultCode = savedInstanceState.getInt(SAVED_WP_LOGIN_STATE);
            mLocalBlogID = savedInstanceState.getInt(ARG_LOCAL_TABLE_BLOG_ID);
            mCurrentTimeframe = (StatsTimeframe) savedInstanceState.getSerializable(SAVED_STATS_TIMEFRAME);
            mRequestedDate = savedInstanceState.getString(SAVED_STATS_REQUESTED_DATE);
        } else if (getIntent() != null) {
            mLocalBlogID = getIntent().getIntExtra(ARG_LOCAL_TABLE_BLOG_ID, -1);
            if (getIntent().hasExtra(SAVED_STATS_TIMEFRAME)) {
                mCurrentTimeframe = (StatsTimeframe) getIntent().getSerializableExtra(SAVED_STATS_TIMEFRAME);
            } else {
                mCurrentTimeframe = StatsTimeframe.DAY;
            }
            mRequestedDate = StatsUtils.getCurrentDateTZ(mLocalBlogID);
        }

        //Make sure the blog_id passed to this activity is valid and the blog is available within the app
        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);

        if (currentBlog == null) {
            AppLog.e(T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
            Toast.makeText(this, R.string.stats_no_blog, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadStatsFragments(false, true, true);

        ScrollViewExt scrollView = (ScrollViewExt) findViewById(R.id.scroll_view_stats);
        if (scrollView != null) {
            scrollView.setScrollViewListener(this);
        }

        if (!mNoMenuDrawer && actionBar != null && mSpinner == null) {
            final Toolbar toolbar = getToolbar();
            if (toolbar != null) {
                View view = View.inflate(this, R.layout.reader_spinner, toolbar);
                mSpinner = (Spinner) view.findViewById(R.id.action_bar_spinner);

                StatsTimeframe[] timeframes = {StatsTimeframe.DAY, StatsTimeframe.WEEK,
                        StatsTimeframe.MONTH, StatsTimeframe.YEAR};
                mTimeframeSpinnerAdapter = new TimeframeSpinnerAdapter(this, timeframes);

                actionBar.setDisplayShowTitleEnabled(false);
                mSpinner.setAdapter(mTimeframeSpinnerAdapter);
                mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                        if (isFinishing() || isActivityDestroyed()) {
                            return;
                        }
                        final StatsTimeframe selectedTimeframe =  (StatsTimeframe) mTimeframeSpinnerAdapter.getItem(position);

                        if (mCurrentTimeframe == selectedTimeframe) {
                            AppLog.d(T.STATS, "The selected TIME FRAME is already active: " + selectedTimeframe.getLabel());
                            return;
                        }

                        AppLog.d(T.STATS, "NEW TIME FRAME : " + selectedTimeframe.getLabel());
                        mCurrentTimeframe = selectedTimeframe;
                        if (NetworkUtils.isNetworkAvailable(StatsActivity.this)) {
                            String date = StatsUtils.getCurrentDateTZ(mLocalBlogID);
                            mSwipeToRefreshHelper.setRefreshing(true);
                            refreshStats(selectedTimeframe, date, true, true);
                            emptyDataModelInFragments(true, false);
                            loadStatsFragments(false, true, false); // This is here just for a security check
                        }
                    }
                    @Override
                    public void onNothingSelected(AdapterView<?> parent) {
                        // nop
                    }
                });
            }
        }

        selectCurrentTimeframeInActionBar();
    }

    @Override
    protected void onDestroy() {
        stopStatsService();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsInFront = true;
        // register to receive broadcasts when StatsService starts/stops updating
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsService.ACTION_STATS_UPDATING));
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsInFront = false;
        mIsUpdatingStats = false;
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(mReceiver);
        mSwipeToRefreshHelper.setRefreshing(false);
        mUpdateStatsHandler.removeCallbacks(mUpdateStatsRequestedRunnable);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_NAV_POSITION, mNavPosition);
        outState.putInt(SAVED_WP_LOGIN_STATE, mResultCode);
        outState.putInt(ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        outState.putSerializable(SAVED_STATS_TIMEFRAME, mCurrentTimeframe);
        outState.putString(SAVED_STATS_REQUESTED_DATE, mRequestedDate);
        super.onSaveInstanceState(outState);
    }

    private void resetDatamodelForFragment( FragmentManager fm , String fragmentTAG) {
        StatsAbstractFragment fragment = (StatsAbstractFragment) fm.findFragmentByTag(fragmentTAG);
        if (fragment != null) {
            fragment.resetDataModel();
        }
    }

    private void emptyDataModelInFragments(boolean resetGraphData, boolean resetAlltimeFragmets) {
        FragmentManager fm = getFragmentManager();
        if (resetGraphData) {
            resetDatamodelForFragment(fm, StatsVisitorsAndViewsFragment.TAG);
        }
        resetDatamodelForFragment(fm, StatsTopPostsAndPagesFragment.TAG);
        resetDatamodelForFragment(fm, StatsReferrersFragment.TAG);
        resetDatamodelForFragment(fm, StatsClicksFragment.TAG);
        resetDatamodelForFragment(fm, StatsGeoviewsFragment.TAG);
        resetDatamodelForFragment(fm, StatsAuthorsFragment.TAG);
        resetDatamodelForFragment(fm, StatsVideoplaysFragment.TAG);
        if (resetAlltimeFragmets) {
            resetDatamodelForFragment(fm, StatsCommentsFragment.TAG);
            resetDatamodelForFragment(fm, StatsTagsAndCategoriesFragment.TAG);
            resetDatamodelForFragment(fm, StatsPublicizeFragment.TAG);
            resetDatamodelForFragment(fm, StatsFollowersFragment.TAG);
        }
    }

    private void loadStatsFragments(boolean forceRecreationOfFragments, boolean loadGraphFragment, boolean loadAlltimeFragmets) {
        if (isFinishing() || isActivityDestroyed()) {
            return;
        }
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        StatsAbstractFragment fragment;

        if (loadGraphFragment) {
            if (fm.findFragmentByTag(StatsVisitorsAndViewsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.GRAPH_AND_SUMMARY, mLocalBlogID);
                ft.replace(R.id.stats_visitors_and_views_container, fragment, StatsVisitorsAndViewsFragment.TAG);
            }
        }

        if (fm.findFragmentByTag(StatsTopPostsAndPagesFragment.TAG) == null || forceRecreationOfFragments) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.TOP_POSTS_AND_PAGES, mLocalBlogID);
            ft.replace(R.id.stats_top_posts_container, fragment, StatsTopPostsAndPagesFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsReferrersFragment.TAG) == null || forceRecreationOfFragments) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.REFERRERS, mLocalBlogID);
            ft.replace(R.id.stats_referrers_container, fragment, StatsReferrersFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsClicksFragment.TAG) == null || forceRecreationOfFragments) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.CLICKS, mLocalBlogID);
            ft.replace(R.id.stats_clicks_container, fragment, StatsClicksFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsGeoviewsFragment.TAG) == null || forceRecreationOfFragments) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.GEOVIEWS, mLocalBlogID);
            ft.replace(R.id.stats_geoviews_container, fragment, StatsGeoviewsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsAuthorsFragment.TAG) == null || forceRecreationOfFragments) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.AUTHORS, mLocalBlogID);
            ft.replace(R.id.stats_top_authors_container, fragment, StatsAuthorsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsVideoplaysFragment.TAG) == null || forceRecreationOfFragments) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.VIDEO_PLAYS, mLocalBlogID);
            ft.replace(R.id.stats_video_container, fragment, StatsVideoplaysFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsSearchTermsFragment.TAG) == null || forceRecreationOfFragments) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.SEARCH_TERMS, mLocalBlogID);
            ft.replace(R.id.stats_search_terms_container, fragment, StatsSearchTermsFragment.TAG);
        }

        if (loadAlltimeFragmets) {
            if (fm.findFragmentByTag(StatsCommentsFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.COMMENTS, mLocalBlogID);
                ft.replace(R.id.stats_comments_container, fragment, StatsCommentsFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsTagsAndCategoriesFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.TAGS_AND_CATEGORIES, mLocalBlogID);
                ft.replace(R.id.stats_tags_and_categories_container, fragment, StatsTagsAndCategoriesFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsPublicizeFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.PUBLICIZE, mLocalBlogID);
                ft.replace(R.id.stats_publicize_container, fragment, StatsPublicizeFragment.TAG);
            }

            if (fm.findFragmentByTag(StatsFollowersFragment.TAG) == null || forceRecreationOfFragments) {
                fragment = StatsAbstractFragment.newInstance(StatsViewType.FOLLOWERS, mLocalBlogID);
                ft.replace(R.id.stats_followers_container, fragment, StatsFollowersFragment.TAG);
            }
        }

        ft.commitAllowingStateLoss();
    }

    // AuthorsFragment should be dismissed when 0 or 1 author.
    public void onAuthorsVisibilityChange(boolean isEmpty) {
        View authorsContainer = this.findViewById(R.id.stats_top_authors_container);
        if (authorsContainer != null) {
            authorsContainer.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
    }

    @Override
    public void onMoreDataRequested(StatsService.StatsEndpointsEnum endPointNeedUpdate, int pageNumber) {
        // nope
    }

    @Override
    public void onRefreshRequested(StatsService.StatsEndpointsEnum[] endPointsNeedUpdate) {
        mSwipeToRefreshHelper.setRefreshing(mIsUpdatingStats);
        if (mIsUpdatingStats) {
            return;
        }

        // fragments that needs an update require it at almost the same time. Keep a list of fragments
        // that require the update and call the service with the right parameters.
        synchronized (fragmentsRefreshListSynchObj) {
            for (int i = 0; i < endPointsNeedUpdate.length; i++) {
                StatsService.StatsEndpointsEnum current = endPointsNeedUpdate[i];
                if (!fragmentsRefreshList.contains(current)) {
                    fragmentsRefreshList.add(current);
                }
            }
            mUpdateStatsHandler.removeCallbacks(mUpdateStatsRequestedRunnable);
            mUpdateStatsHandler.postDelayed(mUpdateStatsRequestedRunnable, 500);
        }
    }

    private final Runnable mUpdateStatsRequestedRunnable = new Runnable() {
        @Override
        public void run() {
            synchronized (fragmentsRefreshListSynchObj) {
                if (fragmentsRefreshList.contains(StatsService.StatsEndpointsEnum.VISITS)) {
                    // update stats with graph data
                    refreshStats(mCurrentTimeframe, mRequestedDate, true, true);
                } else {
                    refreshStats(mCurrentTimeframe, mRequestedDate, false, true);
                }
                mSwipeToRefreshHelper.setRefreshing(mIsUpdatingStats);
                fragmentsRefreshList.clear();
            }
        }
    };

    private void startWPComLoginActivity() {
        mResultCode = RESULT_CANCELED;
        Intent signInIntent = new Intent(this, SignInActivity.class);
        signInIntent.putExtra(SignInActivity.ARG_JETPACK_SITE_AUTH, mLocalBlogID);
        startActivityForResult(signInIntent, SignInActivity.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == SignInActivity.REQUEST_CODE) {
            mResultCode = resultCode;
            final Blog currentBlog = WordPress.getBlog(mLocalBlogID);
            if (resultCode == RESULT_OK && currentBlog != null && !currentBlog.isDotcomFlag()) {
                if (StatsUtils.getBlogId(mLocalBlogID) == null) {
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
                                AnalyticsTracker.refreshMetadata();
                                AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNED_INTO_JETPACK);
                                AnalyticsTracker.track(
                                        AnalyticsTracker.Stat.PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN);
                                if (!isFinishing()) {
                                    mSwipeToRefreshHelper.setRefreshing(true);
                                    refreshStats(StatsTimeframe.DAY, StatsUtils.getCurrentDateTZ(mLocalBlogID), true, true);
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
                    }, "wp.getOptions", params);
                } else {
                    refreshStats(mCurrentTimeframe, StatsUtils.getCurrentDateTZ(mLocalBlogID), true, true);
                }
                mSwipeToRefreshHelper.setRefreshing(true);
            }
        }
    }

    private class VerifyJetpackSettingsCallback implements ApiHelper.GenericCallback {
        @Override
        public void onSuccess() {
            if (isFinishing() || !mIsInFront) {
                return;
            }

            if (StatsUtils.getBlogId(mLocalBlogID) == null) {
                // Blog has not returned a jetpack_client_id
                stopStatsService();
                mSwipeToRefreshHelper.setRefreshing(false);
                showJetpackMissingAlert();
            }
        }

        @Override
        public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
            mSwipeToRefreshHelper.setRefreshing(false);
            if (isFinishing() || !mIsInFront) {
                return;
            }
            if (mSignInDialog != null && mSignInDialog.isShowing()) {
                return;
            }
            stopStatsService();
            Toast.makeText(StatsActivity.this, R.string.error_refresh_stats, Toast.LENGTH_LONG).show();
        }
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
                    // User cancelled the dialog
                }
            });
        } else {
            builder.setMessage(getString(R.string.jetpack_message_not_admin))
                    .setTitle(getString(R.string.jetpack_not_found));
            builder.setPositiveButton(R.string.yes, null);
        }
        builder.create().show();
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.stats, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_view_stats_full_site) {
            final String blogId = StatsUtils.getBlogId(mLocalBlogID);
            if (blogId == null) {
                showJetpackMissingAlert();
                return true;
            }

            String statsAuthenticatedUser = StatsUtils.getBlogStatsUsername(mLocalBlogID);
            if (statsAuthenticatedUser == null) {
                Toast.makeText(this, R.string.jetpack_message_not_admin, Toast.LENGTH_LONG).show();
                return true;
            }

            String addressToLoad = "https://wordpress.com/my-stats/?no-chrome&blog=" + blogId + "&unit=1";

            WPWebViewActivity.openUrlByUsingWPCOMCredentials(this, addressToLoad, statsAuthenticatedUser);
            AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_OPENED_WEB_VERSION);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scrollToTop() {
        ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_view_stats);
        if (scrollView != null) {
            scrollView.fullScroll(ScrollView.FOCUS_UP);
        }
    }

    @Override
    public void onBlogChanged() {
        stopStatsService();
        mLocalBlogID = WordPress.getCurrentBlog().getLocalTableBlogId();
        mCurrentTimeframe = StatsTimeframe.DAY;
        mRequestedDate = StatsUtils.getCurrentDateTZ(mLocalBlogID);
        selectCurrentTimeframeInActionBar();
        scrollToTop();
        mSwipeToRefreshHelper.setRefreshing(true);
        refreshStats(mCurrentTimeframe, mRequestedDate, true, true);
        loadStatsFragments(true, true, true);
    }

    // StatsVisitorsAndViewsFragment calls this when the user taps on a bar in the graph
    @Override
    public void onDateChanged(String blogID, StatsTimeframe timeframe, String date) {
        if (isFinishing() || isActivityDestroyed()) {
            return;
        }
        mRequestedDate = date;
        refreshStats(timeframe, date, false, false);
        emptyDataModelInFragments(false, false);
        loadStatsFragments(false, false, false); // This is here just for a security check
    }

    // Fragments call these two methods below to access the current timeframe/date selected by the user.
    @Override
    public String getCurrentDate() {
        return mRequestedDate;
    }

    @Override
    public StatsTimeframe getCurrentTimeFrame() {
        return mCurrentTimeframe;
    }

    private void refreshStats(StatsTimeframe timeframe, String date, boolean updateGraph, boolean updateAlltimeStats) {
        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);

        if (currentBlog == null) {
            AppLog.w(T.STATS, "Current blog is null. This should never happen here.");
            mSwipeToRefreshHelper.setRefreshing(false);
            return;
        }

        if (!NetworkUtils.checkConnection(this)) {
            mSwipeToRefreshHelper.setRefreshing(false);
            AppLog.w(AppLog.T.STATS, "StatsActivity > no connection, update canceled");
            return;
        }

        final String blogId = StatsUtils.getBlogId(mLocalBlogID);

        // Make sure the blogId is available.
        if (blogId != null) {
            // for self-hosted sites; launch the user into an activity where they can provide their credentials
            if (!currentBlog.isDotcomFlag()
                    && !currentBlog.hasValidJetpackCredentials() && mResultCode != RESULT_CANCELED) {
                if (WordPress.hasDotComToken(this)) {
                    // Let's try the global wpcom credentials them first
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                    String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
                    currentBlog.setDotcom_username(username);
                    WordPress.wpDB.saveBlog(currentBlog);
                    mSwipeToRefreshHelper.setRefreshing(true);
                } else {
                    startWPComLoginActivity();
                    return;
                }
            }
        } else {
            // blogId is null at this point.
            if (!currentBlog.isDotcomFlag()) {
                // Refresh blog settings/options that includes 'jetpack_client_id'needed here
                new ApiHelper.RefreshBlogContentTask(currentBlog,
                        new VerifyJetpackSettingsCallback()).execute(false);
            } else {
                // blodID cannot be null on dotcom blogs.
                Toast.makeText(this, R.string.error_refresh_stats, Toast.LENGTH_LONG).show();
                AppLog.e(T.STATS, "blogID is null for a wpcom blog!! " + currentBlog.getHomeURL());
            }
            return;
        }

        // check again that we've valid credentials for a Jetpack site
        if (!currentBlog.isDotcomFlag()
                && !currentBlog.hasValidJetpackCredentials()
                && !WordPress.hasDotComToken(this)) {
            mSwipeToRefreshHelper.setRefreshing(false);
            AppLog.w(T.STATS, "Jetpack blog with no wpcom credentials");
            return;
        }

        mIsUpdatingStats = true;

        // start service to get stats
        Intent intent = new Intent(this, StatsService.class);
        intent.putExtra(StatsService.ARG_BLOG_ID, blogId);
        intent.putExtra(StatsService.ARG_PERIOD, timeframe);
        intent.putExtra(StatsService.ARG_DATE, date);
        intent.putExtra(StatsService.ARG_UPDATE_ALLTIME_STATS, updateAlltimeStats);
        intent.putExtra(StatsService.ARG_UPDATE_GRAPH_STATS, updateGraph);
        startService(intent);
    }

    private void stopStatsService() {
        stopService(new Intent(this, StatsService.class));
        if (mIsUpdatingStats) {
            mIsUpdatingStats = false;
            mSwipeToRefreshHelper.setRefreshing(false);
        }
    }

    /*
     * receiver for broadcast from StatsService which alerts when stats update has started/ended
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = StringUtils.notNullStr(intent.getAction());

            if (action.equals(StatsService.ACTION_STATS_UPDATING)) {
                mIsUpdatingStats = intent.getBooleanExtra(StatsService.EXTRA_IS_UPDATING, false);
                mSwipeToRefreshHelper.setRefreshing(mIsUpdatingStats);

                // Check if there were errors
                if (intent.getBooleanExtra(StatsService.EXTRA_IS_ERROR, false) && !isFinishing()
                        && (mSignInDialog == null || !mSignInDialog.isShowing())) {
                    Serializable errorObject = intent.getSerializableExtra(StatsService.EXTRA_ERROR_OBJECT);
                    if (errorObject instanceof String && errorObject.toString().contains("unauthorized")
                            && errorObject.toString().contains("403")) {
                        // This site has the wrong WP.com credentials
                        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(
                                StatsActivity.this);
                        // Read the current wpcom username from blog settings, then read it from
                        // the app wpcom account.
                        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);
                        // If currentBlog is null at this point just exit.
                        if (currentBlog==null) {
                            return;
                        }
                        String username = StringUtils.notNullStr(currentBlog.getDotcom_username());
                        if (username.equals("")) {
                            username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, "");
                        }

                        AlertDialog.Builder builder = new AlertDialog.Builder(StatsActivity.this);
                        builder.setTitle(getString(R.string.jetpack_stats_unauthorized))
                        .setMessage(getString(R.string.jetpack_stats_switch_user, username));
                        builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                startWPComLoginActivity();
                            }
                        });
                        builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                // User cancelled the dialog
                            }
                        });
                        mSignInDialog = builder.create();
                        mSignInDialog.show();
                    } else if (errorObject instanceof VolleyError) {
                        AuthenticationDialogUtils.showToastOrAuthAlert(StatsActivity.this, (VolleyError) errorObject,
                                StatsActivity.this.getString(R.string.error_refresh_stats));
                    } else {
                        ToastUtils.showToast(StatsActivity.this,
                                StatsActivity.this.getString(R.string.error_refresh_stats),
                                Duration.LONG);
                    }
                } // End error check
            }
        }
    };

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
        private StatsTimeframe[] mTimeframes;
        private LayoutInflater mInflater;

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
                view = mInflater.inflate(R.layout.reader_spinner_item, parent, false);
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
                convertView = mInflater.inflate(R.layout.reader_spinner_dropdown_item, parent, false);
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
    public void onScrollChanged(ScrollViewExt scrollView, int x, int y, int oldx, int oldy) {
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

}
