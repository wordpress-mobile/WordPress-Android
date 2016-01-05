package org.wordpress.android.ui.stats;

import com.android.volley.VolleyError;

import org.wordpress.android.ui.stats.models.ReferrersModel;
import org.wordpress.android.ui.stats.models.TopPostsAndPagesModel;
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
