package org.wordpress.android.ui.stats;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Point;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.ui.AuthenticatedWebViewActivity;
import org.wordpress.android.ui.PullToRefreshHelper;
import org.wordpress.android.ui.PullToRefreshHelper.RefreshListener;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.StringUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.Utils;
import org.xmlrpc.android.ApiHelper;
import org.xmlrpc.android.XMLRPCCallback;
import org.xmlrpc.android.XMLRPCClient;

import java.lang.ref.WeakReference;
import java.util.HashMap;
import java.util.Map;

import uk.co.senab.actionbarpulltorefresh.extras.actionbarsherlock.PullToRefreshLayout;

/**
 * The native stats activity, accessible via the menu drawer.
 * <p>
 * By pressing a spinner on the action bar, the user can select which stats view they wish to see.
 * </p>
 */
public class StatsActivity extends WPActionBarActivity {

    // Max number of rows to show in a stats fragment
    public static final int STATS_GROUP_MAX_ITEMS = 10;
    public static final int STATS_CHILD_MAX_ITEMS = 25;

    private static final String SAVED_NAV_POSITION = "SAVED_NAV_POSITION";
    private static final String SAVED_WP_LOGIN_STATE = "SAVED_WP_LOGIN_STATE";
    private static final int REQUEST_JETPACK = 7000;
    public static final String ARG_NO_MENU_DRAWER = "no_menu_drawer";

    private static final String KEY_VERIFIED_CREDS = "verified_creds";

    private Dialog mSignInDialog;
    private int mNavPosition = 0;

    private int mResultCode = -1;
    private boolean mIsRestoredFromState = false;
    private boolean mIsInFront;
    private boolean mNoMenuDrawer = false;
    private boolean mIsUpdatingStats;
    private boolean mHasVerifiedCreds;
    private PullToRefreshHelper mPullToRefreshHelper;

    // Used for tablet UI
    private static final int TABLET_720DP = 720;
    private static final int TABLET_600DP = 600;
    private LinearLayout mFragmentContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        mNoMenuDrawer = getIntent().getBooleanExtra(ARG_NO_MENU_DRAWER, false);
        if (mNoMenuDrawer) {
            setContentView(R.layout.stats_activity);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
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

        loadStatsFragments();
        setTitle(R.string.stats);

        restoreState(savedInstanceState);
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

        // for self-hosted sites; launch the user into an activity where they can provide their credentials
        if (WordPress.getCurrentBlog() != null && !WordPress.getCurrentBlog().isDotcomFlag() &&
                !WordPress.getCurrentBlog().hasValidJetpackCredentials() && mResultCode != RESULT_CANCELED) {
            if (WordPress.hasValidWPComCredentials(this)) {
                // Let's try the global wpcom credentials them first
                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
                String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, null);
                String password = WordPressDB.decryptPassword(settings.getString(WordPress.WPCOM_PASSWORD_PREFERENCE, null));
                WordPress.getCurrentBlog().setDotcom_username(username);
                WordPress.getCurrentBlog().setDotcom_password(password);
                WordPress.wpDB.saveBlog(WordPress.getCurrentBlog());
                mPullToRefreshHelper.setRefreshing(true);
                refreshStats();
            } else {
                startWPComLoginActivity();
            }
            return;
        }

        if (!mIsRestoredFromState) {
            mPullToRefreshHelper.setRefreshing(true);
            refreshStats();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        mIsInFront = false;
        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(mReceiver);
    }

    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;

