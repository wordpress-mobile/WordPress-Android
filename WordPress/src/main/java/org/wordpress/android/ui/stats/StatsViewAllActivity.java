package org.wordpress.android.ui.stats;

import android.app.ActionBar;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.Bundle;
import android.os.Handler;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.R;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.WPActionBarActivity;
import org.wordpress.android.ui.accounts.AbstractFragment;
import org.wordpress.android.ui.stats.model.AuthorsModel;
import org.wordpress.android.ui.stats.model.ClicksModel;
import org.wordpress.android.ui.stats.model.CommentFollowersModel;
import org.wordpress.android.ui.stats.model.CommentsModel;
import org.wordpress.android.ui.stats.model.FollowersModel;
import org.wordpress.android.ui.stats.model.GeoviewsModel;
import org.wordpress.android.ui.stats.model.PublicizeModel;
import org.wordpress.android.ui.stats.model.ReferrersModel;
import org.wordpress.android.ui.stats.model.SingleItemModel;
import org.wordpress.android.ui.stats.model.TagsContainerModel;
import org.wordpress.android.ui.stats.model.TagsModel;
import org.wordpress.android.ui.stats.model.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.model.VideoPlaysModel;
import org.wordpress.android.ui.stats.service.StatsService;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.NetworkUtils;
import org.wordpress.android.util.ToastUtils;
import org.wordpress.android.util.ptr.PullToRefreshHelper;

import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import uk.co.senab.actionbarpulltorefresh.library.PullToRefreshLayout;

/**
 *  Single item details activity.
 */
