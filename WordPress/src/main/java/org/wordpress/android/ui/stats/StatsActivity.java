package org.wordpress.android.ui.stats;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
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

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.WPWebViewActivity;
import org.wordpress.android.ui.accounts.SignInActivity;
import org.wordpress.android.models.AccountHelper;
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
public class StatsActivity extends ActionBarActivity
        implements ScrollViewExt.ScrollViewListener,
                StatsVisitorsAndViewsFragment.OnDateChangeListener {

    private static final String SAVED_NAV_POSITION = "SAVED_NAV_POSITION";
    private static final String SAVED_WP_LOGIN_STATE = "SAVED_WP_LOGIN_STATE";
    private static final String SAVED_STATS_TIMEFRAME = "SAVED_STATS_TIMEFRAME";
    private static final String SAVED_STATS_REQUESTED_DATE= "SAVED_STATS_REQUESTED_DATE";

    private Spinner mSpinner;

    private static final int REQUEST_JETPACK = 7000;

    public static final String ARG_LOCAL_TABLE_BLOG_ID = "ARG_LOCAL_TABLE_BLOG_ID";
    private int mNavPosition = 0;
    private Dialog mSignInDialog;

    private int mResultCode = -1;
    private boolean mIsInFront;
    private int mLocalBlogID = -1;
    private StatsTimeframe mCurrentTimeframe = StatsTimeframe.DAY;
    private String mRequestedDate;
    private boolean mIsUpdatingStats;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private TimeframeSpinnerAdapter mTimeframeSpinnerAdapter;

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

        setContentView(R.layout.stats_activity);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowTitleEnabled(false);
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

        // create the fragments without forcing the re-creation. If the activity is restarted fragments can already
        // be there, and ready to be displayed without making any network connections. A fragment calls the stats service
        // if its internal datamodel is empty.
        createFragments(false);

        ScrollViewExt scrollView = (ScrollViewExt) findViewById(R.id.scroll_view_stats);
        if (scrollView != null) {
            scrollView.setScrollViewListener(this);
        }

        if (mSpinner == null && toolbar != null) {
            View view = View.inflate(this, R.layout.toolbar_spinner, toolbar);
            mSpinner = (Spinner) view.findViewById(R.id.action_bar_spinner);

            StatsTimeframe[] timeframes = {StatsTimeframe.DAY, StatsTimeframe.WEEK,
                    StatsTimeframe.MONTH, StatsTimeframe.YEAR};
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
                    mRequestedDate = StatsUtils.getCurrentDateTZ(mLocalBlogID);
                    if (NetworkUtils.checkConnection(StatsActivity.this)) {
                        mSwipeToRefreshHelper.setRefreshing(true);
                    } else {
                        mSwipeToRefreshHelper.setRefreshing(false);
                    }
                    createFragments(true); // Need to recreate fragment here, since a new timeline was selected.
                }
                @Override
                public void onNothingSelected(AdapterView<?> parent) {
                    // nop
                }
            });
        }

        selectCurrentTimeframeInActionBar();
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
        outState.putInt(SAVED_NAV_POSITION, mNavPosition);
        outState.putInt(SAVED_WP_LOGIN_STATE, mResultCode);
        outState.putInt(ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        outState.putSerializable(SAVED_STATS_TIMEFRAME, mCurrentTimeframe);
        outState.putString(SAVED_STATS_REQUESTED_DATE, mRequestedDate);
        super.onSaveInstanceState(outState);
    }

    private void createFragments(boolean forceRecreationOfFragments) {
        if (isFinishing()) {
            return;
        }
        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        StatsAbstractFragment fragment;

        if (fm.findFragmentByTag(StatsVisitorsAndViewsFragment.TAG) == null || forceRecreationOfFragments) {
            fragment = StatsAbstractFragment.newInstance(StatsViewType.GRAPH_AND_SUMMARY, mLocalBlogID, mCurrentTimeframe, mRequestedDate);
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

        ft.commitAllowingStateLoss();
    }

    private void updateTimeframeAndDateAndStartRefreshOfFragments(boolean includeGraphAndTimelessFragments) {
        if (isFinishing()) {
            return;
        }
        FragmentManager fm = getFragmentManager();

        updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsTopPostsAndPagesFragment.TAG);
        updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsReferrersFragment.TAG);
        updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsClicksFragment.TAG);
        updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsGeoviewsFragment.TAG);
        updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsAuthorsFragment.TAG);
        updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsVideoplaysFragment.TAG);
        updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsSearchTermsFragment.TAG);

        if (includeGraphAndTimelessFragments) {
            updateTimeframeAndDateAndStartRefreshInFragment(fm, StatsVisitorsAndViewsFragment.TAG);
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
        signInIntent.putExtra(SignInActivity.ARG_JETPACK_SITE_AUTH, mLocalBlogID);
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
                                AnalyticsUtils.refreshMetadata();
                                AnalyticsTracker.track(AnalyticsTracker.Stat.SIGNED_INTO_JETPACK);
                                AnalyticsTracker.track(
                                        AnalyticsTracker.Stat.PERFORMED_JETPACK_SIGN_IN_FROM_STATS_SCREEN);
                                if (!isFinishing()) {
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
                    }, "wp.getOptions", params);
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
                    // User cancelled the dialog. Hide Stats.
                    finish();
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
        int i = item.getItemId();
        if (i == android.R.id.home) {
            onBackPressed();
            return true;
        } else if (i == R.id.menu_view_stats_full_site) {
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

    private boolean checkCredentials() {
        if (!NetworkUtils.isNetworkAvailable(this)) {
            AppLog.w(AppLog.T.STATS, "StatsActivity > cannot check credentials since no internet connection available");
            return false;
        }
        final String blogId = StatsUtils.getBlogId(mLocalBlogID);
        final Blog currentBlog = WordPress.getBlog(mLocalBlogID);

        if (currentBlog == null) {
            AppLog.e(T.STATS, "The blog with local_blog_id " + mLocalBlogID + " cannot be loaded from the DB.");
            return false;
        }

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

        // check again that we've valid credentials for a Jetpack site
        if (!currentBlog.isDotcomFlag() && !currentBlog.hasValidJetpackCredentials() &&
                !AccountHelper.isSignedInWordPressDotCom()) {
            mSwipeToRefreshHelper.setRefreshing(false);
            AppLog.w(T.STATS, "Jetpack blog with no wpcom credentials");
            return false;
        }

        return true;
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.UpdateStatusChanged event) {
        if (isFinishing() || !mIsInFront) {
            return;
        }
        mSwipeToRefreshHelper.setRefreshing(event.mUpdating);
        mIsUpdatingStats = event.mUpdating;
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.JetpackSettingsCompleted event) {
        if (isFinishing() || !mIsInFront) {
            return;
        }
        mSwipeToRefreshHelper.setRefreshing(false);

        if (!event.isError) {
            if (StatsUtils.getBlogId(mLocalBlogID) == null) {
                // Blog has not returned a jetpack_client_id
                showJetpackMissingAlert();
            } else {
                mSwipeToRefreshHelper.setRefreshing(true);
                checkCredentials();
            }
        } else {
            if (mSignInDialog != null && mSignInDialog.isShowing()) {
                return;
            }
            Toast.makeText(StatsActivity.this, R.string.error_refresh_stats, Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.JetpackAuthError event) {
        if (isFinishing() || !mIsInFront) {
            return;
        }

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
                view = mInflater.inflate(R.layout.toolbar_spinner_item, parent, false);
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

    private static final RateLimitedTask sTrackBottomReachedStats = new RateLimitedTask(2) {
        protected boolean run() {
            AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_SCROLLED_TO_BOTTOM);
            return true;
        }
    };
}