        mNavPosition = savedInstanceState.getInt(SAVED_NAV_POSITION);
        mResultCode = savedInstanceState.getInt(SAVED_WP_LOGIN_STATE);
        mHasVerifiedCreds = savedInstanceState.getBoolean(KEY_VERIFIED_CREDS);
        mIsRestoredFromState = true;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt(SAVED_NAV_POSITION, mNavPosition);
        outState.putInt(SAVED_WP_LOGIN_STATE, mResultCode);
        outState.putBoolean(KEY_VERIFIED_CREDS, mHasVerifiedCreds);
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
            if (resultCode == RESULT_OK && !WordPress.getCurrentBlog().isDotcomFlag()) {
                if (getBlogId() == null) {
                    final Blog currentBlog = WordPress.getCurrentBlog();
                    // Attempt to get the Jetpack blog ID
                    XMLRPCClient xmlrpcClient = new XMLRPCClient(currentBlog.getUrl(), "", "");
                    Map<String, String> args = ApiHelper.blogOptionsXMLRPCParameters;
                    Object[] params = {
                            currentBlog.getRemoteBlogId(), currentBlog.getUsername(), currentBlog.getPassword(), args
                    };
                    xmlrpcClient.callAsync(new XMLRPCCallback() {
                        @Override
                        public void onSuccess(long id, Object result) {
                            if (result != null && ( result instanceof HashMap )) {
                                Map<?, ?> blogOptions = (HashMap<?, ?>) result;
                                ApiHelper.updateBlogOptions(currentBlog, blogOptions);
                                if (!isFinishing()) {
                                    mPullToRefreshHelper.setRefreshing(true);
                                    refreshStats();
                                }
                            }
                        }
                        @Override
                        public void onFailure(long id, Exception error) {
                            AppLog.e(T.STATS, "Cannot load blog options (wp.getOptions failed and no jetpack_client_id is then available", error);
                        }
                    }, "wp.getOptions", params);
                }
                mPullToRefreshHelper.setRefreshing(true);
                refreshStats();
            }
        }
    }

    private void loadStatsFragments() {

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        StatsAbsViewFragment fragment;

        if (fm.findFragmentByTag(StatsVisitorsAndViewsFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.VISITORS_AND_VIEWS);
            ft.replace(R.id.stats_visitors_and_views_container, fragment, StatsVisitorsAndViewsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsReferrersFragment.TAG) == null) {
            fragment = StatsReferrersFragment.newInstance(StatsViewType.REFERRERS);
            ft.replace(R.id.stats_referrers_container, fragment, StatsReferrersFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsClicksFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.CLICKS);
            ft.replace(R.id.stats_clicks_container, fragment, StatsClicksFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsGeoviewsFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.VIEWS_BY_COUNTRY);
            ft.replace(R.id.stats_geoviews_container, fragment, StatsGeoviewsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsSearchEngineTermsFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.SEARCH_ENGINE_TERMS);
            ft.replace(R.id.stats_searchengine_container, fragment, StatsSearchEngineTermsFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsTotalsFollowersAndSharesFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOTALS_FOLLOWERS_AND_SHARES);
            ft.replace(R.id.stats_totals_followers_shares_container, fragment, StatsTotalsFollowersAndSharesFragment.TAG);
        }

        if (fm.findFragmentByTag(StatsTopPostsAndPagesFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOP_POSTS_AND_PAGES);
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
        if (Utils.getSmallestWidthDP() >= TABLET_720DP || (Utils.getSmallestWidthDP() == TABLET_600DP && isInLandscape()))
            loadSplitLayout();
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

    private boolean isInLandscape() {
        if (android.os.Build.VERSION.SDK_INT >= 13) {
            Display display = getWindowManager().getDefaultDisplay();
            Point point = new Point();
            display.getSize(point);
            return (point.y < point.x);
        } else {
            return false;
        }
    }

    private class VerifyJetpackSettingsCallback implements ApiHelper.GenericCallback {
        private final WeakReference<StatsActivity> statsActivityWeakRef;

        public VerifyJetpackSettingsCallback(StatsActivity refActivity) {
            this.statsActivityWeakRef = new WeakReference<StatsActivity>(refActivity);
        }

        @Override
        public void onSuccess() {
            if (statsActivityWeakRef.get() == null || statsActivityWeakRef.get().isFinishing()
                    || !statsActivityWeakRef.get().mIsInFront) {
                return;
            }

            if (getBlogId() == null) {
                stopStatsService();
                // Blog has not returned a jetpack_client_id
                AlertDialog.Builder builder = new AlertDialog.Builder(this.statsActivityWeakRef.get());
                if (WordPress.getCurrentBlog().isAdmin()) {
                    builder.setMessage(getString(R.string.jetpack_message))
                    .setTitle(getString(R.string.jetpack_not_found));
                    builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int id) {
                            Intent jetpackIntent = new Intent(VerifyJetpackSettingsCallback.this.statsActivityWeakRef.get(), AuthenticatedWebViewActivity.class);
                            jetpackIntent.putExtra(AuthenticatedWebViewActivity.LOAD_AUTHENTICATED_URL, WordPress.getCurrentBlog().getAdminUrl()
                                    + "plugin-install.php?tab=search&s=jetpack+by+wordpress.com&plugin-search-input=Search+Plugins");
                            startActivityForResult(jetpackIntent, REQUEST_JETPACK);
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
        }

        @Override
        public void onFailure(ApiHelper.ErrorType errorType, String errorMessage, Throwable throwable) {
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.stats, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_view_stats_full_site) {
            startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("http://wordpress.com/my-stats")));
            return true;
        } else if (mNoMenuDrawer && item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void scrollToTop() {
        ScrollView scrollView = (ScrollView) findViewById(R.id.scroll_view_stats);
        if (scrollView != null)
            scrollView.fullScroll(ScrollView.FOCUS_UP);
    }

    @Override
    public void onBlogChanged() {
        super.onBlogChanged();

        stopStatsService();
        scrollToTop();
        mHasVerifiedCreds = false;

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        StatsAbsViewFragment fragment;

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.VISITORS_AND_VIEWS);
        ft.replace(R.id.stats_visitors_and_views_container, fragment, StatsVisitorsAndViewsFragment.TAG);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOP_POSTS_AND_PAGES);
        ft.replace(R.id.stats_top_posts_container, fragment, StatsTopPostsAndPagesFragment.TAG);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.VIEWS_BY_COUNTRY);
        ft.replace(R.id.stats_geoviews_container, fragment, StatsGeoviewsFragment.TAG);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.CLICKS);
        ft.replace(R.id.stats_clicks_container, fragment, StatsClicksFragment.TAG);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.SEARCH_ENGINE_TERMS);
        ft.replace(R.id.stats_searchengine_container, fragment, StatsSearchEngineTermsFragment.TAG);

        fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOTALS_FOLLOWERS_AND_SHARES);
        ft.replace(R.id.stats_totals_followers_shares_container, fragment, StatsTotalsFollowersAndSharesFragment.TAG);

        fragment = StatsReferrersFragment.newInstance(StatsViewType.REFERRERS);
        ft.replace(R.id.stats_referrers_container, fragment, StatsReferrersFragment.TAG);

        ft.commit();

        mPullToRefreshHelper.setRefreshing(true);
        refreshStats();
    }

    boolean dotComCredentialsMatch() {
        SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(this);
        String username = settings.getString(WordPress.WPCOM_USERNAME_PREFERENCE, "");
        return username.equals(WordPress.getCurrentBlog().getUsername());
    }

    private void refreshStats() {
        if (WordPress.getCurrentBlog() == null) {
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

        final String blogId;
        if (WordPress.getCurrentBlog().isDotcomFlag() && dotComCredentialsMatch()) {
            blogId = String.valueOf(WordPress.getCurrentBlog().getRemoteBlogId());
        } else {
            blogId = getBlogId();
            if (blogId == null) {
                //Refresh Jetpack Settings
                new ApiHelper.RefreshBlogContentTask(this, WordPress.getCurrentBlog(),
                        new VerifyJetpackSettingsCallback(StatsActivity.this)).execute(false);
                return;
            }
        }

        // verify the users credentials if it hasn't already been done
        if (!mHasVerifiedCreds) {
            verifyCredentials(blogId);
        }

        // start service to get stats
        Intent intent = new Intent(this, StatsService.class);
        intent.putExtra(StatsService.ARG_BLOG_ID, blogId);
        startService(intent);
    }

    private void verifyCredentials(final String blogId) {
        WordPress.getRestClientUtils().getStatsSummary(blogId,
                new RestRequest.Listener() {
                    @Override
                    public void onResponse(final JSONObject response) {
                        mHasVerifiedCreds = true;
                    }
                },
                new RestRequest.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if (isFinishing())
                            return;
                        if (mSignInDialog != null && mSignInDialog.isShowing()) {
                            return;
                        }
                        stopStatsService();

                        if (error.networkResponse != null && error.networkResponse.statusCode == 403) {
                            // This site has the wrong WP.com credentials
                            AlertDialog.Builder builder = new AlertDialog.Builder(StatsActivity.this);
                            builder.setTitle(getString(R.string.jetpack_stats_unauthorized))
                                    .setMessage(getString(R.string.jetpack_stats_switch_user));
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
                            return;
                        }

                        ToastUtils.showToastOrAuthAlert(StatsActivity.this, error,
                                StatsActivity.this.getString(R.string.error_refresh_stats));
                    }
                });
    }

    String getBlogId() {
        Blog currentBlog = WordPress.getCurrentBlog();
        // for dotcom blogs that were added manually
        if (currentBlog.isDotcomFlag() && !dotComCredentialsMatch()) {
            return String.valueOf(currentBlog.getRemoteBlogId());
        }
        // Can return null
        return currentBlog.getApi_blogid();
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
            }
        }
    };
}
