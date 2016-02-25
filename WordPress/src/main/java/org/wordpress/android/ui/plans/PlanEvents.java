package org.wordpress.android.ui.plans;

import android.support.annotation.NonNull;

import org.wordpress.android.ui.plans.models.SitePlanList;

/**
 * Plan-related EventBus event classes
 */
class PlanEvents {

    public static class PlansUpdated {
        private final SitePlanList mSitePlans;
        public PlansUpdated(@NonNull SitePlanList sitePlans) {
            mSitePlans = sitePlans;
        }
        public SitePlanList getSitePlans() {
            return mSitePlans;
        }
    }

    public static class PlansUpdateFailed { }
}
