package org.wordpress.android.util

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.ui.plans.PlansConstants.BLOGGER_PLAN_ONE_YEAR_ID
import org.wordpress.android.ui.plans.PlansConstants.BLOGGER_PLAN_TWO_YEARS_ID
import org.wordpress.android.ui.plans.PlansConstants.FREE_PLAN_ID
import org.wordpress.android.ui.plans.PlansConstants.PREMIUM_PLAN_ID

class SiteUtilsTest {
    @Test
    fun `onFreePlan returns true when site is on free plan`() {
        val site = SiteModel()
        site.planId = FREE_PLAN_ID

        assertTrue(SiteUtils.onFreePlan(site))

        site.planId = PREMIUM_PLAN_ID
        assertFalse(SiteUtils.onFreePlan(site))
    }

    @Test
    fun `onBloggerPlan returns true when site is on blogger plan`() {
        val site = SiteModel()
        site.planId = BLOGGER_PLAN_ONE_YEAR_ID

        assertTrue(SiteUtils.onBloggerPlan(site))

        site.planId = BLOGGER_PLAN_TWO_YEARS_ID
        assertTrue(SiteUtils.onBloggerPlan(site))

        site.planId = FREE_PLAN_ID
        assertFalse(SiteUtils.onBloggerPlan(site))
    }

    @Test
    fun `hasCustomDomain returns true when site has custom domain`() {
        val site = SiteModel()
        site.url = "http://wordpress.com"

        assertTrue(SiteUtils.hasCustomDomain(site))

        site.url = "https://***.wordpress.com"
        assertFalse(SiteUtils.hasCustomDomain(site))
    }
}
