package org.wordpress.android.ui.stats;

import com.android.volley.VolleyError;

import org.wordpress.android.ui.stats.models.AuthorsModel;
import org.wordpress.android.ui.stats.models.ClicksModel;
import org.wordpress.android.ui.stats.models.CommentFollowersModel;
import org.wordpress.android.ui.stats.models.CommentsModel;
import org.wordpress.android.ui.stats.models.GeoviewsModel;
import org.wordpress.android.ui.stats.models.ReferrersModel;
import org.wordpress.android.ui.stats.models.SearchTermsModel;
import org.wordpress.android.ui.stats.models.TopPostsAndPagesModel;
import org.wordpress.android.ui.stats.models.VideoPlaysModel;
import org.wordpress.android.ui.stats.models.VisitsModel;
import org.wordpress.android.ui.stats.service.StatsService.StatsEndpointsEnum;

import java.io.Serializable;

public class StatsEvents {
    public static class UpdateStatusChanged {
        public final boolean mUpdating;
        public UpdateStatusChanged(boolean updating) {
            mUpdating = updating;
        }
    }

    //TODO: REMOVE
    public static class SectionUpdated  extends SectionUpdatedAbstract {
        public final StatsEndpointsEnum mEndPointName;
        // TODO: replace Serializable by a POJO or use several event types (like SectionXUpdated, SectionYUpdated)
        public final Serializable mResponseObjectModel;
        public SectionUpdated(StatsEndpointsEnum endPointName, String blogId, StatsTimeframe timeframe, String date,
                              final int maxResultsRequested, final int pageRequested, Serializable responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mEndPointName = endPointName;
            mResponseObjectModel = responseObjectModel;
        }
    }

    public abstract static class SectionUpdatedAbstract {
        public final String mRequestBlogId; // This is the remote blog ID
        public final StatsTimeframe mTimeframe;
        public final String mDate;
        public final int mMaxResultsRequested, mPageRequested;

        public SectionUpdatedAbstract(String blogId, StatsTimeframe timeframe, String date,
                                      final int maxResultsRequested, final int pageRequested) {
            mRequestBlogId = blogId;
            mDate = date;
            mTimeframe = timeframe;
            mMaxResultsRequested = maxResultsRequested;
            mPageRequested = pageRequested;
        }
    }

    public static class SectionUpdateError extends SectionUpdatedAbstract {

        public final VolleyError mError;
        public final StatsEndpointsEnum mEndPointName;

        public SectionUpdateError(StatsEndpointsEnum endPointName, String blogId, StatsTimeframe timeframe, String date,
                                      final int maxResultsRequested, final int pageRequested, VolleyError error) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mEndPointName = endPointName;
            this.mError = error;
        }
    }

    public static class VisitorsAndViewsSectionUpdated extends SectionUpdatedAbstract {

        public final VisitsModel mVisitsAndViews;

        public VisitorsAndViewsSectionUpdated(String blogId, StatsTimeframe timeframe, String date,
                                      final int maxResultsRequested, final int pageRequested, VisitsModel responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mVisitsAndViews = responseObjectModel;
        }
    }

    public static class TopPostsSectionUpdated extends SectionUpdatedAbstract {

        public final TopPostsAndPagesModel mTopPostsAndPagesModel;

        public TopPostsSectionUpdated(String blogId, StatsTimeframe timeframe, String date,
                                      final int maxResultsRequested, final int pageRequested, TopPostsAndPagesModel responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mTopPostsAndPagesModel = responseObjectModel;
        }
    }

    public static class ReferrersSectionUpdated extends SectionUpdatedAbstract {

        public final ReferrersModel mReferrers;

        public ReferrersSectionUpdated(String blogId, StatsTimeframe timeframe, String date,
                                      final int maxResultsRequested, final int pageRequested, ReferrersModel responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mReferrers = responseObjectModel;
        }
    }

    public static class ClicksSectionUpdated extends SectionUpdatedAbstract {

        public final ClicksModel mClicks;

        public ClicksSectionUpdated(String blogId, StatsTimeframe timeframe, String date,
                                       final int maxResultsRequested, final int pageRequested, ClicksModel responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mClicks = responseObjectModel;
        }
    }


    public static class AuthorsSectionUpdated extends SectionUpdatedAbstract {

        public final AuthorsModel mAuthors;

        public AuthorsSectionUpdated(String blogId, StatsTimeframe timeframe, String date,
                                    final int maxResultsRequested, final int pageRequested, AuthorsModel responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mAuthors = responseObjectModel;
        }
    }

    public static class CountrySectionUpdated extends SectionUpdatedAbstract {

        public final GeoviewsModel mCountries;

        public CountrySectionUpdated(String blogId, StatsTimeframe timeframe, String date,
                                     final int maxResultsRequested, final int pageRequested, GeoviewsModel responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mCountries = responseObjectModel;
        }
    }

    public static class VideoSectionUpdated extends SectionUpdatedAbstract {

        public final VideoPlaysModel mVideos;

        public VideoSectionUpdated(String blogId, StatsTimeframe timeframe, String date,
                                     final int maxResultsRequested, final int pageRequested, VideoPlaysModel responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mVideos = responseObjectModel;
        }
    }

    public static class SearchTermsSectionUpdated extends SectionUpdatedAbstract {

        public final SearchTermsModel mSearchTerms;

        public SearchTermsSectionUpdated(String blogId, StatsTimeframe timeframe, String date,
                                   final int maxResultsRequested, final int pageRequested, SearchTermsModel responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mSearchTerms = responseObjectModel;
        }
    }

    public static class CommentsSectionUpdated extends SectionUpdatedAbstract {

        public final CommentsModel mComments;

        public CommentsSectionUpdated(String blogId, StatsTimeframe timeframe, String date,
                                         final int maxResultsRequested, final int pageRequested, CommentsModel responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mComments = responseObjectModel;
        }
    }

    public static class CommentFollowersSectionUpdated extends SectionUpdatedAbstract {

        public final CommentFollowersModel mCommentFollowers;

        public CommentFollowersSectionUpdated(String blogId, StatsTimeframe timeframe, String date,
                                      final int maxResultsRequested, final int pageRequested, CommentFollowersModel responseObjectModel) {
            super(blogId, timeframe, date, maxResultsRequested, pageRequested);
            mCommentFollowers = responseObjectModel;
        }
    }

    public static class JetpackSettingsCompleted {
        public final boolean isError;
        public JetpackSettingsCompleted(boolean isError) {
            this.isError = isError;
        }
    }

    public static class JetpackAuthError {
        public final int mLocalBlogId; // This is the local blogID

        public JetpackAuthError(int blogId) {
            mLocalBlogId = blogId;
        }
    }
}
