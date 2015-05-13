package org.wordpress.android.ui.stats;

import org.wordpress.android.ui.stats.service.StatsService.StatsEndpointsEnum;

import java.io.Serializable;

public class StatsEvents {
    public static class UpdateStatusChanged {
        public final boolean mUpdating;
        public UpdateStatusChanged(boolean updating) {
            mUpdating = updating;
        }
    }
    public static class SectionUpdated {
        public final StatsEndpointsEnum mEndPointName;
        public final String mRequestBlogId; // This is the remote blog ID
        public final StatsTimeframe mTimeframe;
        public final String mDate;
        public final int mMaxResultsRequested, mPageRequested;

        // TODO: replace Serializable by a POJO or use several event types (like SectionXUpdated, SectionYUpdated)
        public final Serializable mResponseObjectModel;
        public SectionUpdated(StatsEndpointsEnum endPointName, String blogId, StatsTimeframe timeframe, String date,
                              final int maxResultsRequested, final int pageRequested, Serializable responseObjectModel) {
            mEndPointName = endPointName;
            mResponseObjectModel = responseObjectModel;
            mRequestBlogId = blogId;
            mDate = date;
            mTimeframe = timeframe;
            mMaxResultsRequested = maxResultsRequested;
            mPageRequested = pageRequested;
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
