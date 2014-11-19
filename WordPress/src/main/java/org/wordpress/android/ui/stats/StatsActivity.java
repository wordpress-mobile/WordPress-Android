package org.wordpress.android.ui.stats;

import android.app.ActionBar;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.Fragment;
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
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
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
public class StatsActivity extends WPActionBarActivity implements ScrollViewExt.ScrollViewListener,
        ActionBar.OnNavigationListener, StatsAuthorsFragment.OnAuthorsSectionChangeListener {
    private static final String SAVED_NAV_POSITION = "SAVED_NAV_POSITION";
    private static final String SAVED_WP_LOGIN_STATE = "SAVED_WP_LOGIN_STATE";
    private static final String SAVED_STATS_TIMEFRAME = "SAVED_STATS_TIMEFRAME";

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
    private boolean mIsUpdatingStats;
    private PullToRefreshHelper mPullToRefreshHelper;
    private TimeframeSpinnerAdapter mTimeframeSpinnerAdapter;

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
        ActionBar actionBar = getActionBar();
        if (mNoMenuDrawer) {
            setContentView(R.layout.stats_activity);
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

                        if (mIsUpdatingStats) {
                            AppLog.w(T.STATS, "stats are already updating, refresh cancelled");
                            return;
                        }
                        refreshStats(mCurrentTimeframe, StatsUtils.getCurrentDateTZ(mLocalBlogID));
                    }
                });

        setTitle(R.string.stats);

        boolean needToRefreshStats = false;

        if (savedInstanceState != null) {
            mNavPosition = savedInstanceState.getInt(SAVED_NAV_POSITION);
            mResultCode = savedInstanceState.getInt(SAVED_WP_LOGIN_STATE);
            mLocalBlogID = savedInstanceState.getInt(ARG_LOCAL_TABLE_BLOG_ID);
            mCurrentTimeframe = (StatsTimeframe) savedInstanceState.getSerializable(SAVED_STATS_TIMEFRAME);
        } else if (getIntent() != null) {
            needToRefreshStats = true;
            mLocalBlogID = getIntent().getIntExtra(ARG_LOCAL_TABLE_BLOG_ID, -1);
            if (getIntent().hasExtra(SAVED_STATS_TIMEFRAME)) {
                mCurrentTimeframe = (StatsTimeframe) getIntent().getSerializableExtra(SAVED_STATS_TIMEFRAME);
            } else {
                mCurrentTimeframe = StatsTimeframe.DAY;
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

        loadStatsFragments(false);

        ScrollViewExt scrollView = (ScrollViewExt) findViewById(R.id.scroll_view_stats);
        if (scrollView != null) {
            scrollView.setScrollViewListener(this);
        }

        // only change if we're not in list navigation mode, since that means the actionBar
        // is already correctly configured
        if (!mNoMenuDrawer && actionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_LIST) {
            StatsTimeframe[] timeframes = {StatsTimeframe.DAY, StatsTimeframe.WEEK,
                    StatsTimeframe.MONTH, StatsTimeframe.YEAR};
            mTimeframeSpinnerAdapter =
                    new TimeframeSpinnerAdapter(this, timeframes);
            actionBar.setDisplayShowTitleEnabled(false);
            actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_LIST);
            actionBar.setListNavigationCallbacks(mTimeframeSpinnerAdapter, this);
        }

        selectTimeframeInActionBar(mCurrentTimeframe);

        // Refresh stats on new activity only.
        if (needToRefreshStats) {
            refreshStats(mCurrentTimeframe, StatsUtils.getCurrentDateTZ(mLocalBlogID));
            mPullToRefreshHelper.setRefreshing(true);
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
        outState.putSerializable(SAVED_STATS_TIMEFRAME, mCurrentTimeframe);
        super.onSaveInstanceState(outState);
    }

    private void loadStatsFragments(boolean force) {
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        StatsAbstractFragment fragment;

      /*  if (fm.findFragmentByTag(StatsDateSelectorFragment.TAG) == null) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.TIMEFRAME_SELECTOR, mLocalBlogID);
            ((StatsDateSelectorFragment)fragment).setTimeframeChangeListener(this);
            ft.replace(R.id.stats_timeframe_selector, fragment, StatsDateSelectorFragment.TAG);
        }
*/
        if (fm.findFragmentByTag(StatsVisitorsAndViewsFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.GRAPH_AND_SUMMARY, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_visitors_and_views_container, fragment, StatsVisitorsAndViewsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsTopPostsAndPagesFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.TOP_POSTS_AND_PAGES, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_top_posts_container, fragment, StatsTopPostsAndPagesFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsReferrersFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.REFERRERS, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_referrers_container, fragment, StatsReferrersFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsClicksFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.CLICKS, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_clicks_container, fragment, StatsClicksFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsGeoviewsFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.GEOVIEWS, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_geoviews_container, fragment, StatsGeoviewsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsAuthorsFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.AUTHORS, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_top_authors_container, fragment, StatsAuthorsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsVideoplaysFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.VIDEO_PLAYS, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_video_container, fragment, StatsVideoplaysFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsCommentsFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.COMMENTS, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_comments_container, fragment, StatsCommentsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsTagsAndCategoriesFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.TAGS_AND_CATEGORIES, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_tags_and_categories_container, fragment, StatsTagsAndCategoriesFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsPublicizeFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.PUBLICIZE, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_publicize_container, fragment, StatsPublicizeFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsFollowersFragment.TAG) == null || force) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.FOLLOWERS, mLocalBlogID, mCurrentTimeframe);
            ft.replace(R.id.stats_followers_container, fragment, StatsFollowersFragment.TAG);
        }

        ft.commit();
    }

    // AuthorsFragment should be dismissed when 0 or 1 author.
    public void onAuthorsVisibilityChange(boolean isEmpty) {
        View authorsContainer = this.findViewById(R.id.stats_top_authors_container);
        if (authorsContainer != null) {
            authorsContainer.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
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
                                    refreshStats(StatsTimeframe.DAY, StatsUtils.getCurrentDateTZ(mLocalBlogID));
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
                    refreshStats(StatsTimeframe.DAY, StatsUtils.getCurrentDateTZ(mLocalBlogID));
                }
                mPullToRefreshHelper.setRefreshing(true);
            }
        }
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
        stopStatsService();
        mLocalBlogID = WordPress.getCurrentBlog().getLocalTableBlogId();
        mCurrentTimeframe = StatsTimeframe.DAY;
        selectTimeframeInActionBar(mCurrentTimeframe);
        scrollToTop();
        loadStatsFragments(true);
        mPullToRefreshHelper.setRefreshing(true);
        refreshStats(mCurrentTimeframe, StatsUtils.getCurrentDateTZ(mLocalBlogID));
    }

    /**
     * Do not refresh Stats in BG when user switch between blogs since the refresh
     * is already started in foreground at this point.
     */
    @Override
    protected boolean shouldUpdateCurrentBlogStatsInBackground() {
        return false;
    }

    private void refreshStats(StatsTimeframe timeframe, String date) {
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
                new ApiHelper.RefreshBlogContentTask(currentBlog,
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
        intent.putExtra(StatsService.ARG_PERIOD, timeframe);
        intent.putExtra(StatsService.ARG_DATE, date);
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
                mPullToRefreshHelper.setRefreshing(mIsUpdatingStats);

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
    private void selectTimeframeInActionBar(final StatsTimeframe timeframe) {
        ActionBar actionBar = getActionBar();
        if (actionBar == null) {
            return;
        }

        if (mTimeframeSpinnerAdapter == null) {
            return;
        }

        int position = mTimeframeSpinnerAdapter.getIndexOfTimeframe(mCurrentTimeframe);
        if (position == -1 || position == actionBar.getSelectedNavigationIndex()) {
            return;
        }

        if (actionBar.getNavigationMode() != ActionBar.NAVIGATION_MODE_LIST) {
            AppLog.w(T.STATS, "stats activity  > unexpected ActionBar navigation mode");
            return;
        }

        actionBar.setSelectedNavigationItem(position);
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
                view = mInflater.inflate(R.layout.spinner_menu_dropdown_item, parent, false);
            } else {
                view = convertView;
            }

            final TextView text = (TextView) view.findViewById(R.id.menu_text_dropdown);
            StatsTimeframe selectedTimeframe = (StatsTimeframe)getItem(position);
            text.setText(selectedTimeframe.getLabel());
            return view;
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

    /*
      * called when user selects a timeframe from the ActionBar dropdown
      */
    @Override
    public boolean onNavigationItemSelected(int itemPosition, long itemId) {
        final StatsTimeframe selectedTimeframe =  (StatsTimeframe) mTimeframeSpinnerAdapter.getItem(itemPosition);

        if (mCurrentTimeframe == selectedTimeframe) {
            AppLog.d(T.STATS, "The selected TIME FRAME is already active: " + selectedTimeframe.getLabel());
            return true;
        }

        AppLog.d(T.STATS, "NEW TIME FRAME : " + selectedTimeframe.getLabel());
        mCurrentTimeframe = selectedTimeframe;
        if (NetworkUtils.isNetworkAvailable(this)) {
            loadStatsFragments(true);
            refreshStats(selectedTimeframe, StatsUtils.getCurrentDateTZ(mLocalBlogID));
            mPullToRefreshHelper.setRefreshing(true);
        }

        return true;
    }

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
