package org.wordpress.android.support

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.wordpress.android.util.CrashLogging

class ZendeskPlanFieldHelperTest {
    private lateinit var zendeskPlanFieldHelper: ZendeskPlanFieldHelper
    private val crashLogging: CrashLogging = mock()

    @Before
    fun setUp() {
        zendeskPlanFieldHelper = ZendeskPlanFieldHelper(crashLogging)
    }

    @Test
    fun `getHighestPlan returns ecommerce if planIds includes an ecommerce plan`() {
        // Given
        val planIds = listOf(
            WpComPlansConstants.WPCOM_PERSONAL_BUNDLE,
            WpComPlansConstants.WPCOM_ECOMMERCE_BUNDLE_2Y,
            JetpackPlansConstants.JETPACK_BUSINESS,
            WpComPlansConstants.WPCOM_FREE
        )

        // Then
        Assert.assertEquals(
            ZendeskPlanConstants.ECOMMERCE,
            zendeskPlanFieldHelper.getHighestPlan(planIds)
        )
    }

    @Test
    fun `getHighestPlan returns business_professional if planIds includes business but no ecommerce plans`() {
        // Given
        val planIds = listOf(
            WpComPlansConstants.WPCOM_PERSONAL_BUNDLE,
            JetpackPlansConstants.JETPACK_BUSINESS,
            WpComPlansConstants.WPCOM_BUSINESS_BUNDLE,
            WpComPlansConstants.WPCOM_FREE
        )

        // Then
        Assert.assertEquals(
            ZendeskPlanConstants.BUSINESS_PROFESSIONAL,
            zendeskPlanFieldHelper.getHighestPlan(planIds)
        )
    }

    @Test
    fun `getHighestPlan returns premium if planIds includes premium but no ecommerce, business_professional plans`() {
        // Given
        val planIds = listOf(
            WpComPlansConstants.WPCOM_VALUE_BUNDLE,
            WpComPlansConstants.WPCOM_PERSONAL_BUNDLE,
            WpComPlansConstants.WPCOM_FREE
        )

        // Then
        Assert.assertEquals(
            ZendeskPlanConstants.PREMIUM,
            zendeskPlanFieldHelper.getHighestPlan(planIds)
        )
    }

    @Test
    fun `getHighestPlan returns personal if planIds includes a personal plan but no plan higher than it`() {
        // Given
        val planIds = listOf(
            WpComPlansConstants.WPCOM_PERSONAL_BUNDLE,
            JetpackPlansConstants.JETPACK_PERSONAL,
            WpComPlansConstants.WPCOM_FREE,
            WpComPlansConstants.WPCOM_BLOGGER_BUNDLE
        )

        // Then
        Assert.assertEquals(
            ZendeskPlanConstants.PERSONAL,
            zendeskPlanFieldHelper.getHighestPlan(planIds)
        )
    }

    @Test
    fun `getHighestPlan returns blogger if planIds includes a blogger plan but no plan higher than it`() {
        // Given
        val planIds = listOf(
            WpComPlansConstants.WPCOM_FREE,
            WpComPlansConstants.WPCOM_BLOGGER_BUNDLE,
            JetpackPlansConstants.JETPACK_FREE
        )

        // Then
        Assert.assertEquals(
            ZendeskPlanConstants.BLOGGER,
            zendeskPlanFieldHelper.getHighestPlan(planIds)
        )
    }

    @Test
    fun `getHighestPlan returns free if planIds includes a free plan but no plan higher than it`() {
        // Given
        val planIds = listOf(
            WpComPlansConstants.WPCOM_FREE,
            JetpackPlansConstants.JETPACK_FREE
        )

        // Then
        Assert.assertEquals(
            ZendeskPlanConstants.FREE,
            zendeskPlanFieldHelper.getHighestPlan(planIds)
        )
    }

    @Test
    fun `getHighestPlan returns UNKNOWN_PLAN if no matching plan id found`() {
        // Given
        val planIds = listOf(100000L)

        // Then
        Assert.assertEquals(
            UNKNOWN_PLAN,
            zendeskPlanFieldHelper.getHighestPlan(planIds)
        )
    }

    @Test
    fun `getHighestPlan logs error if unknown plan ids are found`() {
        // Given
        val planIds = listOf(
            WpComPlansConstants.WPCOM_PERSONAL_BUNDLE,
            123456789L,
            9999999L,
            JetpackPlansConstants.JETPACK_PREMIUM
        )

        // When
        zendeskPlanFieldHelper.getHighestPlan(planIds)

        // Then
        verify(crashLogging, times(1)).reportException(any<Throwable>())
    }
}
