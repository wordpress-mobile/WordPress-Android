package org.wordpress.android.ui.stats.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
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
    public static final String ARG_UPDATE_ALLTIME_STATS = "ARG_UPDATE_ALLTIME_STATS";
    public static final String ARG_UPDATE_GRAPH_STATS = "ARG_UPDATE_GRAPH_STATS";
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
    private final LinkedList<Request<JSONObject>> statsNetworkRequests = new LinkedList<>();
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
        boolean updateAlltimeStats = intent.getBooleanExtra(ARG_UPDATE_ALLTIME_STATS, true);
        boolean updateGraphStats = intent.getBooleanExtra(ARG_UPDATE_GRAPH_STATS, true);

        if (mServiceBlogId == null) {
            startTasks(blogId, period, requestedDate, updateGraphStats, updateAlltimeStats, startId);
        } else if (blogId.equals(mServiceBlogId) && mServiceRequestedTimeframe == period
                && requestedDate.equals(mServiceRequestedDate)) {
            // already running on the same blogID, same period
            // Do nothing
            AppLog.i(T.STATS, "StatsService is already running on this blogID - " + mServiceBlogId
                    + " for the same period and the same date.");
        } else {
            // stats is running on a different blogID
            stopRefresh();
            startTasks(blogId, period, requestedDate, updateGraphStats, updateAlltimeStats, startId);
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
                            final boolean updateGraphStats, final boolean updateAlltimeStats, final int startId) {
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

                if (updateGraphStats) {
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

                if (updateAlltimeStats) {
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
                }
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
                            //AppLog.d(T.STATS, response.toString());
                            mResponseObjectModel = StatsUtils.parseResponse(mEndpointName, mRequestBlogId, response);
                        } catch (JSONException e) {
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
}
