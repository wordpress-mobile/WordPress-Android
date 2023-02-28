package org.wordpress.android.util

import org.assertj.core.api.Assertions.assertThat
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.store.SiteStore
import org.wordpress.android.ui.jetpackoverlay.JetpackFeatureRemovalPhaseHelper
import org.wordpress.android.ui.plans.PlansConstants.BLOGGER_PLAN_ONE_YEAR_ID
import org.wordpress.android.ui.plans.PlansConstants.BLOGGER_PLAN_TWO_YEARS_ID
import org.wordpress.android.ui.plans.PlansConstants.FREE_PLAN_ID
import org.wordpress.android.ui.plans.PlansConstants.PREMIUM_PLAN_ID
import org.wordpress.android.util.image.BlavatarShape.CIRCULAR
import org.wordpress.android.util.image.BlavatarShape.SQUARE
import org.wordpress.android.util.image.BlavatarShape.SQUARE_WITH_ROUNDED_CORNERES
import org.wordpress.android.util.image.ImageType.BLAVATAR
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR
import org.wordpress.android.util.image.ImageType.BLAVATAR_ROUNDED_CORNERS
import org.wordpress.android.util.image.ImageType.P2_BLAVATAR
import org.wordpress.android.util.image.ImageType.P2_BLAVATAR_CIRCULAR
import org.wordpress.android.util.image.ImageType.P2_BLAVATAR_ROUNDED_CORNERS

@RunWith(MockitoJUnitRunner::class)
class SiteUtilsTest {
    @Mock
    private lateinit var jetpackFeatureRemovalPhaseHelper: JetpackFeatureRemovalPhaseHelper

    @Mock
    private lateinit var siteStore: SiteStore

    @Test
    fun `onFreePlan returns true when site is on free plan`() {
        val site = SiteModel()
        site.planId = FREE_PLAN_ID

        assertTrue(SiteUtils.onFreePlan(site))

        site.planId = PREMIUM_PLAN_ID
        assertFalse(SiteUtils.onFreePlan(site))
    }

    @Test
    fun `hasSiteWithPaidPlan returns true when site is on a paid plan`() {
        val site1 = SiteModel()
        val site2 = SiteModel()
        whenever(siteStore.sites).thenReturn(listOf(site1, site2))

        site1.hasFreePlan = true
        site2.hasFreePlan = true
        site1.planId = FREE_PLAN_ID
        site2.planId = FREE_PLAN_ID
        assertFalse(SiteUtils.hasSiteWithPaidPlan(siteStore))

        site2.hasFreePlan = false
        site2.planId = BLOGGER_PLAN_ONE_YEAR_ID
        assertTrue(SiteUtils.hasSiteWithPaidPlan(siteStore))
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

    @Test
    fun `checkMinimalWordPressVersion returns true when software version is higher than the minimal version`() {
        val minVersion = "5.5"

        val site = SiteModel()
        site.softwareVersion = "5.6"

        val hasMinimalWordPressVersion = SiteUtils.checkMinimalWordPressVersion(site, minVersion)

        assertThat(hasMinimalWordPressVersion).isTrue
    }

    @Test
    fun `checkMinimalWordPressVersion returns true when software version is equal to the minimal version`() {
        val minVersion = "5.5"

        val site = SiteModel()
        site.softwareVersion = "5.5"

        val hasMinimalWordPressVersion = SiteUtils.checkMinimalWordPressVersion(site, minVersion)

        assertThat(hasMinimalWordPressVersion).isTrue
    }

    @Test
    fun `checkMinimalWordPressVersion returns false when software version is lower than the minimal version`() {
        val minVersion = "5.5"

        val site = SiteModel()
        site.softwareVersion = "5.4"

        val hasMinimalWordPressVersion = SiteUtils.checkMinimalWordPressVersion(site, minVersion)

        assertThat(hasMinimalWordPressVersion).isFalse
    }

    @Test
    fun `isAccessedViaWPComRest return false when origin is not wpcom rest`() {
        val site = SiteModel()
        site.origin = SiteModel.ORIGIN_XMLRPC

        val isAccessedViaWPComRest = SiteUtils.isAccessedViaWPComRest(site)

        assertThat(isAccessedViaWPComRest).isFalse()
    }

    @Test
    fun `isAccessedViaWPComRest return true when origin is wpcom rest`() {
        val site = SiteModel()
        site.origin = SiteModel.ORIGIN_WPCOM_REST

        val isAccessedViaWPComRest = SiteUtils.isAccessedViaWPComRest(site)

        assertThat(isAccessedViaWPComRest).isTrue()
    }

    @Test
    fun `supportsStoriesFeature returns true when origin is wpcom rest`() {
        val site = SiteModel().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
            setIsWPCom(true)
        }

        val supportsStoriesFeature = SiteUtils.supportsStoriesFeature(site, jetpackFeatureRemovalPhaseHelper)

        assertTrue(supportsStoriesFeature)
    }

