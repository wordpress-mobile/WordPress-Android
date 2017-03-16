package org.wordpress.android.ui.plans;

import android.support.annotation.NonNull;

import org.wordpress.android.fluxc.model.SiteModel;
import org.wordpress.android.ui.plans.models.Plan;

import java.util.List;

/**
 * Plan-related EventBus event classes
 */
class PlanEvents {

    public static class PlansUpdated {
        private final List<Plan> mPlans;
        private final SiteModel mSite;
        public PlansUpdated(SiteModel site, @NonNull List<Plan> plans) {
            mSite = site;
            mPlans = plans;
        }
        public SiteModel getSite() {
            return mSite;
        }
        public List<Plan> getPlans() {
            return mPlans;
        }
    }

    public static class PlansUpdateFailed { }
}