public class StatsViewAllActivity extends WPActionBarActivity
        implements StatsAuthorsFragment.OnAuthorsSectionChangeListener {

    private boolean mIsInFront;
    private boolean mIsUpdatingStats;
    private PullToRefreshHelper mPullToRefreshHelper;

    private final Handler mHandler = new Handler();

    private LinearLayout outerContainer;
    private StatsAbstractListFragment fragment;

    private int mLocalBlogID = -1;
    private StatsTimeframe mTimeframe;
    private StatsViewType mStatsViewType;
    private String mDate;
    private Serializable mRestResponse;
    private int mOuterPagerSelectedButtonIndex = -1;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.stats_activity_view_all);

        if (savedInstanceState == null) {
            // AnalyticsTracker.track(AnalyticsTracker.Stat.STATS_ACCESSED);
            // TODO: add analytics here
        }

        ActionBar actionBar = getActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        outerContainer = (LinearLayout) findViewById(R.id.stats_fragment_container);

        // pull to refresh setup
        mPullToRefreshHelper = new PullToRefreshHelper(this, (PullToRefreshLayout) findViewById(R.id.ptr_layout),
                new PullToRefreshHelper.RefreshListener() {
                    @Override
                    public void onRefreshStarted(View view) {
                        if (!NetworkUtils.checkConnection(getBaseContext())) {
                            mPullToRefreshHelper.setRefreshing(false);
                            return;
                        }
                        refreshStats();
                    }
                }
        );


        setTitle(getString(R.string.stats));

        if (savedInstanceState != null) {
            mLocalBlogID = savedInstanceState.getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, -1);
            mRestResponse = savedInstanceState.getSerializable(StatsAbstractFragment.ARG_REST_RESPONSE);
            mTimeframe = (StatsTimeframe) savedInstanceState.getSerializable(StatsAbstractFragment.ARGS_TIMEFRAME);
            mDate = savedInstanceState.getString(StatsAbstractFragment.ARGS_START_DATE);
            int ordinal = savedInstanceState.getInt(StatsAbstractFragment.ARGS_VIEW_TYPE, -1);
            mStatsViewType = StatsViewType.values()[ordinal];
            mOuterPagerSelectedButtonIndex = savedInstanceState.getInt(StatsAbstractListFragment.ARGS_OUTER_PAGER_SELECTED_BUTTON_INDEX, -1);
        } else if (getIntent() != null) {
            Bundle extras = getIntent().getExtras();
            mLocalBlogID = extras.getInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, -1);
            mTimeframe = (StatsTimeframe) extras.getSerializable(StatsAbstractFragment.ARGS_TIMEFRAME);
            mDate = extras.getString(StatsAbstractFragment.ARGS_START_DATE);
            mRestResponse = extras.getSerializable(StatsAbstractFragment.ARG_REST_RESPONSE);
            int ordinal = extras.getInt(StatsAbstractFragment.ARGS_VIEW_TYPE, -1);
            mStatsViewType = StatsViewType.values()[ordinal];
            mOuterPagerSelectedButtonIndex = extras.getInt(StatsAbstractListFragment.ARGS_OUTER_PAGER_SELECTED_BUTTON_INDEX, -1);
            refreshStats(); // refresh stats when launched for the first time
        }

        TextView dateTextView = (TextView) findViewById(R.id.stats_summary_date);
        dateTextView.setText(StatsUIHelper.getDateForDisplayInLabels(mDate, mTimeframe));

        FragmentManager fm = getFragmentManager();
        FragmentTransaction ft = fm.beginTransaction();

        if (fm.findFragmentByTag("ViewAll-"+getInnerFragmentTAG()) == null) {
            fragment = getInnerFragment();
            ft.replace(R.id.stats_single_view_fragment, fragment, "ViewAll-"+getInnerFragmentTAG());
            ft.commit();
        }
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
        }

        Bundle args = new Bundle();
        args.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        args.putInt(StatsAbstractFragment.ARGS_VIEW_TYPE, mStatsViewType.ordinal());
        args.putSerializable(StatsAbstractFragment.ARGS_TIMEFRAME, mTimeframe);
        args.putBoolean(StatsAbstractListFragment.ARGS_IS_SINGLE_VIEW, true); // Always true here
        args.putString(StatsAbstractFragment.ARGS_START_DATE, mDate);
        args.putInt(StatsAbstractListFragment.ARGS_OUTER_PAGER_SELECTED_BUTTON_INDEX, mOuterPagerSelectedButtonIndex);
        fragment.setArguments(args);
        return fragment;
    }

    private String getInnerFragmentTAG() {
        switch (mStatsViewType) {
            case TOP_POSTS_AND_PAGES:
                return StatsTopPostsAndPagesFragment.TAG;
            case REFERRERS:
                return StatsReferrersFragment.TAG;
            case CLICKS:
                return StatsClicksFragment.TAG;
            case GEOVIEWS:
                return StatsGeoviewsFragment.TAG;
            case AUTHORS:
                return StatsAuthorsFragment.TAG;
            case VIDEO_PLAYS:
                return StatsVideoplaysFragment.TAG;
            case COMMENTS:
                return StatsCommentsFragment.TAG;
            case TAGS_AND_CATEGORIES:
                return StatsTagsAndCategoriesFragment.TAG;
            case PUBLICIZE:
                return StatsPublicizeFragment.TAG;
            case FOLLOWERS:
                return StatsFollowersFragment.TAG;
        }
        return StatsAbstractFragment.TAG;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        outState.putInt(StatsActivity.ARG_LOCAL_TABLE_BLOG_ID, mLocalBlogID);
        outState.putSerializable(StatsAbstractFragment.ARG_REST_RESPONSE, mRestResponse);
        outState.putSerializable(StatsAbstractFragment.ARGS_TIMEFRAME, mTimeframe);
        outState.putString(StatsAbstractFragment.ARGS_START_DATE, mDate);
        outState.putInt(StatsAbstractFragment.ARGS_VIEW_TYPE, mStatsViewType.ordinal());
        outState.putInt(StatsAbstractListFragment.ARGS_OUTER_PAGER_SELECTED_BUTTON_INDEX, mOuterPagerSelectedButtonIndex);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mPullToRefreshHelper.registerReceiver(this);
        mIsInFront = true;

        if (mRestResponse == null) {
            refreshStats();
        } else {
            ThreadPoolExecutor parseResponseExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            parseResponseExecutor.submit(new Thread() {
                @Override
                public void run() {
                    notifySectionUpdated();
                }
            });
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mIsInFront = false;
        mPullToRefreshHelper.unregisterReceiver(this);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.stats_details, menu);
        return true;
    }

    private String getRestPath() {
        final String blogId = StatsUtils.getBlogId(mLocalBlogID);
        String endpointPath = "";
        switch (mStatsViewType) {
            case TOP_POSTS_AND_PAGES:
                endpointPath = "top-posts";
                break;
            case REFERRERS:
                endpointPath = "referrers";
                break;
            case CLICKS:
                endpointPath = "clicks";
                break;
            case GEOVIEWS:
                endpointPath = "country-views";
                break;
            case AUTHORS:
                endpointPath = "top-authors";
                break;
            case VIDEO_PLAYS:
                endpointPath = "video-plays";
                break;
            case COMMENTS:
                endpointPath = "comments";
                break;
            case TAGS_AND_CATEGORIES:
                endpointPath = "tags";
                break;
            case PUBLICIZE:
                endpointPath = "publicize";
                break;
            case FOLLOWERS:
                endpointPath = "followers";
                break;
        }

        return String.format("/sites/%s/stats/%s?period=%s&date=%s&max=%s", blogId, endpointPath,
                mTimeframe.getLabelForRestCall(), mDate, 100);
    }

    private StatsService.StatsEndpointsEnum getRestEndpointName() {
        switch (mStatsViewType) {
            case TOP_POSTS_AND_PAGES:
                return StatsService.StatsEndpointsEnum.TOP_POSTS;
            case REFERRERS:
                return StatsService.StatsEndpointsEnum.REFERRERS;
            case CLICKS:
                return StatsService.StatsEndpointsEnum.CLICKS;
            case GEOVIEWS:
                return StatsService.StatsEndpointsEnum.GEO_VIEWS;
            case AUTHORS:
                return StatsService.StatsEndpointsEnum.AUTHORS;
            case VIDEO_PLAYS:
                return StatsService.StatsEndpointsEnum.VIDEO_PLAYS;
            case COMMENTS:
                return StatsService.StatsEndpointsEnum.COMMENTS;
            case TAGS_AND_CATEGORIES:
                return StatsService.StatsEndpointsEnum.TAGS_AND_CATEGORIES;
            case PUBLICIZE:
                return StatsService.StatsEndpointsEnum.PUBLICIZE;
            case FOLLOWERS:
                return StatsService.StatsEndpointsEnum.COMMENT_FOLLOWERS;
        }
        return null;
    }

    private void refreshStats() {
       if (mIsUpdatingStats) {
            return;
        }
        mIsUpdatingStats = true;
        final RestClientUtils restClientUtils = WordPress.getRestClientUtilsV1_1();
        final String blogId = StatsUtils.getBlogId(mLocalBlogID);
        final String singlePostRestPath = getRestPath();
        AppLog.d(AppLog.T.STATS, "Enqueuing the following Stats request " + singlePostRestPath);
        RestListener vListener = new RestListener(this, getRestEndpointName(), blogId, mTimeframe);
        restClientUtils.get(singlePostRestPath, vListener, vListener);
        mPullToRefreshHelper.setRefreshing(true);
        return;
    }

    @Override
    public void onAuthorsVisibilityChange(boolean isEmpty) {
        // Nothing to do here, since the section must not disappear here.
    }

    private class RestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        private String mRequestBlogId;
        private final StatsTimeframe mTimeframe;
        private final StatsService.StatsEndpointsEnum mEndpointName;
        private final WeakReference<Activity> mActivityRef;

        public RestListener(Activity activity, StatsService.StatsEndpointsEnum endpointName, String blogId, StatsTimeframe timeframe) {
                mActivityRef = new WeakReference<Activity>(activity);
                mRequestBlogId = blogId;
                mTimeframe = timeframe;
                mEndpointName = endpointName;
        }

        @Override
        public void onResponse(final JSONObject response) {
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
                return;
            }
            mIsUpdatingStats = false;
            mPullToRefreshHelper.setRefreshing(false);
            // single background thread used to parse the response in BG.
            ThreadPoolExecutor parseResponseExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            parseResponseExecutor.submit(new Thread() {
                @Override
                public void run() {
                    if (response != null) {
                        try {
                            AppLog.d(AppLog.T.STATS, response.toString());
                            mRestResponse = parseResponse(response);
                        } catch (JSONException e) {
                            AppLog.e(AppLog.T.STATS, e);
                        } catch (RemoteException e) {
                            AppLog.e(AppLog.T.STATS, e);
                        } catch (OperationApplicationException e) {
                            AppLog.e(AppLog.T.STATS, e);
                        }
                    }
                    notifySectionUpdated();
                }
            });
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            if (volleyError != null) {
                AppLog.e(AppLog.T.STATS, "Error while reading Stats details "
                        + volleyError.getMessage(), volleyError);
            }
            if (mActivityRef.get() == null || mActivityRef.get().isFinishing()) {
                return;
            }
            resetModelVariables();
            ToastUtils.showToast(mActivityRef.get(),
                    mActivityRef.get().getString(R.string.error_refresh_stats),
                    ToastUtils.Duration.LONG);
            mIsUpdatingStats = false;
            mPullToRefreshHelper.setRefreshing(false);
            fragment.showEmptyUI(true); //FixME this could throws NPE
        }

        Serializable parseResponse(JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            Serializable model = null;
            switch (mEndpointName) {
                case TOP_POSTS:
                    model = new TopPostsAndPagesModel(mRequestBlogId, response);
                    break;
                case REFERRERS:
                    model = new ReferrersModel(mRequestBlogId, response);
                    break;
                case CLICKS:
                    model = new ClicksModel(mRequestBlogId, response);
                    break;
                case GEO_VIEWS:
                    model = new GeoviewsModel(mRequestBlogId, response);
                    break;
                case AUTHORS:
                    model = new AuthorsModel(mRequestBlogId, response);
                    break;
                case VIDEO_PLAYS:
                    model = new VideoPlaysModel(mRequestBlogId, response);
                    break;
                case COMMENTS:
                    model = new CommentsModel(mRequestBlogId, response);
                    break;
                case FOLLOWERS:
                    model = new FollowersModel(mRequestBlogId, response);
                    break;
                case COMMENT_FOLLOWERS:
                    model = new CommentFollowersModel(mRequestBlogId, response);
                    break;
                case TAGS_AND_CATEGORIES:
                    model = new TagsContainerModel(mRequestBlogId, response);
                    break;
                case PUBLICIZE:
                    model = new PublicizeModel(mRequestBlogId, response);
                    break;
            }
            return model;
        }
    }

    private void resetModelVariables() {
        mRestResponse = null;
    }

    private void notifySectionUpdated() {
        Intent intent = new Intent()
                .setAction(StatsService.ACTION_STATS_UPDATED)
                .putExtra(StatsService.EXTRA_ENDPOINT_NAME, getRestEndpointName())
                .putExtra(StatsService.EXTRA_ENDPOINT_DATA, mRestResponse);
        LocalBroadcastManager.getInstance(WordPress.getContext()).sendBroadcast(intent);
    }
}
