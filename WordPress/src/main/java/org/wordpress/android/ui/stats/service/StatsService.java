package org.wordpress.android.ui.stats.service;

import android.app.Service;
import android.content.Intent;
import android.content.OperationApplicationException;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.content.LocalBroadcastManager;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.stats.StatsTimeframe;
import org.wordpress.android.ui.stats.StatsUtils;
import org.wordpress.android.ui.stats.model.AuthorsModel;
import org.wordpress.android.ui.stats.model.ClicksModel;
import org.wordpress.android.ui.stats.model.CommentFollowersModel;
import org.wordpress.android.ui.stats.model.CommentsModel;
import org.wordpress.android.ui.stats.model.FollowersModel;
import org.wordpress.android.ui.stats.model.GeoviewsModel;
import org.wordpress.android.ui.stats.model.PublicizeModel;
import org.wordpress.android.ui.stats.model.ReferrersModel;
import org.wordpress.android.ui.stats.model.TagsContainerModel;
import org.wordpress.android.ui.stats.model.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.model.VideoPlaysModel;
import org.wordpress.android.ui.stats.model.VisitsModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;
import org.wordpress.android.util.StringUtils;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * Background service to retrieve Stats.
 * Parsing of response(s) is done by using a ThreadPoolExecutor with a single thread.
 */

public class StatsService extends Service {
    public static final String ARG_BLOG_ID = "blog_id";
    public static final String ARG_PERIOD = "stats_period";
    public static final String ARG_DATE = "stats_date";
    public static final String ARG_UPDATE_GRAPH = "stats_update_graph";
    public static enum StatsEndpointsEnum {VISITS, TOP_POSTS, REFERRERS, CLICKS, GEO_VIEWS, AUTHORS,
        VIDEO_PLAYS, COMMENTS, FOLLOWERS_WPCOM, FOLLOWERS_EMAIL, COMMENT_FOLLOWERS, TAGS_AND_CATEGORIES, PUBLICIZE}

    // broadcast action to notify clients of update start/end
    public static final String ACTION_STATS_UPDATING = "ACTION_STATS_UPDATING";
    public static final String ACTION_STATS_SECTION_UPDATED = "ACTION_STATS_SECTION_UPDATED";
    public static final String EXTRA_IS_UPDATING = "is-updating";
    public static final String EXTRA_ENDPOINT_NAME = "updated-endpoint-name";
    public static final String EXTRA_ENDPOINT_DATA = "updated-endpoint-data";

    public static final String EXTRA_IS_ERROR = "is-error";
    public static final String EXTRA_ERROR_OBJECT = "error-object";

