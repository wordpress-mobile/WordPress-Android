package org.wordpress.android.ui.stats.service;

import android.os.Bundle;

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
import org.wordpress.android.ui.stats.models.BaseStatsModel;
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
 * Parsing of response(s) and submission of new network calls are done by using a ThreadPoolExecutor
 * with a single thread.
 */

public class StatsServiceLogic {
    private static final int DEFAULT_NUMBER_OF_RESULTS = 12;

    private WordPress mApplication;
    private final LinkedList<Request<JSONObject>> mStatsNetworkRequests = new LinkedList<>();
    private final ThreadPoolExecutor mSingleThreadNetworkHandler = (ThreadPoolExecutor) Executors.newFixedThreadPool(1);

    private ServiceCompletionListener mCompletionListener;
    private Object mListenerCompanion;


    @Inject SiteStore mSiteStore;

    public StatsServiceLogic(ServiceCompletionListener completionListener) {
        mCompletionListener = completionListener;
    }

    public void onCreate(WordPress app) {
        AppLog.i(T.STATS, "service created");
        mApplication = app;
        app.component().inject(this);
    }

    public void onDestroy() {
        stopRefresh();
        AppLog.i(T.STATS, "service destroyed");
    }

    public void performTask(Bundle extras, Object companion) {
        mListenerCompanion = companion;
        if (extras == null) {
            AppLog.e(T.STATS, "StatsService was killed and restarted with a null intent.");
            // if this service's process is killed while it is started (after returning
            // from onStartCommand(Intent, int, int)), then leave it in the started state but don't retain
            // this delivered intent. Later the system will try to re-create the service. Because it is in the
            // started state, it will guarantee to call onStartCommand(Intent, int, int) after
            // creating the new service instance; if there are not any pending start commands to be delivered to
            // the service, it will be called with a null intent object.
            stopRefresh();
            return;
        }

        final long siteId = extras.getLong(StatsService.ARG_BLOG_ID, 0);
        if (siteId == 0) {
            AppLog.e(T.STATS, "StatsService was started with siteid == 0");
            return;
        }

        int[] sectionFromIntent = extras.getIntArray(StatsService.ARG_SECTION);
        if (sectionFromIntent == null || sectionFromIntent.length == 0) {
            // No sections to update
            AppLog.e(T.STATS, "StatsService was started without valid sections info");
            return;
        }

        final StatsTimeframe period;
        if (extras.containsKey(StatsService.ARG_PERIOD)) {
            period = (StatsTimeframe) extras.getSerializable(StatsService.ARG_PERIOD);
        } else {
            period = StatsTimeframe.DAY;
        }

        final String requestedDate;
        if (extras.getString(StatsService.ARG_DATE) == null) {
            AppLog.w(T.STATS, "StatsService is started with a NULL date on this blogID - "
                              + siteId + ". Using current date.");
            SiteModel site = mSiteStore.getSiteBySiteId(siteId);
            requestedDate = StatsUtils.getCurrentDateTZ(site);
        } else {
            requestedDate = extras.getString(StatsService.ARG_DATE);
        }

        final int maxResultsRequested = extras.getInt(StatsService.ARG_MAX_RESULTS, DEFAULT_NUMBER_OF_RESULTS);
        final int pageRequested = extras.getInt(StatsService.ARG_PAGE_REQUESTED, -1);

        for (int i = 0; i < sectionFromIntent.length; i++) {
            final StatsService.StatsEndpointsEnum currentSectionsToUpdate =
                    StatsService.StatsEndpointsEnum.values()[sectionFromIntent[i]];
            mSingleThreadNetworkHandler.submit(new Thread() {
                @Override
                public void run() {
                    startTasks(siteId, period, requestedDate, currentSectionsToUpdate, maxResultsRequested,
                               pageRequested);
                }
            });
        }

        return;
    }

