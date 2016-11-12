package org.wordpress.android.ui.stats;

import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.analytics.AnalyticsTracker;
import org.wordpress.android.ui.ActivityId;
import org.wordpress.android.util.AnalyticsUtils;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.helpers.SwipeToRefreshHelper;
import org.wordpress.android.util.widgets.CustomSwipeRefreshLayout;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import de.greenrobot.event.EventBus;

/**
 *  Single item details activity.
 */
public class StatsViewAllActivity extends AppCompatActivity {

    public static final String ARG_STATS_VIEW_ALL_TITLE = "arg_stats_view_all_title";
    private static final String SAVED_STATS_SCROLL_POSITION = "SAVED_STATS_SCROLL_POSITION";

    private boolean mIsInFront;
    private boolean mIsUpdatingStats;
    private SwipeToRefreshHelper mSwipeToRefreshHelper;
    private ScrollViewExt mOuterScrollView;

    private StatsAbstractListFragment mFragment;

    private int mLocalBlogID = -1;
    private StatsTimeframe mTimeframe;
    private StatsViewType mStatsViewType;
    private String mDate;
    private Serializable[] mRestResponse;
    private int mOuterPagerSelectedButtonIndex = 0;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.stats_activity_view_all);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        setTitle(R.string.stats);

        mOuterScrollView = (ScrollViewExt) findViewById(R.id.scroll_view_stats);

        // pull to refresh setup
        mSwipeToRefreshHelper = new SwipeToRefreshHelper(this, (CustomSwipeRefreshLayout) findViewById(R.id.ptr_layout),
                new SwipeToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted() {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mSwipeToRefreshHelper.setRefreshing(false);
                            mIsUpdatingStats = false;
                            return;
                        }

                        if (mIsUpdatingStats) {
                            AppLog.w(AppLog.T.STATS, "stats are already updating, refresh cancelled");
                            return;
                        }

                        refreshStats();
                    }
                }
        );

        if (savedInstanceState != null) {
            mLocalBlogID = savedInstanceState.getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, -1);
            Serializable oldData = savedInstanceState.getSerializable(StatsAbstractFragment.ARG_REST_RESPONSE);
            if (oldData != null && oldData instanceof Serializable[]) {
                mRestResponse = (Serializable[]) oldData;
            }
            mTimeframe = (StatsTimeframe) savedInstanceState.getSerializable(StatsAbstractFragment.ARGS_TIMEFRAME);
            mDate = savedInstanceState.getString(StatsAbstractFragment.ARGS_SELECTED_DATE);
            mStatsViewType = (StatsViewType) savedInstanceState.getSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE);
            mOuterPagerSelectedButtonIndex = savedInstanceState.getInt(StatsAbstractListFragment.ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, 0);
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
            Bundle extras = getIntent().getExtras();
            mLocalBlogID = extras.getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, -1);
            mTimeframe = (StatsTimeframe) extras.getSerializable(StatsAbstractFragment.ARGS_TIMEFRAME);
            mDate = extras.getString(StatsAbstractFragment.ARGS_SELECTED_DATE);
            mStatsViewType = (StatsViewType) extras.getSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE);
            mOuterPagerSelectedButtonIndex = extras.getInt(StatsAbstractListFragment.ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, 0);

            // Set a custom activity title if one was passed
            if (!TextUtils.isEmpty(extras.getString(ARG_STATS_VIEW_ALL_TITLE))) {
                setTitle(extras.getString(ARG_STATS_VIEW_ALL_TITLE));
            }
        }

        if (mStatsViewType == null || mTimeframe == null || mDate == null) {
            Toast.makeText(this, getResources().getText(R.string.stats_generic_error),
                    Toast.LENGTH_SHORT).show();
            finish();
        }

        // Setup the top date label. It's available on those fragments that are affected by the top date selector.
        TextView dateTextView = (TextView) findViewById(R.id.stats_summary_date);
        switch (mStatsViewType) {
            case TOP_POSTS_AND_PAGES:
            case REFERRERS:
            case CLICKS:
            case GEOVIEWS:
            case AUTHORS:
            case VIDEO_PLAYS:
            case SEARCH_TERMS:
                dateTextView.setText(getDateForDisplayInLabels(mDate, mTimeframe));
                dateTextView.setVisibility(View.VISIBLE);
                break;
            default:
                dateTextView.setVisibility(View.GONE);
                break;
        }

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();
        mFragment = (StatsAbstractListFragment) fm.findFragmentByTag("ViewAll-Fragment");
        if (mFragment == null) {
            mFragment = getInnerFragment();
            ft.replace(R.id.stats_single_view_fragment, mFragment, "ViewAll-Fragment");
            ft.commitAllowingStateLoss();
        }

        if (savedInstanceState == null) {
            AnalyticsUtils.trackWithBlogDetails(AnalyticsTracker.Stat.STATS_VIEW_ALL_ACCESSED, WordPress.getBlog(mLocalBlogID));
        }
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

    @SuppressWarnings("unused")
    public void onEventMainThread(StatsEvents.UpdateStatusChanged event) {
        if (isFinishing() || !mIsInFront) {
            return;
        }
        mSwipeToRefreshHelper.setRefreshing(event.mUpdating);
        mIsUpdatingStats = event.mUpdating;
    }

    private String getDateForDisplayInLabels(String date, StatsTimeframe timeframe) {
        String prefix = getString(R.string.stats_for);
        switch (timeframe) {
            case DAY:
                return String.format(prefix, StatsUtils.parseDate(date, StatsConstants.STATS_INPUT_DATE_FORMAT,
                        StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_SHORT_FORMAT));
            case WEEK:
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat(StatsConstants.STATS_INPUT_DATE_FORMAT);
                    final Date parsedDate = sdf.parse(date);
                    Calendar c = Calendar.getInstance();
                    c.setTime(parsedDate);
                    String  endDateLabel = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_LONG_FORMAT);
                    // last day of this week
                    c.add(Calendar.DAY_OF_WEEK, - 6);
                    String startDateLabel = StatsUtils.msToString(c.getTimeInMillis(), StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_DAY_LONG_FORMAT);
                    return String.format(prefix,  startDateLabel + " - " + endDateLabel);
                } catch (ParseException e) {
                    AppLog.e(AppLog.T.UTILS, e);
                    return "";
                }
            case MONTH:
                return String.format(prefix, StatsUtils.parseDate(date, StatsConstants.STATS_INPUT_DATE_FORMAT, StatsConstants.STATS_OUTPUT_DATE_MONTH_LONG_FORMAT));
            case YEAR:
                return String.format(prefix, StatsUtils.parseDate(date, StatsConstants.STATS_INPUT_DATE_FORMAT, StatsConstants.STATS_OUTPUT_DATE_YEAR_FORMAT));
        }
        return "";
    }

    private StatsAbstractListFragment getInnerFragment() {
        StatsAbstractListFragment fragment = null;
        switch (mStatsViewType) {
            case TOP_POSTS_AND_PAGES:
                fragment = new StatsTopPostsAndPagesFragment();
                break;
            case REFERRERS:
                fragment = new StatsReferrersFragment();
                break;
            case CLICKS:
                fragment = new StatsClicksFragment();
                break;
            case GEOVIEWS:
                fragment = new StatsGeoviewsFragment();
                break;
            case AUTHORS:
                fragment = new StatsAuthorsFragment();
                break;
            case VIDEO_PLAYS:
                fragment = new StatsVideoplaysFragment();
                break;
            case COMMENTS:
                fragment = new StatsCommentsFragment();
                break;
            case TAGS_AND_CATEGORIES:
                fragment = new StatsTagsAndCategoriesFragment();
                break;
            case PUBLICIZE:
                fragment = new StatsPublicizeFragment();
                break;
            case FOLLOWERS:
                fragment = new StatsFollowersFragment();
                break;
            case SEARCH_TERMS:
                fragment = new StatsSearchTermsFragment();
                break;
        }

        fragment.setTimeframe(mTimeframe);
        fragment.setDate(mDate);

        Bundle args = new Bundle();
        args.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        args.putSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE, mStatsViewType);
        args.putBoolean(StatsAbstractListFragment.ARGS_IS_SINGLE_VIEW, true); // Always true here
        args.putInt(StatsAbstractListFragment.ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, mOuterPagerSelectedButtonIndex);
        args.putSerializable(StatsAbstractFragment.ARG_REST_RESPONSE, mRestResponse);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        outState.putSerializable(StatsAbstractFragment.ARG_REST_RESPONSE, mRestResponse);
        outState.putSerializable(StatsAbstractFragment.ARGS_TIMEFRAME, mTimeframe);
        outState.putString(StatsAbstractFragment.ARGS_SELECTED_DATE, mDate);
        outState.putSerializable(StatsAbstractFragment.ARGS_VIEW_TYPE, mStatsViewType);
        outState.putInt(StatsAbstractListFragment.ARGS_TOP_PAGER_SELECTED_BUTTON_INDEX, mOuterPagerSelectedButtonIndex);
        if (mOuterScrollView.getScrollY() != 0) {
            outState.putInt(SAVED_STATS_SCROLL_POSITION, mOuterScrollView.getScrollY());
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mIsInFront = true;
        NetworkUtils.checkConnection(this); // show the error toast if the network is offline
        ActivityId.trackLastActivity(ActivityId.STATS_VIEW_ALL);
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsInFront = false;
        mIsUpdatingStats = false;
        mSwipeToRefreshHelper.setRefreshing(false);
    }

    @Override
    public boolean onOptionsItemSelected(final MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void refreshStats() {
        if (mIsUpdatingStats) {
            return;
        }
        if (!NetworkUtils.isNetworkAvailable(this)) {
            mSwipeToRefreshHelper.setRefreshing(false);
            AppLog.w(AppLog.T.STATS, "ViewAll on "+ mFragment.getTag() + " > no connection, update canceled");
            return;
        }

        if (mFragment != null) {
            mFragment.refreshStats();
        }
    }
}
