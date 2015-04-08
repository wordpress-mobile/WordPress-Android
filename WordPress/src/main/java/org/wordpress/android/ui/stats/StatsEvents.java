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
        // TODO: replace Serializable by a POJO or use several event types (like SectionXUpdated, SectionYUpdated)
        public final Serializable mResponseObjectModel;
        public SectionUpdated(StatsEndpointsEnum endPointName, Serializable responseObjectModel) {
            mEndPointName = endPointName;
            mResponseObjectModel = responseObjectModel;
        }
    }
}