    private void stopRefresh() {
        synchronized (mStatsNetworkRequests) {
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
    private String getCachedStats(final long siteId, final StatsTimeframe timeframe, final String date,
                                  final StatsService.StatsEndpointsEnum sectionToUpdate, final int maxResultsRequested,
                                  final int pageRequested) {
        if (!isCacheEnabled()) {
            return null;
        }
        return StatsTable.getStats(mApplication, siteId, timeframe, date, sectionToUpdate, maxResultsRequested,
                pageRequested);
    }

    private void startTasks(final long blogId, final StatsTimeframe timeframe, final String date,
                            final StatsService.StatsEndpointsEnum sectionToUpdate, final int maxResultsRequested,
                            final int pageRequested) {
        EventBus.getDefault().post(new StatsEvents.UpdateStatusChanged(true));

        String cachedStats =
                getCachedStats(blogId, timeframe, date, sectionToUpdate, maxResultsRequested, pageRequested);
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
                AppLog.e(T.STATS, e);
            }
        }

        final RestClientUtils restClientUtils = WordPress.getRestClientUtilsV1_1();

        String period = timeframe.getLabelForRestCall();

        RestListener vListener =
                new RestListener(sectionToUpdate, blogId, timeframe, date, maxResultsRequested, pageRequested);

        final String periodDateMaxPlaceholder = "?period=%s&date=%s&max=%s";

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
                    // This is an edge cases since we're not loading stats but posts
                    path = String.format(Locale.US, "/sites/%s/%s", blogId,
                            sectionToUpdate.getRestEndpointPath()
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
                AppLog.d(T.STATS, "Enqueuing the following Stats request " + path);
                Request<JSONObject> currentRequest = restClientUtils.get(path, vListener, vListener);
                vListener.mCurrentRequest = currentRequest;
                currentRequest.setTag("StatsCall");
                mStatsNetworkRequests.add(currentRequest);
            } else {
                AppLog.d(T.STATS, "Stats request is already in the queue:" + path);
            }
        }
    }

    /**
     * This method checks if we already have the same request in the Queue. No need to re-enqueue a new request
     * if one with the same parameters is there.
     * <p>
     * This method is a kind of tricky, since it does the comparison by checking the origin URL of requests.
     * To do that we had to get the fullURL of the new request by calling a method of the REST client `getAbsoluteURL`.
     * That's good for now, but could lead to errors if the RestClient changes the way the URL is constructed
     * internally, by calling `getAbsoluteURL`.
     * <p>
     * - Another approach would involve the get of the requests ErrorListener and the check Listener's parameters.
     * - Cleanest approach is for sure to create a new class that extends Request<JSONObject> and stores parameters
     * for later comparison, unfortunately we have to change the REST Client and RestClientUtils
     * a lot if we want follow this way...
     */
    private boolean checkIfRequestShouldBeEnqueued(final RestClientUtils restClientUtils, String path) {
        String absoluteRequestPath = restClientUtils.getRestClient().getAbsoluteURL(path);
        Iterator<Request<JSONObject>> it = mStatsNetworkRequests.iterator();
        while (it.hasNext()) {
            Request<JSONObject> req = it.next();
            if (!req.hasHadResponseDelivered() && !req.isCanceled()
                && absoluteRequestPath.equals(req.getUrl())) {
                return false;
            }
        }

        return true;
    }

    // Call an updates on the installed widgets if the blog is the primary, the endpoint is Visits
    // the timeframe is DAY or INSIGHTS, and the date = TODAY
    private void updateWidgetsUI(long siteId, final StatsService.StatsEndpointsEnum endpointName,
                                 StatsTimeframe timeframe, String date, int pageRequested,
                                 Serializable responseObjectModel) {
        if (pageRequested != -1) {
            return;
        }
        if (endpointName != StatsService.StatsEndpointsEnum.VISITS) {
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

        if (!StatsWidgetProvider.isBlogDisplayedInWidget(siteId)) {
            AppLog.d(T.STATS,
                     "The blog with remoteID " + siteId
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
            StatsWidgetProvider.updateWidgets(mApplication, site, data);
        } else if (responseObjectModel instanceof VolleyError) {
            VolleyError error = (VolleyError) responseObjectModel;
            StatsWidgetProvider.updateWidgets(mApplication, site, mSiteStore, error);
        } else if (responseObjectModel instanceof StatsError) {
            StatsError statsError = (StatsError) responseObjectModel;
            StatsWidgetProvider.updateWidgets(mApplication, site, mSiteStore, statsError);
        }
    }

    private class RestListener implements RestRequest.Listener, RestRequest.ErrorListener {
        final long mRequestBlogId;
        private final StatsTimeframe mTimeframe;
        final StatsService.StatsEndpointsEnum mEndpointName;
        private final String mDate;
        private Request<JSONObject> mCurrentRequest;
        private final int mMaxResultsRequested, mPageRequested;

        RestListener(StatsService.StatsEndpointsEnum endpointName, long blogId, StatsTimeframe timeframe, String date,
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
            mSingleThreadNetworkHandler.submit(new Thread() {
                @Override
                public void run() {
                    // do other stuff here
                    BaseStatsModel mResponseObjectModel = null;
                    if (response != null) {
                        try {
                            mResponseObjectModel = StatsUtils.parseResponse(mEndpointName, mRequestBlogId, response);
                            if (isCacheEnabled()) {
                                StatsTable.insertStats(mApplication, mRequestBlogId, mTimeframe, mDate,
                                                       mEndpointName,
                                                       mMaxResultsRequested, mPageRequested,
                                                       response.toString(), System.currentTimeMillis());
                            }
                        } catch (JSONException e) {
                            AppLog.e(T.STATS, e);
                        }
                    }

                    EventBus.getDefault().post(
                            mEndpointName.getEndpointUpdateEvent(mRequestBlogId, mTimeframe, mDate,
                                                                 mMaxResultsRequested, mPageRequested,
                                                                 mResponseObjectModel)
                                              );

                    updateWidgetsUI(mRequestBlogId, mEndpointName, mTimeframe, mDate, mPageRequested,
                                    mResponseObjectModel);
                    checkAllRequestsFinished(mCurrentRequest);
                }
            });
        }

        @Override
        public void onErrorResponse(final VolleyError volleyError) {
            mSingleThreadNetworkHandler.submit(new Thread() {
                @Override
                public void run() {
                    AppLog.e(T.STATS, "Error while loading Stats!");
                    StatsUtils.logVolleyErrorDetails(volleyError);
                    BaseStatsModel mResponseObjectModel = null;
                    EventBus.getDefault()
                            .post(new StatsEvents.SectionUpdateError(mEndpointName, mRequestBlogId, mTimeframe, mDate,
                                                                     mMaxResultsRequested, mPageRequested,
                                                                     volleyError));
                    updateWidgetsUI(mRequestBlogId, mEndpointName, mTimeframe, mDate, mPageRequested,
                                    mResponseObjectModel);
                    checkAllRequestsFinished(mCurrentRequest);
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
        // EventBus.getDefault().post(new StatsEvents.UpdateStatusChanged(false));
        // stopSelf(mServiceStartId);
        synchronized (mStatsNetworkRequests) {
            mStatsNetworkRequests.clear();
        }
        mCompletionListener.onCompleted(mListenerCompanion);
    }

    private void checkAllRequestsFinished(Request<JSONObject> req) {
        synchronized (mStatsNetworkRequests) {
            if (req != null) {
                mStatsNetworkRequests.remove(req);
            }
            boolean isStillWorking =
                    mStatsNetworkRequests.size() > 0 || mSingleThreadNetworkHandler.getQueue().size() > 0;
            EventBus.getDefault().post(new StatsEvents.UpdateStatusChanged(isStillWorking));
            if (!isStillWorking) {
                stopService();
            }
        }
    }

    interface ServiceCompletionListener {
        void onCompleted(Object companion);
    }
}
