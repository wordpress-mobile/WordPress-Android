package org.wordpress.android.ui.stats;

import android.app.ActionBar;
import android.app.Activity;
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
import android.support.v4.view.GestureDetectorCompat;
import android.view.GestureDetector;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.android.volley.VolleyError;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.accounts.WPComLoginActivity;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.AuthenticationDialogUtils;
import org.wordpress.android.util.RateLimitedTask;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ToastUtils.Duration;
import org.wordpress.android.util.ptr.PullToRefreshHelper;
import org.wordpress.android.util.ptr.PullToRefreshHelper.RefreshListener;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClientInterface;
import org.xmlrpc.android.XMLRPCFactory;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

/**
 * The native stats activity, accessible via the menu drawer.
 * <p>
 * By pressing a spinner on the action bar, the user can select which stats view they wish to see.
 * </p>
 */
public class StatsActivity extends WPActionBarActivity implements ScrollViewExt.ScrollViewListener {
    private static final String SAVED_NAV_POSITION = "SAVED_NAV_POSITION";
    private static final String SAVED_WP_LOGIN_STATE = "SAVED_WP_LOGIN_STATE";
    private static final int REQUEST_JETPACK = 7000;

    public static final String ARG_NO_MENU_DRAWER = "no_menu_drawer";
    public static final String ARG_LOCAL_TABLE_BLOG_ID = "ARG_LOCAL_TABLE_BLOG_ID";

    public static final String STATS_GESTURE_SHOW_TAP = "STATS_SHOW_TAP";
    public static final String STATS_GESTURE_SINGLE_TAP_CONFIRMED = "STATS_SINGLE_TAP_CONFIRMED";
    public static final String STATS_GESTURE_OTHER = "STATS_GESTURE_OTHER";
    public static final String STATS_DETAILS_DATE = "STATS_DETAILS_DATE";
    private GestureDetectorCompat mDetector;

    private Dialog mSignInDialog;
    private int mNavPosition = 0;

    private int mResultCode = -1;
    private boolean mIsInFront;
    private boolean mNoMenuDrawer = false;
    private int mLocalBlogID = -1;
    private boolean mIsUpdatingStats;
    private PullToRefreshHelper mPullToRefreshHelper;

