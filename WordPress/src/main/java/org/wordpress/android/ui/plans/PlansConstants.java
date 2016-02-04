package org.wordpress.android.ui.plans;


public class PlansConstants {
    // This constant is used to set a plan id on newly created blog. Note that a refresh of the blog
    // is started immediately after, so even if we decide to offer premium for all on new blogs
    // the app will be in synch after a while.
    public static final long DEFAULT_PLAN_ID_FOR_NEW_BLOG = 1L;
}
