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
        private final int mLocalBlogId;
        public PlansUpdated(int localBlogId, @NonNull List<SitePlan> plans) {
            mLocalBlogId = localBlogId;
            mPlans = plans;
        }
        public int getLocalBlogId() {
            return mLocalBlogId;
        }
        public List<SitePlan> getPlans() {
            return mPlans;
        }
    }

    public static class PlansUpdateFailed { }
}
