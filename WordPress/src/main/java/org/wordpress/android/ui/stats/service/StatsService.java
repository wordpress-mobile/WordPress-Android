package org.wordpress.android.ui.stats.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import com.android.volley.Request;
import com.android.volley.VolleyError;
import com.wordpress.rest.RestRequest;

import org.json.JSONException;
import org.json.JSONObject;
import org.wordpress.android.WordPress;
import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.fluxc.store.SiteStore;
import org.wordpress.android.networking.RestClientUtils;
import org.wordpress.android.ui.stats.StatsEvents;
import org.wordpress.android.ui.stats.StatsTimeframe;
import org.wordpress.android.ui.stats.StatsUtils;
import org.wordpress.android.ui.stats.StatsWidgetProvider;
import org.wordpress.android.ui.stats.datasets.StatsTable;
import org.wordpress.android.ui.stats.exceptions.StatsError;
import org.wordpress.android.ui.stats.models.AuthorsModel;
import org.wordpress.android.ui.stats.models.BaseStatsModel;
import org.wordpress.android.ui.stats.models.ClicksModel;
import org.wordpress.android.ui.stats.models.CommentFollowersModel;
import org.wordpress.android.ui.stats.models.CommentsModel;
import org.wordpress.android.ui.stats.models.FollowersModel;
import org.wordpress.android.ui.stats.models.GeoviewsModel;
import org.wordpress.android.ui.stats.models.InsightsAllTimeModel;
import org.wordpress.android.ui.stats.models.InsightsLatestPostDetailsModel;
import org.wordpress.android.ui.stats.models.InsightsLatestPostModel;
import org.wordpress.android.ui.stats.models.InsightsPopularModel;
import org.wordpress.android.ui.stats.models.PublicizeModel;
import org.wordpress.android.ui.stats.models.ReferrersModel;
import org.wordpress.android.ui.stats.models.SearchTermsModel;
import org.wordpress.android.ui.stats.models.TagsContainerModel;
import org.wordpress.android.ui.stats.models.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.models.VideoPlaysModel;
import org.wordpress.android.ui.stats.models.VisitModel;
import org.wordpress.android.ui.stats.models.VisitsModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;

import java.io.Serializable;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;