    private LinearLayout mFragmentContainer;

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
        if (mNoMenuDrawer) {
            setContentView(R.layout.stats_activity);
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setDisplayHomeAsUpEnabled(true);
            }
        } else {
            createMenuDrawer(R.layout.stats_activity);
        }

        mFragmentContainer = (LinearLayout) findViewById(R.id.stats_fragment_container);

        // pull to refresh setup
        mPullToRefreshHelper = new PullToRefreshHelper(this, (PullToRefreshLayout) findViewById(R.id.ptr_layout),
                new RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mPullToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        refreshStats();
                    }
                });

        setTitle(R.string.stats);

        if (savedInstanceState != null) {
            mNavPosition = savedInstanceState.getInt(SAVED_NAV_POSITION);
            mResultCode = savedInstanceState.getInt(SAVED_WP_LOGIN_STATE);
            mLocalBlogID = savedInstanceState.getInt(ARG_LOCAL_TABLE_BLOG_ID);
        } else if (getIntent() != null) {
            mLocalBlogID = getIntent().getIntExtra(ARG_LOCAL_TABLE_BLOG_ID, -1);
        }

        //Make sure the blog_id passed to this activity is valid and the blog is available within the app
        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);

        if (currentBlog == null) {
            AppLog.e(T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
            Toast.makeText(this, R.string.stats_no_blog, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        loadStatsFragments();

        mDetector = new GestureDetectorCompat(this, new MyGestureListener());
        mDetector.setIsLongpressEnabled(false);

        // Refresh stats at startup if network and not on configuration changed
        if (NetworkUtils.isNetworkAvailable(this) && savedInstanceState == null) {
            refreshStats();
            mPullToRefreshHelper.setRefreshing(true);
        }

        ScrollViewExt scrollView = (ScrollViewExt) findViewById(R.id.scroll_view_stats);
        if (scrollView != null) {
            scrollView.setScrollViewListener(this);
        }
    }

    @Override
    protected void onDestroy() {
        stopStatsService();
        super.onDestroy();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPullToRefreshHelper.registerReceiver(this);
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
        mPullToRefreshHelper.setRefreshing(false);
        mPullToRefreshHelper.unregisterReceiver(this);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(SAVED_NAV_POSITION, mNavPosition);
        outState.putInt(SAVED_WP_LOGIN_STATE, mResultCode);
        outState.putInt(ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        super.onSaveInstanceState(outState);
    }

    class MyGestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onDown(MotionEvent event) {
            return true;
        }
        @Override
        public boolean onSingleTapUp(MotionEvent event) {
            WordPress.sendLocalBroadcast(StatsActivity.this, STATS_GESTURE_SINGLE_TAP_CONFIRMED);
            return false;
        }
        @Override
        public void onShowPress(MotionEvent e) {
            WordPress.sendLocalBroadcast(StatsActivity.this, STATS_GESTURE_SHOW_TAP);
        }

        @Override
        public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
            WordPress.sendLocalBroadcast(StatsActivity.this, STATS_GESTURE_OTHER);
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            WordPress.sendLocalBroadcast(StatsActivity.this, STATS_GESTURE_OTHER);
            return false;
        }
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent event) {
        this.mDetector.onTouchEvent(event);
        return super.dispatchTouchEvent(event);
    }

    private void startWPComLoginActivity() {
        mResultCode = RESULT_CANCELED;
        Intent loginIntent = new Intent(this, WPComLoginActivity.class);
        loginIntent.putExtra(WPComLoginActivity.JETPACK_AUTH_REQUEST, true);
        startActivityForResult(loginIntent, WPComLoginActivity.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WPComLoginActivity.REQUEST_CODE) {
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
                                    mPullToRefreshHelper.setRefreshing(true);
                                    refreshStats();
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
                                    mPullToRefreshHelper.setRefreshing(false);
                                    ToastUtils.showToast(StatsActivity.this,
                                            StatsActivity.this.getString(R.string.error_refresh_stats),
                                            Duration.LONG);
                                }
                            });
                        }
                    }, "wp.getOptions", params);
                } else {
                    refreshStats();
                }
                mPullToRefreshHelper.setRefreshing(true);
            }
        }
    }

    private void loadStatsFragments() {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        StatsAbsViewFragment fragment;

        if (fm.findFragmentByTag(StatsVisitorsAndViewsFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.VISITORS_AND_VIEWS, mLocalBlogID);
            ft.replace(R.id.stats_visitors_and_views_container, fragment, StatsVisitorsAndViewsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsReferrersFragment.TAG) == null) {
            fragment = StatsReferrersFragment.newInstance(StatsViewType.REFERRERS, mLocalBlogID);
            ft.replace(R.id.stats_referrers_container, fragment, StatsReferrersFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsClicksFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.CLICKS, mLocalBlogID);
            ft.replace(R.id.stats_clicks_container, fragment, StatsClicksFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsGeoviewsFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.VIEWS_BY_COUNTRY, mLocalBlogID);
            ft.replace(R.id.stats_geoviews_container, fragment, StatsGeoviewsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsSearchEngineTermsFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.SEARCH_ENGINE_TERMS, mLocalBlogID);
            ft.replace(R.id.stats_searchengine_container, fragment, StatsSearchEngineTermsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsTotalsFollowersAndSharesFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOTALS_FOLLOWERS_AND_SHARES, mLocalBlogID);
            ft.replace(R.id.stats_totals_followers_shares_container,
                    fragment, StatsTotalsFollowersAndSharesFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsTopPostsAndPagesFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOP_POSTS_AND_PAGES, mLocalBlogID);
            ft.replace(R.id.stats_top_posts_container, fragment, StatsTopPostsAndPagesFragment.TAG);
        }

        // TODO: awaiting stats APIs
        /*if (fm.findFragmentByTag(StatsVideoFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.VIDEO_PLAYS);
            ft.replace(R.id.stats_video_container, fragment, StatsVideoFragment.TAG);
        }
        if (fm.findFragmentByTag(StatsTagsAndCategoriesFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.TAGS_AND_CATEGORIES);
            ft.replace(R.id.stats_tags_and_categories_container, fragment, StatsTagsAndCategoriesFragment.TAG);
        }
        if (fm.findFragmentByTag(StatsTopAuthorsFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOP_AUTHORS);
            ft.replace(R.id.stats_top_authors_container, fragment, StatsTopAuthorsFragment.TAG);
        }
        if (fm.findFragmentByTag(StatsCommentsFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.COMMENTS);
            ft.replace(R.id.stats_comments_container, fragment, StatsCommentsFragment.TAG);
        }*/

        ft.commit();

        // split layout into two for 720DP tablets and 600DP tablets in landscape
        if (StatsUIHelper.shouldLoadSplitLayout(this)) {
            loadSplitLayout();
        }
    }

    private void loadSplitLayout() {
        LinearLayout columnLeft = (LinearLayout) findViewById(R.id.stats_tablet_col_left);
        LinearLayout columnRight = (LinearLayout) findViewById(R.id.stats_tablet_col_right);
        FrameLayout frameView;

        /*
         * left column
         */
        frameView = (FrameLayout) findViewById(R.id.stats_top_posts_container);
        mFragmentContainer.removeView(frameView);
        columnLeft.addView(frameView);

        frameView = (FrameLayout) findViewById(R.id.stats_referrers_container);
        mFragmentContainer.removeView(frameView);
        columnLeft.addView(frameView);

        frameView = (FrameLayout) findViewById(R.id.stats_clicks_container);
        mFragmentContainer.removeView(frameView);
        columnLeft.addView(frameView);

        /*
         * right column
         */
        frameView = (FrameLayout) findViewById(R.id.stats_geoviews_container);
        mFragmentContainer.removeView(frameView);
        columnRight.addView(frameView);

        frameView = (FrameLayout) findViewById(R.id.stats_searchengine_container);
        mFragmentContainer.removeView(frameView);
        columnRight.addView(frameView);

        frameView = (FrameLayout) findViewById(R.id.stats_totals_followers_shares_container);
        mFragmentContainer.removeView(frameView);
        columnRight.addView(frameView);

        // TODO: awaiting stats APIs
        /*frameView = (FrameLayout) findViewById(R.id.stats_top_authors_container);
        mFragmentContainer.removeView(frameView);
        columnLeft.addView(frameView);

        frameView = (FrameLayout) findViewById(R.id.stats_video_container);
        mFragmentContainer.removeView(frameView);
        columnLeft.addView(frameView);

        frameView = (FrameLayout) findViewById(R.id.stats_comments_container);
        mFragmentContainer.removeView(frameView);
        columnRight.addView(frameView);

        frameView = (FrameLayout) findViewById(R.id.stats_tags_and_categories_container);
        mFragmentContainer.removeView(frameView);
        columnRight.addView(frameView);*/
    }

    private class VerifyJetpackSettingsCallback implements ApiHelper.GenericCallback {
        private final WeakReference<StatsActivity> mStatsActivityWeakRef;

        public VerifyJetpackSettingsCallback(StatsActivity refActivity) {
            this.mStatsActivityWeakRef = new WeakReference<StatsActivity>(refActivity);
        }

        @Override
        public void onSuccess() {
            if (mStatsActivityWeakRef.get() == null || mStatsActivityWeakRef.get().isFinishing()
                    || !mStatsActivityWeakRef.get().mIsInFront) {
                return;
            }

            if (StatsUtils.getBlogId(mLocalBlogID) == null) {
                // Blog has not returned a jetpack_client_id
                stopStatsService();
                mPullToRefreshHelper.setRefreshing(false);
                showJetpackMissingAlert(this.mStatsActivityWeakRef.get());
            }
        }

        @Override
        public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
            mPullToRefreshHelper.setRefreshing(false);
            if (mStatsActivityWeakRef.get() == null || mStatsActivityWeakRef.get().isFinishing()
                    || !mStatsActivityWeakRef.get().mIsInFront) {
                return;
            }
            if (mSignInDialog != null && mSignInDialog.isShowing()) {
                return;
            }
            stopStatsService();
            Toast.makeText(mStatsActivityWeakRef.get(), R.string.error_refresh_stats, Toast.LENGTH_LONG).show();
        }
    }

    private void showJetpackMissingAlert(final Activity currentActivity) {
        AlertDialog.Builder builder = new AlertDialog.Builder(currentActivity);
        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);
        if (currentBlog == null && !isFinishing()) {
            AppLog.e(T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
            Toast.makeText(this, R.string.stats_no_blog, Toast.LENGTH_LONG).show();
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
                    Intent jetpackIntent = new Intent(currentActivity, WPWebViewActivity.class);
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
                showJetpackMissingAlert(this);
                return true;
            }

            StatsUtils.StatsCredentials credentials = StatsUtils.getBlogStatsCredentials(mLocalBlogID);
            if (credentials == null) {
                Toast.makeText(this, R.string.jetpack_message_not_admin, Toast.LENGTH_LONG).show();
                return true;
            }

            String statsAuthenticatedUser = credentials.getUsername();
            String statsAuthenticatedPassword =  credentials.getPassword();
            String addressToLoad = "https://wordpress.com/my-stats/?no-chrome&blog=" + blogId + "&unit=1";

            WPWebViewActivity.openUrlByUsingWPCOMCredentials(this, addressToLoad, statsAuthenticatedUser,
                    statsAuthenticatedPassword);
            AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_OPENED_WEB_VERSION);
            return true;
        } else if (mNoMenuDrawer && item.getItemId() == android.R.id.home) {
            onBackPressed();
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
        super.onBlogChanged();

        mLocalBlogID = WordPress.getCurrentBlog().getLocalTableBlogId();

        stopStatsService();
        scrollToTop();

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        StatsAbsViewFragment fragment;

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.VISITORS_AND_VIEWS, mLocalBlogID);
        ft.replace(R.id.stats_visitors_and_views_container, fragment, StatsVisitorsAndViewsFragment.TAG);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOP_POSTS_AND_PAGES, mLocalBlogID);
        ft.replace(R.id.stats_top_posts_container, fragment, StatsTopPostsAndPagesFragment.TAG);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.VIEWS_BY_COUNTRY, mLocalBlogID);
        ft.replace(R.id.stats_geoviews_container, fragment, StatsGeoviewsFragment.TAG);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.CLICKS, mLocalBlogID);
        ft.replace(R.id.stats_clicks_container, fragment, StatsClicksFragment.TAG);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.SEARCH_ENGINE_TERMS, mLocalBlogID);
        ft.replace(R.id.stats_searchengine_container, fragment, StatsSearchEngineTermsFragment.TAG);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOTALS_FOLLOWERS_AND_SHARES, mLocalBlogID);
        ft.replace(R.id.stats_totals_followers_shares_container, fragment, StatsTotalsFollowersAndSharesFragment.TAG);

        fragment = StatsReferrersFragment.newInstance(StatsViewType.REFERRERS, mLocalBlogID);
        ft.replace(R.id.stats_referrers_container, fragment, StatsReferrersFragment.TAG);

        ft.commit();

        mPullToRefreshHelper.setRefreshing(true);
        refreshStats();
    }

    /**
     * Do not refresh Stats in BG when user switch between blogs since the refresh
     * is already started in foreground at this point.
     */
    @Override
    protected boolean shouldUpdateCurrentBlogStatsInBackground() {
        return false;
    }

    private void refreshStats() {
        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);

        if (currentBlog == null) {
            AppLog.w(T.STATS, "Current blog is null. This should never happen here.");
            mPullToRefreshHelper.setRefreshing(false);
            return;
        }

        if (!NetworkUtils.isNetworkAvailable(this)) {
            mPullToRefreshHelper.setRefreshing(false);
            return;
        }

        if (mIsUpdatingStats) {
            mPullToRefreshHelper.setRefreshing(false);
            AppLog.w(T.STATS, "stats are already updating, refresh cancelled");
            return;
        }

        final String blogId = StatsUtils.getBlogId(mLocalBlogID);

        // Make sure the blogId is available.
        if (blogId != null) {
            // for self-hosted sites; launch the user into an activity where they can provide their credentials
            if (!currentBlog.isDotcomFlag()
                    && !currentBlog.hasValidJetpackCredentials() && mResultCode != RESULT_CANCELED) {
                if (WordPress.hasValidWPComCredentials(this)) {
                    // Let's try the global wpcom credentials them first
                    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                    String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
                    String password = WordPressDB.decryptPassword(
                            settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null)
                            );
                    currentBlog.setDotcom_username(username);
                    currentBlog.setDotcom_password(password);
                    WordPress.wpDB.saveBlog(currentBlog);
                    mPullToRefreshHelper.setRefreshing(true);
                } else {
                    startWPComLoginActivity();
                    return;
                }
            }
        } else {
            // blogId is null at this point.
            if (!currentBlog.isDotcomFlag()) {
                // Refresh blog settings/options that includes 'jetpack_client_id'needed here
                new ApiHelper.RefreshBlogContentTask(this, currentBlog,
                        new VerifyJetpackSettingsCallback(StatsActivity.this)).execute(false);
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
                && !WordPress.hasValidWPComCredentials(this)) {
            mPullToRefreshHelper.setRefreshing(false);
            AppLog.w(T.STATS, "Jetpack blog with no wpcom credentials");
            return;
        }

        // start service to get stats
        Intent intent = new Intent(this, StatsService.class);
        intent.putExtra(StatsService.ARG_BLOG_ID, blogId);
        startService(intent);
    }

    private void stopStatsService() {
        stopService(new Intent(this, StatsService.class));
        if (mIsUpdatingStats) {
            mIsUpdatingStats = false;
            mPullToRefreshHelper.setRefreshing(false);
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
                if (!mIsUpdatingStats) {
                    mPullToRefreshHelper.setRefreshing(false);
                }

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

    @Override
    public void onScrollChanged(ScrollViewExt scrollView, int x, int y, int oldx, int oldy) {
        // We take the last son in the scrollview
        View view = (View) scrollView.getChildAt(scrollView.getChildCount() - 1);
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
