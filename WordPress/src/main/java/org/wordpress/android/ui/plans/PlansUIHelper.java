package org.wordpress.android.ui.plans;

import org.wordpress.android.R;

public class PlansUIHelper {

    public static final int NO_PICTURE_FOR_PLAN_RES_ID = Integer.MIN_VALUE;

    public static int getPrimaryImageResIDForPlan(long planID) {
        if (planID == PlansConstants.FREE_PLAN_ID || planID == PlansConstants.JETPACK_FREE_PLAN_ID) {
            return R.drawable.plan_beginner;
        }

        if (planID == PlansConstants.PREMIUM_PLAN_ID || planID == PlansConstants.JETPACK_PREMIUM_PLAN_ID) {
            return R.drawable.plan_premium;
        }

        if (planID == PlansConstants.BUSINESS_PLAN_ID || planID == PlansConstants.JETPACK_BUSINESS_PLAN_ID) {
            return R.drawable.plan_business;
        }

        return NO_PICTURE_FOR_PLAN_RES_ID;
    }

}