import javax.inject.Inject;

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
        SEARCH_TERMS,
        INSIGHTS_POPULAR,
        INSIGHTS_ALL_TIME,
        INSIGHTS_TODAY,
        INSIGHTS_LATEST_POST_SUMMARY,
        INSIGHTS_LATEST_POST_VIEWS;

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
                case INSIGHTS_POPULAR:
                    return "insights";
                case INSIGHTS_ALL_TIME:
                    return "";
                case INSIGHTS_TODAY:
                    return "summary";
                case INSIGHTS_LATEST_POST_SUMMARY:
                    return "posts";
                case INSIGHTS_LATEST_POST_VIEWS:
                    return "post";
                default:
                    AppLog.i(T.STATS, "Called an update of Stats of unknown section!?? " + this.name());
                    return "";
            }
        }

        public StatsEvents.SectionUpdatedAbstract getEndpointUpdateEvent(final long siteId,
                                                                         final StatsTimeframe timeframe,
                                                                         final String date,
                                                                         final int maxResultsRequested,
                                                                         final int pageRequested,
                                                                         final BaseStatsModel data) {
            switch (this) {
                case VISITS:
                    return new StatsEvents.VisitorsAndViewsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (VisitsModel)data);
                case TOP_POSTS:
                    return new StatsEvents.TopPostsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (TopPostsAndPagesModel)data);
                case REFERRERS:
                    return new StatsEvents.ReferrersUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (ReferrersModel)data);
                case CLICKS:
                    return new StatsEvents.ClicksUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (ClicksModel)data);
                case AUTHORS:
                    return new StatsEvents.AuthorsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (AuthorsModel)data);
                case GEO_VIEWS:
                    return new StatsEvents.CountriesUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (GeoviewsModel)data);
                case VIDEO_PLAYS:
                    return new StatsEvents.VideoPlaysUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (VideoPlaysModel)data);
                case SEARCH_TERMS:
                    return new StatsEvents.SearchTermsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (SearchTermsModel)data);
                case COMMENTS:
                    return new StatsEvents.CommentsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (CommentsModel)data);
                case COMMENT_FOLLOWERS:
                    return new StatsEvents.CommentFollowersUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (CommentFollowersModel)data);
                case TAGS_AND_CATEGORIES:
                    return new StatsEvents.TagsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (TagsContainerModel)data);
                case PUBLICIZE:
                    return new StatsEvents.PublicizeUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (PublicizeModel)data);
                case FOLLOWERS_WPCOM:
                    return new StatsEvents.FollowersWPCOMUdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (FollowersModel)data);
                case FOLLOWERS_EMAIL:
                    return new StatsEvents.FollowersEmailUdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (FollowersModel)data);
                case INSIGHTS_POPULAR:
                    return new StatsEvents.InsightsPopularUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (InsightsPopularModel)data);
                case INSIGHTS_ALL_TIME:
                    return new StatsEvents.InsightsAllTimeUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (InsightsAllTimeModel)data);
                case INSIGHTS_TODAY:
                    return new StatsEvents.VisitorsAndViewsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (VisitsModel)data);
                case INSIGHTS_LATEST_POST_SUMMARY:
                    return new StatsEvents.InsightsLatestPostSummaryUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (InsightsLatestPostModel)data);
                case INSIGHTS_LATEST_POST_VIEWS:
                    return new StatsEvents.InsightsLatestPostDetailsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (InsightsLatestPostDetailsModel)data);
                default:
                    AppLog.e(T.STATS, "Can't find an Update Event that match the current endpoint: " + this.name());
            }

            return null;
        }
    }

    private int mServiceStartId;
    private final LinkedList<Request<JSONObject>> mStatsNetworkRequests = new LinkedList<>();
    private final ThreadPoolExecutor singleThreadNetworkHandler = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

    @Inject SiteStore mSiteStore;

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(T.STATS, "service created");
        ((WordPress) getApplication()).component().inject(this);
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

        final long siteId = intent.getLongExtra(ARG_BLOG_ID, 0);
        if (siteId == 0) {
            AppLog.e(T.STATS, "StatsService was started with siteid == 0");
            return START_NOT_STICKY;
        }

        int[] sectionFromIntent = intent.getIntArrayExtra(ARG_SECTION);
        if (sectionFromIntent == null || sectionFromIntent.length == 0) {
            // No sections to update
            AppLog.e(T.STATS, "StatsService was started without valid sections info");
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
                    + siteId + ". Using current date.");
            SiteModel site = mSiteStore.getSiteBySiteId(siteId);
            requestedDate = StatsUtils.getCurrentDateTZ(site);
        } else {
            requestedDate = intent.getStringExtra(ARG_DATE);
        }

        final int maxResultsRequested = intent.getIntExtra(ARG_MAX_RESULTS, DEFAULT_NUMBER_OF_RESULTS);
        final int pageRequested = intent.getIntExtra(ARG_PAGE_REQUESTED, -1);

        this.mServiceStartId = startId;
        for (int i=0; i < sectionFromIntent.length; i++){
            final StatsEndpointsEnum currentSectionsToUpdate = StatsEndpointsEnum.values()[sectionFromIntent[i]];
            singleThreadNetworkHandler.submit(new Thread() {
                @Override
                public void run() {
                    startTasks(siteId, period, requestedDate, currentSectionsToUpdate, maxResultsRequested, pageRequested);
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
    private boolean cacheEnabledEh() {
        return true;
    }

    // Check if we already have Stats
    private String getCachedStats(final long siteId, final StatsTimeframe timeframe, final String date,
                                  final StatsEndpointsEnum sectionToUpdate, final int maxResultsRequested,
                                  final int pageRequested) {
        if (!cacheEnabledEh()) {
            return null;
        }
        return StatsTable.getStats(this, siteId, timeframe, date, sectionToUpdate, maxResultsRequested, pageRequested);
    }

    private void startTasks(final long blogId, final StatsTimeframe timeframe, final String date,
                            final StatsEndpointsEnum sectionToUpdate, final int maxResultsRequested,
                            final int pageRequested) {
        EventBus.getDefault().post(new StatsEvents.UpdateStatusChanged(true));

        String cachedStats = getCachedStats(blogId, timeframe, date, sectionToUpdate, maxResultsRequested, pageRequested);
        if (cachedStats != null) {
            BaseStatsModel mResponseObjectModel;
                try {
                    JSONObject response = new JSONObject(cachedStats);
                    mResponseObjectModel = StatsUtils.parseResponse(sectionToUpdate, blogId, response);

                    EventBus.getDefault().post(
                            sectionToUpdate.getEndpointUpdateEvent(blogId, timeframe, date,
                                    maxResultsRequested, pageRequested, mResponseObjectModel)
                    );

                    updateWidgetsUI(blogId, sectionToUpdate, timeframe, date, pageRequested, mResponseObjectModel);
                    checkAllRequestsFinished(null);
                    return;
                } catch (JSONException e) {
                    AppLog.e(AppLog.T.STATS, e);
                }
        }

        final RestClientUtils restClientUtils = WordPress.getRestClientUtilsV1_1();

        String period = timeframe.getLabelForRestCall();

        RestListener vListener = new RestListener(sectionToUpdate, blogId, timeframe, date, maxResultsRequested, pageRequested);

        final String periodDateMaxPlaceholder =  "?period=%s&date=%s&max=%s";

        String path = String.format(Locale.US, "/sites/%s/stats/" + sectionToUpdate.getRestEndpointPath(), blogId);
        synchronized (mStatsNetworkRequests) {
            switch (sectionToUpdate) {
                case VISITS:
                    path = String.format(Locale.US, path + "?unit=%s&quantity=15&date=%s", period, date);
                    break;
                case TOP_POSTS:
                case REFERRERS:
                case CLICKS:
                case GEO_VIEWS:
                case AUTHORS:
                case VIDEO_PLAYS:
                case SEARCH_TERMS:
                    path = String.format(Locale.US, path + periodDateMaxPlaceholder, period, date, maxResultsRequested);
                    break;
                case TAGS_AND_CATEGORIES:
                case PUBLICIZE:
                    path = String.format(Locale.US, path + "?max=%s", maxResultsRequested);
                    break;
                case COMMENTS:
                    // No parameters
                    break;
                case FOLLOWERS_WPCOM:
                    if (pageRequested < 1) {
                        path = String.format(Locale.US, path + "&max=%s", maxResultsRequested);
                    } else {
                        path = String.format(Locale.US, path + "&period=%s&date=%s&max=%s&page=%s",
                                period, date, maxResultsRequested, pageRequested);
                    }
                    break;
                case FOLLOWERS_EMAIL:
                    if (pageRequested < 1) {
                        path = String.format(Locale.US, path + "&max=%s", maxResultsRequested);
                    } else {
                        path = String.format(Locale.US, path + "&period=%s&date=%s&max=%s&page=%s",
                                period, date, maxResultsRequested, pageRequested);
                    }
                    break;
                case COMMENT_FOLLOWERS:
                    if (pageRequested < 1) {
                        path = String.format(Locale.US, path + "?max=%s", maxResultsRequested);
                    } else {
                        path = String.format(Locale.US, path + "?period=%s&date=%s&max=%s&page=%s", period,
                                date, maxResultsRequested, pageRequested);
                    }
                    break;
                case INSIGHTS_ALL_TIME:
                case INSIGHTS_POPULAR:
                    break;
                case INSIGHTS_TODAY:
                    path = String.format(Locale.US, path + "?period=day&date=%s", date);
                    break;
                case INSIGHTS_LATEST_POST_SUMMARY:
                    // This is an edge cases since  we're not loading stats but posts
                    path = String.format(Locale.US, "/sites/%s/%s", blogId, sectionToUpdate.getRestEndpointPath()
                            + "?order_by=date&number=1&type=post&fields=ID,title,URL,discussion,like_count,date");
                    break;
                case INSIGHTS_LATEST_POST_VIEWS:
                    // This is a kind of edge case, since we used the pageRequested parameter to request a single postID
                    path = String.format(Locale.US, path + "/%s?fields=views", pageRequested);
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
                    absoluteRequestPath.equals(req.getUrl())) {
                return false;
            }
        }

        return true;
    }

    // Call an updates on the installed widgets if the blog is the primary, the endpoint is Visits
    // the timeframe is DAY or INSIGHTS, and the date = TODAY
    private void updateWidgetsUI(long siteId, final StatsEndpointsEnum endpointName,
                                 StatsTimeframe timeframe, String date, int pageRequested,
                                 Serializable responseObjectModel) {
        if (pageRequested != -1) {
            return;
        }
        if (endpointName != StatsEndpointsEnum.VISITS) {
            return;
        }
        if (timeframe != StatsTimeframe.DAY && timeframe != StatsTimeframe.INSIGHTS) {
            return;
        }

        SiteModel site = mSiteStore.getSiteBySiteId(siteId);
        // make sure the data is for the current date
        if (!date.equals(StatsUtils.getCurrentDateTZ(site))) {
            return;
        }

        if (responseObjectModel == null) {
            // TODO What we want to do here?
            return;
        }

        if (!StatsWidgetProvider.blogDisplayedInWidgetEh(siteId)) {
            AppLog.d(AppLog.T.STATS, "The blog with remoteID " + siteId
                    + " is NOT displayed in any widget. Stats Service doesn't call an update of the widget.");
            return;
        }

        if (responseObjectModel instanceof VisitsModel) {
            VisitsModel visitsModel = (VisitsModel) responseObjectModel;
            if (visitsModel.getVisits() == null || visitsModel.getVisits().size() == 0) {
                return;
            }
            List<VisitModel> visits = visitsModel.getVisits();
            VisitModel data = visits.get(visits.size() - 1);
            StatsWidgetProvider.updateWidgets(getApplicationContext(), site, data);
        } else if (responseObjectModel instanceof VolleyError) {
            VolleyError error = (VolleyError) responseObjectModel;
            StatsWidgetProvider.updateWidgets(getApplicationContext(), site, mSiteStore, error);
        } else if (responseObjectModel instanceof StatsError) {
            StatsError statsError = (StatsError) responseObjectModel;
            StatsWidgetProvider.updateWidgets(getApplicationContext(), site, mSiteStore, statsError);
        }
    }

    private class RestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        final long mRequestBlogId;
        private final StatsTimeframe mTimeframe;
        final StatsEndpointsEnum mEndpointName;
        private final String mDate;
        private Request<JSONObject> currentRequest;
        private final int mMaxResultsRequested, mPageRequested;

        public RestListener(StatsEndpointsEnum endpointName, long blogId, StatsTimeframe timeframe, String date,
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
                    BaseStatsModel mResponseObjectModel = null;
                    if (response != null) {
                        try {
                            mResponseObjectModel = StatsUtils.parseResponse(mEndpointName, mRequestBlogId, response);
                            if (cacheEnabledEh()) {
                                StatsTable.insertStats(StatsService.this, mRequestBlogId, mTimeframe, mDate, mEndpointName,
                                        mMaxResultsRequested, mPageRequested,
                                        response.toString(), System.currentTimeMillis());
                            }
                        } catch (JSONException e) {
                            AppLog.e(AppLog.T.STATS, e);
                        }
                    }

                    EventBus.getDefault().post(
                            mEndpointName.getEndpointUpdateEvent(mRequestBlogId, mTimeframe, mDate,
                            mMaxResultsRequested, mPageRequested, mResponseObjectModel)
                    );

                    updateWidgetsUI(mRequestBlogId, mEndpointName, mTimeframe, mDate, mPageRequested, mResponseObjectModel);
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
                    BaseStatsModel mResponseObjectModel = null;
                    EventBus.getDefault().post(new StatsEvents.SectionUpdateError(mEndpointName, mRequestBlogId, mTimeframe, mDate,
                            mMaxResultsRequested, mPageRequested, volleyError));
                    updateWidgetsUI(mRequestBlogId, mEndpointName, mTimeframe, mDate, mPageRequested, mResponseObjectModel);
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
        synchronized (mStatsNetworkRequests) {
            mStatsNetworkRequests.clear();
        }
    }

    private void checkAllRequestsFinished(Request<JSONObject> req) {
        synchronized (mStatsNetworkRequests) {
            if (req != null) {
                mStatsNetworkRequests.remove(req);
            }
            boolean stillWorkingEh = mStatsNetworkRequests.size() > 0 || singleThreadNetworkHandler.getQueue().size() > 0;
            EventBus.getDefault().post(new StatsEvents.UpdateStatusChanged(stillWorkingEh));
        }
    }
}
