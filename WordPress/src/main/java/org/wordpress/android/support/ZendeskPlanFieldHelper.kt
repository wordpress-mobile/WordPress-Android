package org.wordpress.android.support

class ZendeskPlanFieldHelper {
    fun getHighestPlan(planIds: List<Long>): String {
        return ZendeskPlanConstants.FREE
    }
}

object ZendeskPlanConstants {
    const val ECOMMERCE = "ecommerce"
    const val BUSINESS_PROFESSIONAL = "business_professional"
    const val PREMIUM = "premium"
    const val PERSONAL = "personal"
    const val BLOGGER = "blogger"
    const val FREE = "free"
}
