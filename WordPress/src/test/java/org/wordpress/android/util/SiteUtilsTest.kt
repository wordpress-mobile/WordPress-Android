package org.wordpress.android.util

import org.assertj.core.api.Assertions.assertThat
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

    @Test
    fun `checkMinimalJetpackVersion doesnt fail when Jetpack version is false`() {
        val site = SiteModel()
        site.jetpackVersion = "false"

        val hasMinimalJetpackVersion = SiteUtils.checkMinimalJetpackVersion(site, "0")

        assertThat(hasMinimalJetpackVersion).isFalse()
    }

    @Test
    fun `checkMinimalJetpackVersion returns true when version higher than input`() {
        val site = SiteModel()
        site.jetpackVersion = "5.8"
        site.origin = 1
        site.setIsJetpackConnected(true)

        val hasMinimalJetpackVersion = SiteUtils.checkMinimalJetpackVersion(site, "5.6")

        assertThat(hasMinimalJetpackVersion).isTrue()
    }

    @Test
    fun `checkMinimalJetpackVersion returns true when version is equal to input`() {
        val site = SiteModel()
        site.jetpackVersion = "5.8"
        site.origin = 1
        site.setIsJetpackConnected(true)

        val hasMinimalJetpackVersion = SiteUtils.checkMinimalJetpackVersion(site, "5.8")

        assertThat(hasMinimalJetpackVersion).isTrue()
    }

    @Test
    fun `checkMinimalJetpackVersion returns false when version is lower than input`() {
        val site = SiteModel()
        site.jetpackVersion = "5.8"
        site.origin = 1
        site.setIsJetpackConnected(true)

        val hasMinimalJetpackVersion = SiteUtils.checkMinimalJetpackVersion(site, "5.9")

        assertThat(hasMinimalJetpackVersion).isFalse()
    }

    @Test
    fun `checkMinimalJetpackVersion returns false when origin not WPCOM`() {
        val site = SiteModel()
        site.jetpackVersion = "5.8"
        site.origin = 0

        val hasMinimalJetpackVersion = SiteUtils.checkMinimalJetpackVersion(site, "5.6")

        assertThat(hasMinimalJetpackVersion).isFalse()
    }

    @Test
    fun `checkMinimalJetpackVersion returns false when isWpCom is false`() {
        val site = SiteModel()
        site.jetpackVersion = "5.8"
        site.setIsWPCom(false)

        val hasMinimalJetpackVersion = SiteUtils.checkMinimalJetpackVersion(site, "5.6")

        assertThat(hasMinimalJetpackVersion).isFalse()
    }
}
