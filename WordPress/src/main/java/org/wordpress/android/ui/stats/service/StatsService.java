package org.wordpress.android.ui.stats.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.text.TextUtils;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.WordPressDB;
import org.wordpress.android.models.Blog;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.stats.StatsEvents;
import org.wordpress.android.ui.stats.StatsTimeframe;
import org.wordpress.android.ui.stats.StatsUtils;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import de.greenrobot.event.EventBus;

/**
 * Background service to retrieve Stats.
 * Parsing of response(s) and submission of new network calls are done by using a ThreadPoolExecutor with a single thread.
 */

public class StatsService extends Service {
    public static final String ARG_BLOG_ID = "blog_id";
    public static final String ARG_PERIOD = "stats_period";
    public static final String ARG_DATE = "stats_date";
    public static final String ARG_SECTION = "stats_section";
    public static final String ARG_MAX_RESULTS = "stats_max_results";
    public static final String ARG_PAGE_REQUESTED = "stats_page_requested";

    private static final int DEFAULT_NUMBER_OF_RESULTS = 12;
    // The number of results to return per page for Paged REST endpoints. Numbers larger than 20 will default to 20 on the server.
    public static final int MAX_RESULTS_REQUESTED_PER_PAGE = 20;

    public enum StatsEndpointsEnum {
        VISITS,
        TOP_POSTS,
        REFERRERS,
        CLICKS,
        GEO_VIEWS,
        AUTHORS,
        VIDEO_PLAYS,
        COMMENTS,
        FOLLOWERS_WPCOM,
        FOLLOWERS_EMAIL,
        COMMENT_FOLLOWERS,
        TAGS_AND_CATEGORIES,
        PUBLICIZE,
        SEARCH_TERMS;

        public String getRestEndpointPath() {
            switch (this) {
                case VISITS:
                    return "visits";
                case TOP_POSTS:
                    return "top-posts";
                case REFERRERS:
                    return "referrers";
                case CLICKS:
                    return "clicks";
                case GEO_VIEWS:
                    return "country-views";
                case AUTHORS:
                    return "top-authors";
                case VIDEO_PLAYS:
                    return "video-plays";
                case COMMENTS:
                    return "comments";
                case FOLLOWERS_WPCOM:
                    return "followers?type=wpcom";
                case FOLLOWERS_EMAIL:
                    return "followers?type=email";
                case COMMENT_FOLLOWERS:
                    return "comment-followers";
                case TAGS_AND_CATEGORIES:
                    return "tags";
                case PUBLICIZE:
                    return "publicize";
                case SEARCH_TERMS:
                    return "search-terms";
                default:
                    AppLog.i(T.STATS, "Called an update of Stats of unknown section!?? " + this.name());
                    return "";
            }
        }
    }

