package org.wordpress.android.ui.plans;

import org.wordpress.android.R;

public class PlansUIHelper {

    public static final long FREE_PLAN_ID = 1L;
    public static final long PREMIUM_PLAN_ID = 1003L;
    public static final long BUSINESS_PLAN_ID = 1008L;
    public static final long JETPACK_FREE_PLAN_ID = 2002L;
    public static final long JETPACK_PREMIUM_PLAN_ID = 2000L;
    public static final long JETPACK_BUSINESS_PLAN_ID = 2001L;

    public static final int NO_PICTURE_FOR_PLAN_RES_ID = Integer.MIN_VALUE;

    public static int getPrimaryImageResIDForPlan(long planID) {
        if (planID == FREE_PLAN_ID || planID == JETPACK_FREE_PLAN_ID) {
            return R.drawable.plan_beginner;
        }

        if (planID == PREMIUM_PLAN_ID || planID == JETPACK_PREMIUM_PLAN_ID) {
            return R.drawable.plan_premium;
        }

        if (planID == BUSINESS_PLAN_ID || planID == JETPACK_BUSINESS_PLAN_ID) {
            return R.drawable.plan_business;
        }

        return NO_PICTURE_FOR_PLAN_RES_ID;
    }

}
