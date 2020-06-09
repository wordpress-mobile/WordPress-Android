package org.wordpress.android.support

class ZendeskPlanFieldHelper {
    // wpcom plans
    private val wpComEcommercePlans = listOf(
        WpComPlansConstants.WPCOM_ECOMMERCE_BUNDLE,
        WpComPlansConstants.WPCOM_ECOMMERCE_BUNDLE_2Y
    )

    private val wpComBusinessPlans = listOf(
        WpComPlansConstants.WPCOM_BUSINESS_BUNDLE,
        WpComPlansConstants.WPCOM_BUSINESS_BUNDLE_MONTHLY,
        WpComPlansConstants.WPCOM_BUSINESS_BUNDLE_2Y
    )

    private val wpComPremiumPlans = listOf(
        WpComPlansConstants.WPCOM_PRO_BUNDLE,
        WpComPlansConstants.WPCOM_VALUE_BUNDLE,
        WpComPlansConstants.WPCOM_VALUE_BUNDLE_MONTHLY,
        WpComPlansConstants.WPCOM_VALUE_BUNDLE_2Y
    )

    private val wpComPersonalPlans = listOf(
        WpComPlansConstants.WPCOM_PERSONAL_BUNDLE,
        WpComPlansConstants.WPCOM_PERSONAL_BUNDLE_2Y
    )

    private val wpComBloggerPlans = listOf(
        WpComPlansConstants.WPCOM_BLOGGER_BUNDLE,
        WpComPlansConstants.WPCOM_BLOGGER_BUNDLE_2Y
    )

    // jetpack plans
    private val jetpackBusinessPlans = listOf(
        JetpackPlansConstants.JETPACK_BUSINESS,
        JetpackPlansConstants.JETPACK_BUSINESS_MONTHLY
    )

    private val jetpackPremiumPlans = listOf(
        JetpackPlansConstants.JETPACK_PREMIUM,
        JetpackPlansConstants.JETPACK_PREMIUM_MONTHLY
    )

    private val jetpackPersonalPlans = listOf(
        JetpackPlansConstants.JETPACK_PERSONAL,
        JetpackPlansConstants.JETPACK_PERSONAL_MONTHLY
    )
    private val ecommercePlans: List<Long> = wpComEcommercePlans
    private val businessOrProfessionalPlans: List<Long> = wpComBusinessPlans + jetpackBusinessPlans
    private val premiumPlans: List<Long> = wpComPremiumPlans + jetpackPremiumPlans
    private val personalPlans: List<Long> = wpComPersonalPlans + jetpackPersonalPlans
    private val bloggerPlans: List<Long> = wpComBloggerPlans

    /**
     * This is a helper function that checks plan types from most expensive to least,
     * so that we return the highest value plan type for the user and give them the appropriate
     * service level (in case they have more than one plan).
     * Internal Ref: p8wKgj-1eQ#comment-7475
     */
    fun getHighestPlan(planIds: List<Long>): String {
        return when {
            ecommercePlans.intersect(planIds).isNotEmpty() -> {
                ZendeskPlanConstants.ECOMMERCE
            }
            businessOrProfessionalPlans.intersect(planIds).isNotEmpty() -> {
                ZendeskPlanConstants.BUSINESS_PROFESSIONAL
            }
            premiumPlans.intersect(planIds).isNotEmpty() -> {
                ZendeskPlanConstants.PREMIUM
            }
            personalPlans.intersect(planIds).isNotEmpty() -> {
                ZendeskPlanConstants.PERSONAL
            }
            bloggerPlans.intersect(planIds).isNotEmpty() -> {
                ZendeskPlanConstants.BLOGGER
            }
            else -> {
                ZendeskPlanConstants.FREE
            }
        }
    }
}

object WpComPlansConstants {
    const val WPCOM_BLOGGER_BUNDLE = 1010L
    const val WPCOM_PERSONAL_BUNDLE = 1009L
    const val WPCOM_VALUE_BUNDLE = 1003L
    const val WPCOM_PRO_BUNDLE = 1004L
    const val WPCOM_BUSINESS_BUNDLE = 1008L
    const val WPCOM_ECOMMERCE_BUNDLE = 1011L
    const val WPCOM_VALUE_BUNDLE_MONTHLY = 1013L
    const val WPCOM_BUSINESS_BUNDLE_MONTHLY = 1018L
    const val WPCOM_PERSONAL_BUNDLE_2Y = 1029L
    const val WPCOM_VALUE_BUNDLE_2Y = 1023L
    const val WPCOM_BUSINESS_BUNDLE_2Y = 1028L
    const val WPCOM_ECOMMERCE_BUNDLE_2Y = 1031L
    const val WPCOM_BLOGGER_BUNDLE_2Y = 1030L
}

object JetpackPlansConstants {
    const val JETPACK_PREMIUM = 2000L
    const val JETPACK_BUSINESS = 2001L
    const val JETPACK_PERSONAL = 2005L
    const val JETPACK_PREMIUM_MONTHLY = 2003L
    const val JETPACK_BUSINESS_MONTHLY = 2004L
    const val JETPACK_PERSONAL_MONTHLY = 2006L
}

object ZendeskPlanConstants {
    const val ECOMMERCE = "ecommerce"
    const val BUSINESS_PROFESSIONAL = "business_professional"
    const val PREMIUM = "premium"
    const val PERSONAL = "personal"
    const val BLOGGER = "blogger"
    const val FREE = "free"
}