    private int mServiceStartId;
    private final LinkedList<Request<JSONObject>> mStatsNetworkRequests = new LinkedList<>();
    private final ThreadPoolExecutor singleThreadNetworkHandler = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

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
            AppLog.e(T.STATS, "StatsService was killed and restarted with a null intent.");
            // if this service's process is killed while it is started (after returning from onStartCommand(Intent, int, int)),
            // then leave it in the started state but don't retain this delivered intent.
            // Later the system will try to re-create the service.
            // Because it is in the started state, it will guarantee to call onStartCommand(Intent, int, int) after creating the new service instance;
            // if there are not any pending start commands to be delivered to the service, it will be called with a null intent object.
            stopRefresh();
            return START_NOT_STICKY;
        }

        final String blogId = intent.getStringExtra(ARG_BLOG_ID);
        if (TextUtils.isEmpty(blogId)) {
            AppLog.e(T.STATS, "StatsService was started with a blank blog_id ");
            return START_NOT_STICKY;
        }

        final StatsTimeframe period;
        if (intent.hasExtra(ARG_PERIOD)) {
            period = (StatsTimeframe) intent.getSerializableExtra(ARG_PERIOD);
        } else {
            period = StatsTimeframe.DAY;
        }

        final String requestedDate;
        if (intent.getStringExtra(ARG_DATE) == null) {
            AppLog.w(T.STATS, "StatsService is started with a NULL date on this blogID - "
                    + blogId + ". Using current date!!!");
            int parsedBlogID = Integer.parseInt(blogId);
            int localTableBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId(parsedBlogID);
            requestedDate = StatsUtils.getCurrentDateTZ(localTableBlogId);
        } else {
            requestedDate = intent.getStringExtra(ARG_DATE);
        }

        final int maxResultsRequested = intent.getIntExtra(ARG_MAX_RESULTS, DEFAULT_NUMBER_OF_RESULTS);
        final int pageRequested = intent.getIntExtra(ARG_PAGE_REQUESTED, -1);

        int[] sectionFromIntent = intent.getIntArrayExtra(ARG_SECTION);

        this.mServiceStartId = startId;
        for (int i=0; i < sectionFromIntent.length; i++){
            final StatsEndpointsEnum currentSectionsToUpdate = StatsEndpointsEnum.values()[sectionFromIntent[i]];
            singleThreadNetworkHandler.submit(new Thread() {
                @Override
                public void run() {
                    startTasks(blogId, period, requestedDate, currentSectionsToUpdate, maxResultsRequested, pageRequested);
                }
            });
        }

        return START_NOT_STICKY;
    }

    private void stopRefresh() {
        synchronized (mStatsNetworkRequests) {
            this.mServiceStartId = 0;
            for (Request<JSONObject> req : mStatsNetworkRequests) {
                if (req != null && !req.hasHadResponseDelivered() && !req.isCanceled()) {
                    req.cancel();
                }
            }
            mStatsNetworkRequests.clear();
        }
    }

    // A fast way to disable caching during develop or when we want to disable it
    // under some circumstances. Always true for now.
    private boolean isCacheEnabled() {
        return true;
    }

    // Check if we already have Stats
    private String getCachedStats(final String blogId, final StatsTimeframe timeframe, final String date, final StatsEndpointsEnum sectionToUpdate,
                                  final int maxResultsRequested, final int pageRequested) {
        if (!isCacheEnabled()) {
            return null;
        }

        int parsedBlogID = Integer.parseInt(blogId);
        int localTableBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId(parsedBlogID);
        return StatsTable.getStats(this, localTableBlogId, timeframe, date, sectionToUpdate, maxResultsRequested, pageRequested);
    }

    private void startTasks(final String blogId, final StatsTimeframe timeframe, final String date, final StatsEndpointsEnum sectionToUpdate,
                            final int maxResultsRequested, final int pageRequested) {

        String cachedStats = getCachedStats(blogId, timeframe, date, sectionToUpdate, maxResultsRequested, pageRequested);

        if (cachedStats != null) {
            Serializable mResponseObjectModel;
                try {
                    JSONObject response = new JSONObject(cachedStats);
                    mResponseObjectModel = StatsUtils.parseResponse(sectionToUpdate, blogId, response);
                    EventBus.getDefault().post(new StatsEvents.SectionUpdated(sectionToUpdate, blogId, timeframe, date,
                            maxResultsRequested, pageRequested, mResponseObjectModel));
                    checkAllRequestsFinished(null);
                    return;
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, e);
                }
        }

        final RestClientUtils restClientUtils = WordPress.getRestClientUtilsV1_1();

        String period = timeframe.getLabelForRestCall();
        /*AppLog.i(T.STATS, "A new Stats network request is required for blogID: " + blogId + " - period: " + period
                + " - date: " + date + " - StatsType: " + sectionToUpdate.name());
*/
        EventBus.getDefault().post(new StatsEvents.UpdateStatusChanged(true));

        RestListener vListener = new RestListener(sectionToUpdate, blogId, timeframe, date, maxResultsRequested, pageRequested);

        final String periodDateMaxPlaceholder =  "?period=%s&date=%s&max=%s";

        String path = String.format("/sites/%s/stats/" + sectionToUpdate.getRestEndpointPath(), blogId);
        synchronized (mStatsNetworkRequests) {
            switch (sectionToUpdate) {
                case VISITS:
                    path = String.format(path + "?unit=%s&quantity=10&date=%s", period, date);
                    break;
                case TOP_POSTS:
                case REFERRERS:
                case CLICKS:
                case GEO_VIEWS:
                case AUTHORS:
                case VIDEO_PLAYS:
                case SEARCH_TERMS:
                    path = String.format(path + periodDateMaxPlaceholder, period, date, maxResultsRequested);
                    break;
                case TAGS_AND_CATEGORIES:
                case PUBLICIZE:
                    path = String.format(path + "?max=%s", maxResultsRequested);
                    break;
                case COMMENTS:
                    // No parameters
                    break;
                case FOLLOWERS_WPCOM:
                    if (pageRequested < 1) {
                        path = String.format(path + "&max=%s", maxResultsRequested);
                    } else {
                        path = String.format(path + "&period=%s&date=%s&max=%s&page=%s",
                                period, date, maxResultsRequested, pageRequested);
                    }
                    break;
                case FOLLOWERS_EMAIL:
                    if (pageRequested < 1) {
                        path = String.format(path + "&max=%s", maxResultsRequested);
                    } else {
                        path = String.format(path + "&period=%s&date=%s&max=%s&page=%s",
                                period, date, maxResultsRequested, pageRequested);
                    }
                    break;
                case COMMENT_FOLLOWERS:
                    if (pageRequested < 1) {
                        path = String.format(path + "?max=%s", maxResultsRequested);
                    } else {
                        path = String.format(path + "?period=%s&date=%s&max=%s&page=%s", period,
                                date, maxResultsRequested, pageRequested);
                    }
                    break;
                default:
                    AppLog.i(T.STATS, "Called an update of Stats of unknown section!?? " + sectionToUpdate.name());
                    return;
            }

            // We need to check if we already have the same request in the queue
            if (checkIfRequestShouldBeEnqueued(restClientUtils, path)) {
                AppLog.d(AppLog.T.STATS, "Enqueuing the following Stats request " + path);
                Request<JSONObject> currentRequest = restClientUtils.get(path, vListener, vListener);
                vListener.currentRequest = currentRequest;
                currentRequest.setTag("StatsCall");
                mStatsNetworkRequests.add(currentRequest);
            } else {
                AppLog.d(AppLog.T.STATS, "Stats request is already in the queue:" + path);
            }
        }
    }

    /**
     *  This method checks if we already have the same request in the Queue. No need to re-enqueue a new request
     *  if one with the same parameters is there.
     *
     *  This method is a kind of tricky, since it does the comparison by checking the origin URL of requests.
     *  To do that we had to get the fullURL of the new request by calling a method of the REST client `getAbsoluteURL`.
     *  That's good for now, but could lead to errors if the RestClient changes the way the URL is constructed internally,
     *  by calling `getAbsoluteURL`.
     *
     *  - Another approach would involve the get of the requests ErrorListener and the check Listener's parameters.
     *  - Cleanest approach is for sure to create a new class that extends Request<JSONObject> and stores parameters for later comparison,
     *  unfortunately we have to change the REST Client and RestClientUtils a lot if we want follow this way...
     *
     */
    private boolean checkIfRequestShouldBeEnqueued(final RestClientUtils restClientUtils, String path) {
        String absoluteRequestPath = restClientUtils.getRestClient().getAbsoluteURL(path);
        Iterator<Request<JSONObject>> it = mStatsNetworkRequests.iterator();
        while (it.hasNext()) {
            Request<JSONObject> req = it.next();
            if (!req.hasHadResponseDelivered() && !req.isCanceled() &&
                    absoluteRequestPath.equals(req.getOriginUrl())) {
                return false;
            }
        }

        return true;
    }

    private class RestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        final String mRequestBlogId;
        private final StatsTimeframe mTimeframe;
        Serializable mResponseObjectModel;
        final StatsEndpointsEnum mEndpointName;
        private final String mDate;
        private Request<JSONObject> currentRequest;
        private final int mMaxResultsRequested, mPageRequested;

        public RestListener(StatsEndpointsEnum endpointName, String blogId, StatsTimeframe timeframe, String date,
                            final int maxResultsRequested, final int pageRequested) {
            mRequestBlogId = blogId;
            mTimeframe = timeframe;
            mEndpointName = endpointName;
            mDate = date;
            mMaxResultsRequested = maxResultsRequested;
            mPageRequested = pageRequested;
        }

        @Override
        public void onResponse(final JSONObject response) {
            singleThreadNetworkHandler.submit(new Thread() {
                @Override
                public void run() {
                    // do other stuff here
                    if (response != null) {
                        try {
                            //AppLog.d(T.STATS, response.toString());
                            mResponseObjectModel = StatsUtils.parseResponse(mEndpointName, mRequestBlogId, response);
                            if (isCacheEnabled()) {
                                int parsedBlogID = Integer.parseInt(mRequestBlogId);
                                int localTableBlogId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId(parsedBlogID);
                                StatsTable.insertStats(StatsService.this, localTableBlogId, mTimeframe, mDate, mEndpointName,
                                        mMaxResultsRequested, mPageRequested,
                                        response.toString(), System.currentTimeMillis());
                            }
                        } catch (JSONException e) {
                            AppLog.e(AppLog.T.STATS, e);
                        }
                    }
                    EventBus.getDefault().post(new StatsEvents.SectionUpdated(mEndpointName, mRequestBlogId, mTimeframe, mDate,
                            mMaxResultsRequested, mPageRequested, mResponseObjectModel));
                    checkAllRequestsFinished(currentRequest);
                }
            });
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            singleThreadNetworkHandler.submit(new Thread() {
                @Override
                public void run() {
                    AppLog.e(T.STATS, "Error while loading Stats!");
                    StatsUtils.logVolleyErrorDetails(volleyError);

                    // Check here if this is an authentication error
                    // .com authentication errors are handled automatically by the app
                    if (volleyError.networkResponse != null) {
                        NetworkResponse networkResponse = volleyError.networkResponse;
                        if (networkResponse.statusCode == 403 && networkResponse.data != null) {
                            if (new String(networkResponse.data).contains("unauthorized")) {
                                int localId = WordPress.wpDB.getLocalTableBlogIdForRemoteBlogId(
                                        Integer.parseInt(mRequestBlogId)
                                );
                                Blog blog = WordPress.wpDB.instantiateBlogByLocalId(localId);
                                if (blog != null && blog.isJetpackPowered()) {
                                    EventBus.getDefault().post(new StatsEvents.JetpackAuthError(localId));
                                }
                            }
                        }
                    }
                    mResponseObjectModel = volleyError;
                    EventBus.getDefault().post(new StatsEvents.SectionUpdated(mEndpointName, mRequestBlogId, mTimeframe, mDate,
                            mMaxResultsRequested, mPageRequested, mResponseObjectModel));
                    checkAllRequestsFinished(currentRequest);
                }
            });
        }
    }

    private void stopService() {
        /* Stop the service if this is the current response, or mServiceBlogId is null
        String currentServiceBlogId = getServiceBlogId();
        if (currentServiceBlogId == null || currentServiceBlogId.equals(mRequestBlogId)) {
            stopService();
        }*/
        EventBus.getDefault().post(new StatsEvents.UpdateStatusChanged(false));
        stopSelf(mServiceStartId);
    }


    private void checkAllRequestsFinished(Request<JSONObject> req) {
        synchronized (mStatsNetworkRequests) {
            if (req != null) {
                mStatsNetworkRequests.remove(req);
            }
            boolean isStillWorking = mStatsNetworkRequests.size() > 0 ;
            EventBus.getDefault().post(new StatsEvents.UpdateStatusChanged(isStillWorking));
        }
    }
}