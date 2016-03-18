package org.wordpress.android.ui.plans;

import android.support.annotation.NonNull;

import org.wordpress.android.ui.plans.models.Plan;

import java.util.List;

/**
 * Plan-related EventBus event classes
 */
public class PlanEvents {

    public static class PlansUpdated {
        private final List<Plan> mPlans;
        private final int mLocalBlogId;
        public PlansUpdated(int localBlogId, @NonNull List<Plan> plans) {
            mLocalBlogId = localBlogId;
            mPlans = plans;
        }
        public int getLocalBlogId() {
            return mLocalBlogId;
        }
        public List<Plan> getPlans() {
            return mPlans;
        }
    }

    public static class PlansUpdateFailed { }
}
