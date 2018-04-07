package org.wordpress.android.ui.stats.service;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;

import org.wordpress.android.WordPress;
import org.wordpress.android.ui.stats.StatsEvents;
import org.wordpress.android.ui.stats.StatsTimeframe;
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
import org.wordpress.android.ui.stats.models.VisitsModel;
import org.wordpress.android.util.AppLog;
import org.wordpress.android.util.AppLog.T;


/**
 * Background service to retrieve Stats.
 * Parsing of response(s) and submission of new network calls are done by using a ThreadPoolExecutor
 * with a single thread.
 */

public class StatsService extends Service implements StatsServiceLogic.ServiceCompletionListener {
    public static final String ARG_BLOG_ID = "blog_id";
    public static final String ARG_PERIOD = "stats_period";
    public static final String ARG_DATE = "stats_date";
    public static final String ARG_SECTION = "stats_section";
    public static final String ARG_MAX_RESULTS = "stats_max_results";
    public static final String ARG_PAGE_REQUESTED = "stats_page_requested";

    // The number of results to return per page for Paged REST endpoints. Numbers larger than 20 will
    // default to 20 on the server.
    public static final int MAX_RESULTS_REQUESTED_PER_PAGE = 20;

    private StatsServiceLogic mStatsServiceLogic;

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
                            maxResultsRequested, pageRequested,
                            (VisitsModel) data);
                case TOP_POSTS:
                    return new StatsEvents.TopPostsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (TopPostsAndPagesModel) data);
                case REFERRERS:
                    return new StatsEvents.ReferrersUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (ReferrersModel) data);
                case CLICKS:
                    return new StatsEvents.ClicksUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (ClicksModel) data);
                case AUTHORS:
                    return new StatsEvents.AuthorsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (AuthorsModel) data);
                case GEO_VIEWS:
                    return new StatsEvents.CountriesUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (GeoviewsModel) data);
                case VIDEO_PLAYS:
                    return new StatsEvents.VideoPlaysUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (VideoPlaysModel) data);
                case SEARCH_TERMS:
                    return new StatsEvents.SearchTermsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (SearchTermsModel) data);
                case COMMENTS:
                    return new StatsEvents.CommentsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (CommentsModel) data);
                case COMMENT_FOLLOWERS:
                    return new StatsEvents.CommentFollowersUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (CommentFollowersModel) data);
                case TAGS_AND_CATEGORIES:
                    return new StatsEvents.TagsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (TagsContainerModel) data);
                case PUBLICIZE:
                    return new StatsEvents.PublicizeUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested, (PublicizeModel) data);
                case FOLLOWERS_WPCOM:
                    return new StatsEvents.FollowersWPCOMUdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (FollowersModel) data);
                case FOLLOWERS_EMAIL:
                    return new StatsEvents.FollowersEmailUdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (FollowersModel) data);
                case INSIGHTS_POPULAR:
                    return new StatsEvents.InsightsPopularUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (InsightsPopularModel) data);
                case INSIGHTS_ALL_TIME:
                    return new StatsEvents.InsightsAllTimeUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (InsightsAllTimeModel) data);
                case INSIGHTS_TODAY:
                    return new StatsEvents.VisitorsAndViewsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (VisitsModel) data);
                case INSIGHTS_LATEST_POST_SUMMARY:
                    return new StatsEvents.InsightsLatestPostSummaryUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (InsightsLatestPostModel) data);
                case INSIGHTS_LATEST_POST_VIEWS:
                    return new StatsEvents.InsightsLatestPostDetailsUpdated(siteId, timeframe, date,
                            maxResultsRequested, pageRequested,
                            (InsightsLatestPostDetailsModel) data);
                default:
                    AppLog.e(T.STATS, "Can't find an Update Event that match the current endpoint: " + this.name());
            }

            return null;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        AppLog.i(T.STATS, "stats service created");
        mStatsServiceLogic = new StatsServiceLogic(this);
        mStatsServiceLogic.onCreate((WordPress) getApplication());
    }

    @Override
    public void onDestroy() {
        AppLog.i(T.STATS, "stats service destroyed");
        mStatsServiceLogic.onDestroy();
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        AppLog.i(AppLog.T.STATS, "stats service > task: " + startId + " started");
        mStatsServiceLogic.performTask(intent.getExtras(), new Integer(startId));
        return START_NOT_STICKY;
    }

    @Override
    public void onCompleted(Object companion) {
        if (companion instanceof Integer) {
            AppLog.i(AppLog.T.STATS, "stats service > task: " + companion + " completed");
            stopSelf((Integer) companion);
        } else {
            AppLog.i(AppLog.T.STATS, "stats service > task: <not identified> completed");
            stopSelf();
        }
    }
}
