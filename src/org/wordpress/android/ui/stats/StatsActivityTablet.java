package org.wordpress.android.ui.stats;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Point;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Display;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuInflater;
import com.actionbarsherlock.view.MenuItem;

import org.json.JSONException;
import org.json.JSONObject;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.ui.OldStatsActivity;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.util.StatsRestHelper;
import org.wordpress.android.util.Utils;

/**
 * The native stats activity, accessible via the menu drawer.
 * This activity is for tablet layout, see {@link StatsActivity} for the phone version. 
 * <p>
 * For tablets with a min width of 720DP or 600DP and landscape, the layout is split into two.
 * Otherwise the layout is a long list of stats views
 * </p>
 */
public class StatsActivityTablet extends WPActionBarActivity {

    private static final int TABLET_720DP = 720;
    private static final int TABLET_600DP = 600;
    private static final String SAVED_WP_LOGIN_STATE = "SAVED_WP_LOGIN_STATE";
    private LinearLayout mFragmentContainer;
    private LinearLayout mColumnLeft;
    private LinearLayout mColumnRight;
    private int mResultCode = -1;
    private boolean mIsRestoredFromState = false;

    private MenuItem mRefreshMenuItem;
    
    private BroadcastReceiver mReceiver = new BroadcastReceiver() {
        
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(StatsRestHelper.REFRESH_VIEW_TYPE)) {
                
                if (mRefreshMenuItem == null)
                    return;
                
                boolean started = intent.getBooleanExtra(StatsRestHelper.REFRESH_VIEW_TYPE_STARTED, false);
                if (started)
                    startAnimatingRefreshButton(mRefreshMenuItem);
                else
                    stopAnimatingRefreshButton(mRefreshMenuItem);
            }
        }
    };
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        

        if (WordPress.wpDB == null) {
            Toast.makeText(this, R.string.fatal_db_error, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        createMenuDrawer(R.layout.stats_activity_tablet);
        
        restoreState(savedInstanceState);
        
        mFragmentContainer = (LinearLayout) findViewById(R.id.stats_fragment_container);
        mColumnLeft = (LinearLayout) findViewById(R.id.stats_tablet_col_left);
        mColumnRight = (LinearLayout) findViewById(R.id.stats_tablet_col_right); 
        
        loadStatsFragments();
        
        setTitle(R.string.stats);
    }
    
    private void restoreState(Bundle savedInstanceState) {
        if (savedInstanceState == null)
            return;
        
        mResultCode = savedInstanceState.getInt(SAVED_WP_LOGIN_STATE);
    }
    
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SAVED_WP_LOGIN_STATE, mResultCode);
    }

    private void loadStatsFragments() {

        FragmentManager fm = getSupportFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        
        StatsAbsViewFragment fragment;
        
        // TODO: lines commented out are awaiting stats apis

        if (fm.findFragmentByTag(StatsVisitorsAndViewsFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.VISITORS_AND_VIEWS);
            ft.replace(R.id.stats_visitors_and_views_container, fragment, StatsVisitorsAndViewsFragment.TAG);
        }
        
        if (fm.findFragmentByTag(StatsClicksFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.CLICKS);
            ft.replace(R.id.stats_clicks_container, fragment, StatsClicksFragment.TAG);
        }
        
//        if (fm.findFragmentByTag(StatsCommentsFragment.TAG) == null) {
//            fragment = StatsAbsViewFragment.newInstance(StatsViewType.COMMENTS);
//            ft.replace(R.id.stats_comments_container, fragment, StatsCommentsFragment.TAG);
//        }

//        if (fm.findFragmentByTag(StatsGeoviewsFragment.TAG) == null) {
//            fragment = StatsAbsViewFragment.newInstance(StatsViewType.VIEWS_BY_COUNTRY);
//            ft.replace(R.id.stats_geoviews_container, fragment, StatsGeoviewsFragment.TAG);
//        }

        if (fm.findFragmentByTag(StatsSearchEngineTermsFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.SEARCH_ENGINE_TERMS);
            ft.replace(R.id.stats_searchengine_container, fragment, StatsSearchEngineTermsFragment.TAG);
        }

//        if (fm.findFragmentByTag(StatsTagsAndCategoriesFragment.TAG) == null) {
//            fragment = StatsAbsViewFragment.newInstance(StatsViewType.TAGS_AND_CATEGORIES);
//            ft.replace(R.id.stats_tags_and_categories_container, fragment, StatsTagsAndCategoriesFragment.TAG);
//        }

//        if (fm.findFragmentByTag(StatsTopAuthorsFragment.TAG) == null) {
//            fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOP_AUTHORS);
//            ft.replace(R.id.stats_top_authors_container, fragment, StatsTopAuthorsFragment.TAG);
//        }

        if (fm.findFragmentByTag(StatsTotalsFollowersAndSharesFragment.TAG) == null) {
            fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOTALS_FOLLOWERS_AND_SHARES);
            ft.replace(R.id.stats_totals_followers_shares_container, fragment, StatsTotalsFollowersAndSharesFragment.TAG);
        }

