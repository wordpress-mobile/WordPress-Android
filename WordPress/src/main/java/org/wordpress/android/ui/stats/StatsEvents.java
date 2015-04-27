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
        public final String mRequestBlogId;
        public final StatsTimeframe mTimeframe;
        public final String mDate;

        // TODO: replace Serializable by a POJO or use several event types (like SectionXUpdated, SectionYUpdated)
        public final Serializable mResponseObjectModel;
        public SectionUpdated(StatsEndpointsEnum endPointName, String blogId, StatsTimeframe timeframe, String date,
                              Serializable responseObjectModel) {
            mEndPointName = endPointName;
            mResponseObjectModel = responseObjectModel;
            mRequestBlogId = blogId;
            mDate = date;
            mTimeframe = timeframe;
        }
    }
    public static class JetpackSettingsCompleted {
        public final boolean isError;
        public JetpackSettingsCompleted(boolean isError) {
            this.isError = isError;
        }
    }
}
