package org.wordpress.android.ui.plans;

import android.support.annotation.NonNull;

import org.wordpress.android.ui.plans.models.SitePlan;

import java.util.List;

/**
 * Plan-related EventBus event classes
 */
public class PlanEvents {

    public static class PlansUpdated {
        private final List<SitePlan> mPlans;
        public PlansUpdated(@NonNull List<SitePlan> plans) {
            mPlans = plans;
        }
        public List<SitePlan> getPlans() {
            return mPlans;
        }
    }

    public static class PlansUpdateFailed { }
}