//        if (fm.findFragmentByTag(StatsTopPostsAndPagesFragment.TAG) == null) {
//            fragment = StatsAbsViewFragment.newInstance(StatsViewType.TOP_POSTS_AND_PAGES);
//            ft.replace(R.id.stats_top_posts_container, fragment, StatsTopPostsAndPagesFragment.TAG);
//        }

//        if (fm.findFragmentByTag(StatsVideoFragment.TAG) == null) {
//            fragment = StatsAbsViewFragment.newInstance(StatsViewType.VIDEO_PLAYS);
//            ft.replace(R.id.stats_video_container, fragment, StatsVideoFragment.TAG);
//        }

        if (fm.findFragmentByTag(StatsReferrersFragment.TAG) == null) {
            fragment = StatsReferrersFragment.newInstance(StatsViewType.REFERRERS);
            ft.replace(R.id.stats_referrers_container, fragment, StatsReferrersFragment.TAG);
        }
        
        ft.commit();
        
        // split layout into two for 720DP tablets and 600DP tablets in landscape
        if (Utils.getSmallestWidthDP() >= TABLET_720DP || (Utils.getSmallestWidthDP() == TABLET_600DP && isInLandscape()))
            loadSplitLayout();
        
    }

    private boolean isInLandscape() {
        Display display = getWindowManager().getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        if (point.y < point.x) {
            return true;
        } else {
            return false;
        }
    }

    private void loadSplitLayout() {
        FrameLayout frameView;

        // TODO: lines commented out are awaiting stats apis

        frameView = (FrameLayout) findViewById(R.id.stats_geoviews_container);
        mFragmentContainer.removeView(frameView);
        mColumnLeft.addView(frameView);

        frameView = (FrameLayout) findViewById(R.id.stats_totals_followers_shares_container);
        mFragmentContainer.removeView(frameView);
        mColumnLeft.addView(frameView);

        frameView = (FrameLayout) findViewById(R.id.stats_referrers_container);
        mFragmentContainer.removeView(frameView);
        mColumnLeft.addView(frameView);

//        frameView = (FrameLayout) findViewById(R.id.stats_top_authors_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnLeft.addView(frameView);

//        frameView = (FrameLayout) findViewById(R.id.stats_video_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnLeft.addView(frameView);

//        frameView = (FrameLayout) findViewById(R.id.stats_top_posts_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnRight.addView(frameView);

//        frameView = (FrameLayout) findViewById(R.id.stats_comments_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnRight.addView(frameView);
        
        frameView = (FrameLayout) findViewById(R.id.stats_clicks_container);
        mFragmentContainer.removeView(frameView);
        mColumnRight.addView(frameView);

//        frameView = (FrameLayout) findViewById(R.id.stats_tags_and_categories_container);
//        mFragmentContainer.removeView(frameView);
//        mColumnRight.addView(frameView);

        frameView = (FrameLayout) findViewById(R.id.stats_searchengine_container);
        mFragmentContainer.removeView(frameView);
        mColumnRight.addView(frameView);
        
    }
    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getSupportMenuInflater();
        inflater.inflate(R.menu.stats, menu);
        mRefreshMenuItem = menu.findItem(R.id.menu_refresh);
        return true;
    }
    
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (WordPress.hasValidWPComCredentials(this))
            menu.findItem(R.id.menu_view_stats_login).setVisible(false);
        else
            menu.findItem(R.id.menu_view_stats_login).setVisible(true);
        
        // TODO what if credentials are incorrect?
        
        return super.onPrepareOptionsMenu(menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_refresh) {
            refreshStats();
            return true;
        } else if (item.getItemId() == R.id.menu_view_stats_full_site) {
            Intent intent = new Intent(this, OldStatsActivity.class);
            intent.putExtra("id", WordPress.currentBlog.getId());
            intent.putExtra("isNew", true);
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);
            startActivityWithDelay(intent);
            finish();
            overridePendingTransition(0, 0);
            return true;
        } else if (item.getItemId() == R.id.menu_view_stats_login) {
            startWPComLoginActivity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onPause() {
        super.onPause();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.unregisterReceiver(mReceiver);
        
        stopAnimatingRefreshButton(mRefreshMenuItem);
    }
    
    
    @Override
    protected void onResume() {
        super.onResume();

        LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(this);
        lbm.registerReceiver(mReceiver, new IntentFilter(StatsRestHelper.REFRESH_VIEW_TYPE));

        if (!WordPress.hasValidWPComCredentials(this) && mResultCode != RESULT_CANCELED) {
            startWPComLoginActivity();
        }

        if (!mIsRestoredFromState)
            refreshStats();
    }
    
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        mIsRestoredFromState = true;
    }
    
    private void startWPComLoginActivity() {
        mResultCode = RESULT_CANCELED;
        startActivityForResult(new Intent(this, WPComLoginActivity.class), WPComLoginActivity.REQUEST_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == WPComLoginActivity.REQUEST_CODE) {
            
            mResultCode = resultCode;
            
            if (resultCode == RESULT_OK)
                refreshStats();
        }
    }
    
    @Override
    public void onBlogChanged() {
        super.onBlogChanged();
        refreshStats();
    }

    private void refreshStats() {
        if (WordPress.getCurrentBlog() == null)
            return; 

        String blogId = null;
        
        if (WordPress.getCurrentBlog().isDotcomFlag())
            blogId = String.valueOf(WordPress.getCurrentBlog().getBlogId());
        else  
            blogId = getBlogIdFromJetPack();

        // TODO: lines commented out are awaiting stats apis
        StatsRestHelper.getStatsSummary(blogId);
        StatsRestHelper.getStats(StatsViewType.CLICKS, blogId);
//        StatsRestHelper.getStats(StatsViewType.COMMENTS, blogId);
        StatsRestHelper.getStats(StatsViewType.REFERRERS, blogId);
        StatsRestHelper.getStats(StatsViewType.SEARCH_ENGINE_TERMS, blogId);
//        StatsRestHelper.getStats(StatsViewType.TAGS_AND_CATEGORIES, blogId);
        // data for total followers and shares will already be fetched 
//        StatsRestHelper.getStats(StatsViewType.TOP_AUTHORS, blogId);
        StatsRestHelper.getStats(StatsViewType.TOP_POSTS_AND_PAGES, blogId);
//        StatsRestHelper.getStats(StatsViewType.VIDEO_PLAYS, blogId);
//        StatsRestHelper.getStats(StatsViewType.VIEWS_BY_COUNTRY, blogId);
        StatsRestHelper.getStats(StatsViewType.VISITORS_AND_VIEWS, blogId);
    }

    private String getBlogIdFromJetPack() {
        // for self-hosted sites
        
        try {
            JSONObject options = new JSONObject(WordPress.getCurrentBlog().getBlogOptions());
            return options.getJSONObject("jetpack_client_id").getString("value");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return null;
    }
}
