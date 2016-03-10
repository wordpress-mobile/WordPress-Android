package org.wordpress.android.ui.plans;


public class PlansConstants {
    // This constant is used to set a plan id on newly created blog. Note that a refresh of the blog
    // is started immediately after, so even if we decide to offer premium for all on new blogs
    // the app will be in sync after a while.
    public static final long DEFAULT_PLAN_ID_FOR_NEW_BLOG = 1L;

    public static final long FREE_PLAN_ID = 1L;
    public static final long PREMIUM_PLAN_ID = 1003L;
    public static final long BUSINESS_PLAN_ID = 1008L;

    public static final long JETPACK_FREE_PLAN_ID = 2002L;
    public static final long JETPACK_PREMIUM_PLAN_ID = 2000L;
    public static final long JETPACK_BUSINESS_PLAN_ID = 2001L;
}