    private String mServiceBlogId;
    private StatsTimeframe mServiceRequestedTimeframe;
    private String mServiceRequestedDate;
    private int mServiceStartId;
    private final LinkedList<Request<JSONObject>> statsNetworkRequests = new LinkedList<Request<JSONObject>>();
    private int numberOfNetworkCalls = 0; // The number of networks calls made by Stats.
    private int numberOfFinishedNetworkCalls = 0; // The number of networks calls made by Stats.
    protected ThreadPoolExecutor parseResponseExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(T.STATS, "service created");
    }

    @Override
    public void onDestroy() {
        stopRefresh();
        AppLog.i(T.STATS, "service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            AppLog.d(T.STATS, "StatsService was killed and restarted with a null intent.");
            // if this service's process is killed while it is started (after returning from onStartCommand(Intent, int, int)),
            // then leave it in the started state but don't retain this delivered intent.
            // Later the system will try to re-create the service.
            // Because it is in the started state, it will guarantee to call onStartCommand(Intent, int, int) after creating the new service instance;
            // if there are not any pending start commands to be delivered to the service, it will be called with a null intent object.
            stopRefresh();
            return START_NOT_STICKY;
        }

        final String blogId = StringUtils.notNullStr(intent.getStringExtra(ARG_BLOG_ID));

        StatsTimeframe period = StatsTimeframe.DAY;
        if (intent.hasExtra(ARG_PERIOD)) {
            period = (StatsTimeframe) intent.getSerializableExtra(ARG_PERIOD);
        }

        String requestedDate = intent.getStringExtra(ARG_DATE);
        if (requestedDate == null) {
            AppLog.w(T.STATS, "StatsService is started with a NULL date on this blogID - "
                    + mServiceBlogId + ". Using current date!!!");
            int localTableBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId(
                    Integer.parseInt(mServiceBlogId));
            requestedDate = StatsUtils.getCurrentDateTZ(localTableBlogId);
        }

        // True when the network call to update the graph is needed
        boolean updateGraph = intent.getBooleanExtra(ARG_UPDATE_GRAPH, true);

        if (mServiceBlogId == null) {
            startTasks(blogId, period, requestedDate, updateGraph, startId);
        } else if (blogId.equals(mServiceBlogId) && mServiceRequestedTimeframe == period
                && requestedDate.equals(mServiceRequestedDate)) {
            // already running on the same blogID, same period
            // Do nothing
            AppLog.i(T.STATS, "StatsService is already running on this blogID - " + mServiceBlogId
                    + " for the same period and the same date.");
        } else {
            // stats is running on a different blogID
            stopRefresh();
            startTasks(blogId, period, requestedDate, updateGraph, startId);
        }
        // Always update the startId. Always.
        this.mServiceStartId = startId;

        return START_NOT_STICKY;
    }

    private void stopRefresh() {
        this.mServiceBlogId = null;
        this.mServiceRequestedTimeframe = StatsTimeframe.DAY;
        this.mServiceRequestedDate = null;
        this.mServiceStartId = 0;
        for (Request<JSONObject> req : statsNetworkRequests) {
            if (req != null && !req.hasHadResponseDelivered() && !req.isCanceled()) {
                req.cancel();
            }
        }
        statsNetworkRequests.clear();
        numberOfFinishedNetworkCalls = 0;
        numberOfNetworkCalls = 0;
    }

    private void startTasks(final String blogId, final StatsTimeframe timeframe, final String date,
                            final boolean updateGraph, final int startId) {
        this.mServiceBlogId = blogId;
        this.mServiceRequestedTimeframe = timeframe;
        this.mServiceRequestedDate = date;
        this.mServiceStartId = startId;

        new Thread() {
            @Override
            public void run() {
                final RestClientUtils restClientUtils = WordPress.getRestClientUtilsV1_1();

                String period = timeframe.getLabelForRestCall();

                AppLog.i(T.STATS, "Update started for blogID - " + blogId + " with the following period: " + period
                + " on the following date: " + mServiceRequestedDate);

                broadcastUpdate(true);

                /* Summary call
                SummaryCallListener sListener = new SummaryCallListener(mServiceBlogId, mServiceRequestedTimeframe);
                final String summaryPath = String.format("/sites/%s/stats/summary?period=%s&date=%s", mServiceBlogId, period, mServiceRequestedDate);
                statsNetworkRequests.add(restClientUtils.get(summaryPath, sListener, sListener));
*/
                if (updateGraph) {
                    // Visits call: The Graph and the section just below the graph
                    RestListener vListener = new RestListener(StatsEndpointsEnum.VISITS, mServiceBlogId, mServiceRequestedTimeframe);
                    final String visitsPath = String.format("/sites/%s/stats/visits?unit=%s&quantity=10&date=%s", mServiceBlogId, period, mServiceRequestedDate);
                    statsNetworkRequests.add(restClientUtils.get(visitsPath, vListener, vListener));
                }

               // Posts & Pages
                RestListener topPostsAndPagesListener = new RestListener(StatsEndpointsEnum.TOP_POSTS, mServiceBlogId, mServiceRequestedTimeframe);
                final String topPostsAndPagesPath = String.format("/sites/%s/stats/top-posts?period=%s&date=%s&max=%s", mServiceBlogId, period, mServiceRequestedDate, 12);
                statsNetworkRequests.add(restClientUtils.get(topPostsAndPagesPath, topPostsAndPagesListener, topPostsAndPagesListener));

                // Referrers
                RestListener referrersListener = new RestListener(StatsEndpointsEnum.REFERRERS, mServiceBlogId, mServiceRequestedTimeframe);
                final String referrersPath = String.format("/sites/%s/stats/referrers?period=%s&date=%s&max=%s", mServiceBlogId, period, mServiceRequestedDate, 12);
                statsNetworkRequests.add(restClientUtils.get(referrersPath, referrersListener, referrersListener));

                // Clicks
                RestListener clicksListener = new RestListener(StatsEndpointsEnum.CLICKS, mServiceBlogId, mServiceRequestedTimeframe);
                final String clicksPath = String.format("/sites/%s/stats/clicks?period=%s&date=%s&max=%s", mServiceBlogId, period, mServiceRequestedDate, 12);
                statsNetworkRequests.add(restClientUtils.get(clicksPath, clicksListener, clicksListener));

                // Geoviews
                RestListener countriesListener = new RestListener(StatsEndpointsEnum.GEO_VIEWS, mServiceBlogId, mServiceRequestedTimeframe);
                final String countriesPath = String.format("/sites/%s/stats/country-views?period=%s&date=%s&max=%s", mServiceBlogId, period, mServiceRequestedDate, 12);
                statsNetworkRequests.add(restClientUtils.get(countriesPath, countriesListener, countriesListener));

                // Authors
                RestListener authorsListener = new RestListener(StatsEndpointsEnum.AUTHORS, mServiceBlogId, mServiceRequestedTimeframe);
                final String authorsPath = String.format("/sites/%s/stats/top-authors?period=%s&date=%s&max=%s", mServiceBlogId, period, mServiceRequestedDate, 12);
                statsNetworkRequests.add(restClientUtils.get(authorsPath, authorsListener, authorsListener));

                // Video plays
                RestListener videoPlaysListener = new RestListener(StatsEndpointsEnum.VIDEO_PLAYS, mServiceBlogId, mServiceRequestedTimeframe);
                final String videoPlaysPath = String.format("/sites/%s/stats/video-plays?period=%s&date=%s&max=%s", mServiceBlogId, period, mServiceRequestedDate, 12);
                statsNetworkRequests.add(restClientUtils.get(videoPlaysPath, videoPlaysListener, videoPlaysListener));

                // Comments
                RestListener commentsListener = new RestListener(StatsEndpointsEnum.COMMENTS, mServiceBlogId, mServiceRequestedTimeframe);
                final String commentsPath = String.format("/sites/%s/stats/comments", mServiceBlogId); // No max parameter available
                statsNetworkRequests.add(restClientUtils.get(commentsPath, commentsListener, commentsListener));

                // Comments Followers
                RestListener commentFollowersListener = new RestListener(StatsEndpointsEnum.COMMENT_FOLLOWERS, mServiceBlogId, mServiceRequestedTimeframe);
                final String commentFollowersPath = String.format("/sites/%s/stats/comment-followers?max=%s", mServiceBlogId, 12);
                statsNetworkRequests.add(restClientUtils.get(commentFollowersPath, commentFollowersListener, commentFollowersListener));

                // Followers WPCOM
                RestListener followersListener = new RestListener(StatsEndpointsEnum.FOLLOWERS_WPCOM, mServiceBlogId, mServiceRequestedTimeframe);
                final String followersPath = String.format("/sites/%s/stats/followers?type=wpcom&max=%s", mServiceBlogId, 12);
                statsNetworkRequests.add(restClientUtils.get(followersPath, followersListener, followersListener));

                // Followers EMAIL
                RestListener followersEmailListener = new RestListener(StatsEndpointsEnum.FOLLOWERS_EMAIL, mServiceBlogId, mServiceRequestedTimeframe);
                final String followersEmailPath = String.format("/sites/%s/stats/followers?type=email&max=%s", mServiceBlogId, 12);
                statsNetworkRequests.add(restClientUtils.get(followersEmailPath, followersEmailListener, followersEmailListener));

                // Tags and Categories
                RestListener tagsListener = new RestListener(StatsEndpointsEnum.TAGS_AND_CATEGORIES, mServiceBlogId, mServiceRequestedTimeframe);
                final String tagsPath = String.format("/sites/%s/stats/tags?max=%s", mServiceBlogId, 12);
                statsNetworkRequests.add(restClientUtils.get(tagsPath, tagsListener, tagsListener));

                // Publicize
                RestListener publicizeListener = new RestListener(StatsEndpointsEnum.PUBLICIZE, mServiceBlogId, mServiceRequestedTimeframe);
                final String publicizePath = String.format("/sites/%s/stats/publicize?max=%s", mServiceBlogId, 12);
                statsNetworkRequests.add(restClientUtils.get(publicizePath, publicizeListener, publicizeListener));

                numberOfNetworkCalls = statsNetworkRequests.size();
            } // end run
        } .start();
    }

    private class RestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        protected String mRequestBlogId;
        private final StatsTimeframe mTimeframe;
        protected Serializable mResponseObjectModel;
        final StatsEndpointsEnum mEndpointName;

        public RestListener(StatsEndpointsEnum endpointName, String blogId, StatsTimeframe timeframe) {
            mRequestBlogId = blogId;
            mTimeframe = timeframe;
            mEndpointName = endpointName;
        }

        @Override
        public void onResponse(final JSONObject response) {
            if (mServiceBlogId == null || mServiceRequestedTimeframe == null
            || !mServiceBlogId.equals(mRequestBlogId) || mServiceRequestedTimeframe != mTimeframe ) {
                return;
            }
            parseResponseExecutor.submit(new Thread() {
                @Override
                public void run() {
                    // Re-check here that the use has not changed the blog
                    if (mServiceBlogId == null || !mServiceBlogId.equals(mRequestBlogId)) {
                        return;
                    }
                    numberOfFinishedNetworkCalls++;
                    // do other stuff here
                    if (response != null) {
                        try {
                            AppLog.d(T.STATS, response.toString());
                            mResponseObjectModel = parseResponse(response);
                        } catch (JSONException e) {
                            AppLog.e(AppLog.T.STATS, e);
                        } catch (RemoteException e) {
                            AppLog.e(AppLog.T.STATS, e);
                        } catch (OperationApplicationException e) {
                            AppLog.e(AppLog.T.STATS, e);
                        }
                    }
                    notifySectionUpdated();
                    checkAllRequestsFinished();
                }
            });
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            if (mServiceBlogId == null || !mServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            parseResponseExecutor.submit(new Thread() {
                @Override
                public void run() {
                    // Re-check here that the use has not changed the blog
                    if (mServiceBlogId == null || !mServiceBlogId.equals(mRequestBlogId)) {
                        return;
                    }
                    numberOfFinishedNetworkCalls++;
                    AppLog.e(T.STATS, this.getClass().getName() + " responded with an Error");
                    if (volleyError != null) {
                        AppLog.e(T.STATS, "Error details: \n" + volleyError.getMessage(), volleyError);
                    }
                    mResponseObjectModel = volleyError;
                    notifySectionUpdated();
                    checkAllRequestsFinished();
                }
            });
        }

        private void notifySectionUpdated() {
            Intent intent = new Intent()
                    .setAction(ACTION_STATS_SECTION_UPDATED)
                    .putExtra(EXTRA_ENDPOINT_NAME, mEndpointName)
                    .putExtra(EXTRA_ENDPOINT_DATA, mResponseObjectModel);
            LocalBroadcastManager.getInstance(WordPress.getContext()).sendBroadcast(intent);
        }

        Serializable parseResponse(JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
            Serializable model = null;
            switch (mEndpointName) {
                case VISITS:
                    model = new VisitsModel(mRequestBlogId, response);
                    break;
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
                case FOLLOWERS_WPCOM:
                    model = new FollowersModel(mRequestBlogId, response);
                    break;
                case FOLLOWERS_EMAIL:
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

    private void stopService() {
        /* Stop the service if this is the current response, or mServiceBlogId is null
        String currentServiceBlogId = getServiceBlogId();
        if (currentServiceBlogId == null || currentServiceBlogId.equals(mRequestBlogId)) {
            stopService();
        }*/
        broadcastUpdate(false);
        stopSelf(mServiceStartId);
    }

    /*
     * called when either (a) the response has been received and parsed, or (b) the request failed
     *
     * Only one Thread access this method at the same time
     *
     */
    private void checkAllRequestsFinished() {
        if (isNetworkingDone()) {
            stopService();
        }
    }

    boolean isNetworkingDone() {
        return numberOfFinishedNetworkCalls == numberOfNetworkCalls;
    }

    /*
     * broadcast that the update has started/ended - used by StatsActivity to animate refresh icon
     * while update is in progress
     */
    private void broadcastUpdate(boolean isUpdating) {
        Intent intent = new Intent()
                .setAction(ACTION_STATS_UPDATING)
                .putExtra(EXTRA_IS_UPDATING, isUpdating);
       /* if (mErrorObject != null) {
            intent.putExtra(EXTRA_IS_ERROR, true);
            intent.putExtra(EXTRA_ERROR_OBJECT, mErrorObject);
        }
*/
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }


    /*
    private void broadcastSectionUpdating(StatsEndpointsEnum sectionName) {
        Intent intent = new Intent()
                .setAction(ACTION_STATS_UPDATING)
                .putExtra(EXTRA_ENDPOINT_NAME, sectionName)
                .putExtra(EXTRA_IS_UPDATING, true);

        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }
*/

        /*
    private class SummaryCallListener extends AbsListener {

        public SummaryCallListener(String blogId, StatsTimeframe timeframe) {
            super(blogId, timeframe);
        }

        Serializable parseResponse(JSONObject response) throws JSONException, RemoteException,
                OperationApplicationException {
        //    AppLog.d(T.STATS, ">>>>>>> " + this.getClass().getName() );
        //    AppLog.d(T.STATS, response.toString());
            SummaryModel summaryModel = new SummaryModel();
            summaryModel.setBlogID(mRequestBlogId);
            summaryModel.setFollowers(response.optInt("followers", 0));
            summaryModel.setViews(response.optInt("views", 0));
            summaryModel.setReblog(response.optInt("reblogs", 0));
            summaryModel.setLike(response.optInt("likes", 0));
            summaryModel.setVisitors(response.optInt("visitors", 0));
            summaryModel.setComments(response.optInt("comments", 0));
            summaryModel.setDate(response.getString("date"));
            summaryModel.setPeriod(response.getString("period"));
            return summaryModel;
         //   AppLog.d(T.STATS, "<<<<<<< " + this.getClass().getName() );
        }

        StatsSectionEnum getSectionEnum() {
            return StatsSectionEnum.SUMMARY;
        }
    }
*/

/*
    private String getBarChartPath(String blogID, StatsBarChartUnit mBarChartUnit, int quantity) {
        String path = String.format("/sites/%s/stats/visits", blogID);
        String unit = mBarChartUnit.name().toLowerCase(Locale.ENGLISH);
        path += String.format("?unit=%s", unit);
        if (quantity > 0) {
            path += String.format("&quantity=%d", quantity);
        }
        return path;
    }

    private class RestBatchCallListener implements RestRequest.Listener, RestRequest.ErrorListener {
        final String mRequestBlogId, mSummaryAPICallPath, mBarChartWeekPath, mBarChartMonthPath,
        mTopPostsAndPagesTodayPath, mTopPostsAndPagesYesterdayPath, mReferrersTodayPath,
        mReferrersYesterdayPath, mClicksTodayPath, mClicksYesterdayPath,
        mSearchEngineTermsTodayPath, mSearchEngineTermsYesterdayPath,
        mViewByCountryTodayPath, mViewByCountryYesterdayPath;

        RestBatchCallListener(String blogId, String summaryAPICallPath,
                String barChartWeekPath, String barChartMonthPath,
                String topPostsAndPagesTodayPath, String topPostsAndPagesYesterdayPath,
                String referrersTodayPath, String referrersYesterdayPath,
                String clicksTodayPath, String clicksYesterdayPath,
                String searchEngineTermsTodayPath, String searchEngineTermsYesterdayPath,
                String viewByCountryTodayPath, String viewByCountryYesterdayPath) {
            this.mRequestBlogId = blogId;
            this.mSummaryAPICallPath = summaryAPICallPath;
            this.mBarChartWeekPath = barChartWeekPath;
            this.mBarChartMonthPath = barChartMonthPath;
            this.mTopPostsAndPagesTodayPath = topPostsAndPagesTodayPath;
            this.mTopPostsAndPagesYesterdayPath = topPostsAndPagesYesterdayPath;
            this.mReferrersTodayPath = referrersTodayPath;
            this.mReferrersYesterdayPath = referrersYesterdayPath;
            this.mClicksTodayPath = clicksTodayPath;
            this.mClicksYesterdayPath = clicksYesterdayPath;
            this.mSearchEngineTermsTodayPath = searchEngineTermsTodayPath;
            this.mSearchEngineTermsYesterdayPath = searchEngineTermsYesterdayPath;
            this.mViewByCountryTodayPath = viewByCountryTodayPath;
            this.mViewByCountryYesterdayPath = viewByCountryYesterdayPath;
        }

        @Override
        public void onResponse(final JSONObject response) {
            // single thread
            ThreadPoolExecutor parseResponseExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);
            parseResponseExecutor.submit(new Thread() {
                @Override
                public void run() {
                    parseSummaryResponse(response);
                    parseBarChartResponse(response);
                    parseTopPostsAndPagesResponse(response);
                    parseReferrersResponse(response);
                    parseClicksResponse(response);
                    parseSearchEngineTermsResponse(response);
                    parseViewsByCountryResponse(response);

                    // Stop the service if this is the current response, or mServiceBlogId is null
                    String currentServiceBlogId = getServiceBlogId();
                    if (currentServiceBlogId == null || currentServiceBlogId.equals(mRequestBlogId)) {
                        stopService();
                    }
                }
            });
        }

        private void parseViewsByCountryResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] viewsByCountryPaths = {mViewByCountryTodayPath, mViewByCountryYesterdayPath};
            for (String currentViewsByCountryPath : viewsByCountryPaths) {
                if (response.has(currentViewsByCountryPath)) {
                    try {
                        final JSONObject currentViewsByCountryJsonObject =
                                response.getJSONObject(currentViewsByCountryPath);
                        if (!isSingleCallResponseError(currentViewsByCountryPath, currentViewsByCountryJsonObject)) {
                            if (!currentViewsByCountryJsonObject.has("country-views")) {
                                return;
                            }

                            JSONArray results = currentViewsByCountryJsonObject.getJSONArray("country-views");
                            int count = Math.min(results.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);
                            String date = currentViewsByCountryJsonObject.getString("date");
                            long dateMs = StatsUtils.toMs(date);
                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            if (count > 0) {
                                // delete data with the same date, and data older than two days ago (keep
                                // yesterday's data)
                                ContentProviderOperation deleteOp = ContentProviderOperation
                                        .newDelete(StatsContentProvider.STATS_GEOVIEWS_URI)
                                        .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                                                mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                        }).build();
                                operations.add(deleteOp);
                            }

                            for (int i = 0; i < count; i++) {
                                JSONObject result = results.getJSONObject(i);
                                StatsGeoview stat = new StatsGeoview(mRequestBlogId, result);
                                ContentValues values = StatsGeoviewsTable.getContentValues(stat);
                                ContentProviderOperation op = ContentProviderOperation
                                        .newInsert(StatsContentProvider.STATS_GEOVIEWS_URI).withValues(values).build();
                                operations.add(op);
                            }

                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentViewsByCountryPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentViewsByCountryPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentViewsByCountryPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_GEOVIEWS_URI, null);
        }

        private void parseSearchEngineTermsResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] searchEngineTermsPaths = {mSearchEngineTermsTodayPath, mSearchEngineTermsYesterdayPath};
            for (String currentSearchEngineTermsPath : searchEngineTermsPaths) {
                if (response.has(currentSearchEngineTermsPath)) {
                    try {
                        final JSONObject currentSearchEngineTermsJsonObject =
                                response.getJSONObject(currentSearchEngineTermsPath);
                        if (!isSingleCallResponseError(currentSearchEngineTermsPath,
                                currentSearchEngineTermsJsonObject)) {
                            String date = currentSearchEngineTermsJsonObject.getString("date");
                            long dateMs = StatsUtils.toMs(date);

                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            ContentProviderOperation deleteOp = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)",
                                            new String[] {
                                            mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                    }).build();

                            operations.add(deleteOp);

                            JSONArray results = currentSearchEngineTermsJsonObject.getJSONArray("search-terms");

                            int count = Math.min(results.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);
                            for (int i = 0; i < count; i++) {
                                JSONArray result = results.getJSONArray(i);
                                StatsSearchEngineTerm stat = new StatsSearchEngineTerm(mRequestBlogId, date, result);
                                ContentValues values = StatsSearchEngineTermsTable.getContentValues(stat);
                                getContentResolver()
                                    .insert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, values);

                                ContentProviderOperation insertOp = ContentProviderOperation
                                        .newInsert(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI)
                                        .withValues(values)
                                        .build();
                                operations.add(insertOp);
                            }

                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentSearchEngineTermsPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentSearchEngineTermsPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentSearchEngineTermsPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_SEARCH_ENGINE_TERMS_URI, null);
        }

        private void parseClicksResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] clicksPaths = {mClicksTodayPath, mClicksYesterdayPath};
            for (String currentClickPath : clicksPaths) {
                if (response.has(currentClickPath)) {
                    try {
                        final JSONObject currentClicksJsonObject =
                                response.getJSONObject(currentClickPath);
                        if (!isSingleCallResponseError(currentClickPath, currentClicksJsonObject)) {
                            String date = currentClicksJsonObject.getString("date");
                            long dateMs = StatsUtils.toMs(date);

                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            // delete data with the same date, and data older than two days ago (keep yesterday's
                            // data)
                            ContentProviderOperation deleteGroup = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_CLICK_GROUP_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)",
                                            new String[] {
                                            mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                    }).build();
                            ContentProviderOperation deleteChildOp = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_CLICKS_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)",
                                            new String[] {mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""}
                                    )
                                    .build();

                            operations.add(deleteGroup);
                            operations.add(deleteChildOp);

                            JSONArray groups = currentClicksJsonObject.getJSONArray("clicks");

                            // insert groups, limited to the number that can actually be displayed
                            int groupsCount = Math.min(groups.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);
                            for (int i = 0; i < groupsCount; i++) {
                                JSONObject group = groups.getJSONObject(i);
                                StatsClickGroup statGroup = new StatsClickGroup(mRequestBlogId, date, group);
                                ContentValues values = StatsClickGroupsTable.getContentValues(statGroup);

                                ContentProviderOperation insertGroupOp = ContentProviderOperation
                                        .newInsert(StatsContentProvider.STATS_CLICK_GROUP_URI).withValues(values)
                                        .build();
                                operations.add(insertGroupOp);

                                // insert children if there are any, limited to the number that can be displayed
                                JSONArray clicks = group.getJSONArray("results");
                                int childCount = Math.min(clicks.length(), StatsUIHelper.STATS_CHILD_MAX_ITEMS);
                                if (childCount > 1) {
                                    for (int j = 0; j < childCount; j++) {
                                        StatsClick stat = new StatsClick(mRequestBlogId, date,
                                                statGroup.getGroupId(), clicks.getJSONArray(j));
                                        ContentValues v = StatsClicksTable.getContentValues(stat);
                                        ContentProviderOperation insertChildOp = ContentProviderOperation
                                                .newInsert(StatsContentProvider.STATS_CLICKS_URI).withValues(v).build();
                                        operations.add(insertChildOp);
                                    }
                                }
                            }

                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentClickPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentClickPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentClickPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_CLICK_GROUP_URI, null);
            getContentResolver().notifyChange(StatsContentProvider.STATS_CLICKS_URI, null);
        }

        private void parseReferrersResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] referrersPaths = {mReferrersTodayPath, mReferrersYesterdayPath};
            for (String currentReferrerPath : referrersPaths) {
                if (response.has(currentReferrerPath)) {
                    try {
                        final JSONObject currentReferrersJsonObject =
                                response.getJSONObject(currentReferrerPath);
                        if (!isSingleCallResponseError(currentReferrerPath, currentReferrersJsonObject)) {
                            String date = currentReferrersJsonObject.getString("date");
                            long dateMs = StatsUtils.toMs(date);

                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            // delete data with the same date, and data older than two days ago (keep yesterday's
                            // data)
                            ContentProviderOperation deleteGroupOp = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_REFERRER_GROUP_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                                            mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                    }).build();
                            operations.add(deleteGroupOp);

                            ContentProviderOperation deleteOp = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_REFERRERS_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                                            mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                    }).build();
                            operations.add(deleteOp);

                            JSONArray groups = currentReferrersJsonObject.getJSONArray("referrers");
                            int groupsCount = Math.min(groups.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);

                            // insert groups
                            for (int i = 0; i < groupsCount; i++) {
                                JSONObject group = groups.getJSONObject(i);
                                StatsReferrerGroup statGroup = new StatsReferrerGroup(mRequestBlogId, date, group);
                                ContentValues values = StatsReferrerGroupsTable.getContentValues(statGroup);
                                ContentProviderOperation insertGroupOp = ContentProviderOperation
                                        .newInsert(StatsContentProvider.STATS_REFERRER_GROUP_URI).withValues(values)
                                        .build();
                                operations.add(insertGroupOp);

                                // insert children, only if there is more than one entry
                                JSONArray referrers = group.getJSONArray("results");
                                int childCount = Math.min(referrers.length(), StatsUIHelper.STATS_CHILD_MAX_ITEMS);
                                if (childCount > 1) {
                                    for (int j = 0; j < childCount; j++) {
                                        StatsReferrer stat = new StatsReferrer(mRequestBlogId, date,
                                                statGroup.getGroupId(), referrers.getJSONArray(j));
                                        ContentValues v = StatsReferrersTable.getContentValues(stat);
                                        ContentProviderOperation insertChildOp = ContentProviderOperation
                                                .newInsert(StatsContentProvider.STATS_REFERRERS_URI).withValues(v)
                                                .build();
                                        operations.add(insertChildOp);
                                    }
                                }
                            }
                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentReferrerPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentReferrerPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentReferrerPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_REFERRER_GROUP_URI, null);
            getContentResolver().notifyChange(StatsContentProvider.STATS_REFERRERS_URI, null);
        }

        private void parseTopPostsAndPagesResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] topPostsAndPagesPaths = {mTopPostsAndPagesTodayPath, mTopPostsAndPagesYesterdayPath};
            for (String currentTopPostsAndPagesPath : topPostsAndPagesPaths) {
                if (response.has(currentTopPostsAndPagesPath)) {
                    try {
                        final JSONObject currentTopPostsAndPagesJsonObject =
                                response.getJSONObject(currentTopPostsAndPagesPath);
                        if (!isSingleCallResponseError(
                                currentTopPostsAndPagesPath, currentTopPostsAndPagesJsonObject)) {
                            if (!currentTopPostsAndPagesJsonObject.has("top-posts")) {
                                return;
                            }

                            JSONArray results = currentTopPostsAndPagesJsonObject.getJSONArray("top-posts");
                            int count = Math.min(results.length(), StatsUIHelper.STATS_GROUP_MAX_ITEMS);

                            String date = currentTopPostsAndPagesJsonObject.getString("date");
                            long dateMs = StatsUtils.toMs(date);

                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();
                            // delete data with the same date, and data older than two days ago (keep yesterday's data)
                            ContentProviderOperation deleteOp = ContentProviderOperation
                                    .newDelete(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI)
                                    .withSelection("blogId=? AND (date=? OR date<=?)", new String[] {
                                            mRequestBlogId, dateMs + "", (dateMs - TWO_DAYS) + ""
                                    }).build();
                            operations.add(deleteOp);

                            for (int i = 0; i < count; i++) {
                                JSONObject result = results.getJSONObject(i);
                                StatsTopPostsAndPages stat = new StatsTopPostsAndPages(mRequestBlogId, result);
                                ContentValues values = StatsTopPostsAndPagesTable.getContentValues(stat);
                                ContentProviderOperation op = ContentProviderOperation
                                        .newInsert(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI)
                                        .withValues(values)
                                        .build();
                                operations.add(op);
                            }

                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentTopPostsAndPagesPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentTopPostsAndPagesPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentTopPostsAndPagesPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_TOP_POSTS_AND_PAGES_URI, null);
        }

        private void parseBarChartResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            String[] barChartPaths = {mBarChartWeekPath, mBarChartMonthPath};
            for (String currentBarChartPath : barChartPaths) {
                if (response.has(currentBarChartPath)) {
                    try {
                        final JSONObject barChartJsonObject = response.getJSONObject(currentBarChartPath);
                        final StatsBarChartUnit currentUnit = currentBarChartPath.equals(mBarChartWeekPath)
                               ? StatsBarChartUnit.WEEK : StatsBarChartUnit.MONTH;
                        if (!isSingleCallResponseError(currentBarChartPath, barChartJsonObject)) {
                            if (!barChartJsonObject.has("data")) {
                                return;
                            }

                            Uri uri = StatsContentProvider.STATS_BAR_CHART_DATA_URI;
                            JSONArray results = barChartJsonObject.getJSONArray("data");

                            int count = results.length();

                            ArrayList<ContentProviderOperation> operations = new ArrayList<ContentProviderOperation>();

                            // delete old stats and insert new ones
                            if (count > 0) {
                                ContentProviderOperation op = ContentProviderOperation.newDelete(uri)
                                        .withSelection("blogId=? AND unit=?", new String[] {
                                                mRequestBlogId, currentUnit.name()
                                        }).build();
                                operations.add(op);
                            }

                            for (int i = 0; i < count; i++) {
                                JSONArray result = results.getJSONArray(i);
                                StatsBarChartData stat = new StatsBarChartData(mRequestBlogId, currentUnit, result);
                                ContentValues values = StatsBarChartDataTable.getContentValues(stat);

                                if (values != null && uri != null) {
                                    ContentProviderOperation op = ContentProviderOperation.newInsert(uri)
                                            .withValues(values).build();
                                    operations.add(op);
                                }
                            }

                            getContentResolver().applyBatch(BuildConfig.STATS_PROVIDER_AUTHORITY, operations);
                        }
                    } catch (RemoteException e) {
                        logSingleCallError(currentBarChartPath, e);
                    } catch (OperationApplicationException e) {
                        logSingleCallError(currentBarChartPath, e);
                    } catch (JSONException e) {
                        logSingleCallError(currentBarChartPath, e);
                    }
                }
            }
            getContentResolver().notifyChange(StatsContentProvider.STATS_BAR_CHART_DATA_URI, null);
        }

        private void parseSummaryResponse(final JSONObject response) {
            String currentServiceBlogId = getServiceBlogId();
            if (currentServiceBlogId == null || !currentServiceBlogId.equals(mRequestBlogId)) {
                return;
            }
            final String currentPath = mSummaryAPICallPath;
            if (response.has(currentPath)) {
                try {
                    JSONObject summaryJsonObject = response.getJSONObject(currentPath);
                    if (!isSingleCallResponseError(currentPath, summaryJsonObject)) {
                        if (summaryJsonObject == null) {
                            return;
                        }
                        // save summary, then send broadcast that they've changed
                        StatsUtils.saveSummary(mRequestBlogId, summaryJsonObject);
                        StatsSummary stats = StatsUtils.getSummary(mRequestBlogId);
                        if (stats != null) {
                            LocalBroadcastManager lbm = LocalBroadcastManager.getInstance(WordPress.getContext());
                            Intent intent = new Intent(StatsService.ACTION_STATS_SUMMARY_UPDATED);
                            intent.putExtra(StatsService.STATS_SUMMARY_UPDATED_EXTRA, stats);
                            lbm.sendBroadcast(intent);
                        }
                    }
                } catch (JSONException e) {
                    logSingleCallError(mSummaryAPICallPath, e);
                }
            }
        }

        private boolean isSingleCallResponseError(String restCallPATH, final JSONObject response) {
            if (response.has("errors")) {
                mErrorObject = response.toString();
                AppLog.e(AppLog.T.STATS, "The single call " + restCallPATH
                        + " failed with the following response: " + response.toString());
                return true;
            }

            return false;
        }

        private void logSingleCallError(String restCallPATH, Exception e) {
            AppLog.e(AppLog.T.STATS, "Single call failed " + restCallPATH, e);
        }

        private void stopService() {
            broadcastUpdate(false);
            stopSelf(mServiceStartId);
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            if (volleyError != null) {
                mErrorObject = volleyError;
                AppLog.e(T.STATS, "Error while reading Stats summary for " + mRequestBlogId + " "
                + volleyError.getMessage(), volleyError);
            }
            stopService();
        }
    }

*/
}