    @Test
    fun `supportsStoriesFeature returns true when Jetpack site meets requirement`() {
        val site = initJetpackSite().apply {
            jetpackVersion = SiteUtils.WP_STORIES_JETPACK_VERSION
        }

        val supportsStoriesFeature = SiteUtils.supportsStoriesFeature(site, jetpackFeatureRemovalPhaseHelper)

        assertTrue(supportsStoriesFeature)
    }

    @Test
    fun `supportsStoriesFeature returns false when Jetpack site does not meet requirement`() {
        val site = initJetpackSite().apply {
            jetpackVersion = (SiteUtils.WP_STORIES_JETPACK_VERSION.toFloat() - 1).toString()
        }

        val supportsStoriesFeature = SiteUtils.supportsStoriesFeature(site, jetpackFeatureRemovalPhaseHelper)

        assertFalse(supportsStoriesFeature)
    }

    @Test
    fun `supportsStoriesFeature returns false when Jetpack features are removed`() {
        val site = SiteModel().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
            setIsWPCom(true)
        }
        whenever(jetpackFeatureRemovalPhaseHelper.shouldRemoveJetpackFeatures()).thenReturn(true)

        val supportsStoriesFeature = SiteUtils.supportsStoriesFeature(site, jetpackFeatureRemovalPhaseHelper)

        assertFalse(supportsStoriesFeature)
    }

    @Test
    fun `getSiteIconType returns correct value for p2 and regular sites`() {
        val squareP2Image = SiteUtils.getSiteImageType(true, SQUARE)
        assertThat(squareP2Image).isEqualTo(P2_BLAVATAR)

        val roundedCornersP2Image = SiteUtils.getSiteImageType(true, SQUARE_WITH_ROUNDED_CORNERES)
        assertThat(roundedCornersP2Image).isEqualTo(P2_BLAVATAR_ROUNDED_CORNERS)

        val circularP2Image = SiteUtils.getSiteImageType(true, CIRCULAR)
        assertThat(circularP2Image).isEqualTo(P2_BLAVATAR_CIRCULAR)

        val squareSiteImage = SiteUtils.getSiteImageType(false, SQUARE)
        assertThat(squareSiteImage).isEqualTo(BLAVATAR)

        val roundedCornersSiteImage = SiteUtils.getSiteImageType(false, SQUARE_WITH_ROUNDED_CORNERES)
        assertThat(roundedCornersSiteImage).isEqualTo(BLAVATAR_ROUNDED_CORNERS)

        val circularSiteImage = SiteUtils.getSiteImageType(false, CIRCULAR)
        assertThat(circularSiteImage).isEqualTo(BLAVATAR_CIRCULAR)
    }

    private fun initJetpackSite(): SiteModel {
        return SiteModel().apply {
            origin = SiteModel.ORIGIN_WPCOM_REST
            setIsJetpackInstalled(true)
            setIsJetpackConnected(true)
        }
    }
}
