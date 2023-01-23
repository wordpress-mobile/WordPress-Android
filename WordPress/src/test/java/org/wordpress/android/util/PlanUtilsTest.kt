package org.wordpress.android.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.ui.plans.getCurrentPlan
import org.wordpress.android.ui.plans.isDomainCreditAvailable

class PlanUtilsTest {
    @Test
    fun `getCurrentPlan returns the current plan`() {
        val plans = listOf(
            PlanModel(
                1,
                "product-1",
                "Product 1",
                isCurrentPlan = false,
                hasDomainCredit = false
            ),
            PlanModel(
                2,
                "product-2",
                "Product 2",
                isCurrentPlan = true,
                hasDomainCredit = false
            )
        )

        assertEquals(2, getCurrentPlan(plans)?.productId)
    }

    @Test
    fun `getCurrentPlan returns null when there are no current plans`() {
        val plans = listOf(
            PlanModel(
                1,
                "product-1",
                "Product 1",
                isCurrentPlan = false,
                hasDomainCredit = false
            ),
            PlanModel(
                2,
                "product-2",
                "Product 2",
                isCurrentPlan = false,
                hasDomainCredit = false
            )
        )

        assertNull(getCurrentPlan(plans))
    }

    @Test
    fun `getCurrentPlan returns null when there are no plans`() {
        assertNull(getCurrentPlan(listOf()))
    }

    @Test
    fun `getCurrentPlan returns null when plans is null`() {
        assertNull(getCurrentPlan(null))
    }

    @Test
    fun `isDomainCreditAvailable returns true when there is a current plan with domain credit available`() {
        val plans = listOf(
            PlanModel(
                1,
                "product-1",
                "Product 1",
                isCurrentPlan = false,
                hasDomainCredit = false
            ),
            PlanModel(
                2,
                "product-2",
                "Product 2",
                isCurrentPlan = true,
                hasDomainCredit = true
            )
        )

        assert(isDomainCreditAvailable(plans))
    }

    @Test
    fun `isDomainCreditAvailable return false when current plan has no domain credit`() {
        val plans = listOf(
            PlanModel(
                1,
                "product-1",
                "Product 1",
                isCurrentPlan = false,
                hasDomainCredit = false
            ),
            PlanModel(
                2,
                "product-2",
                "Product 2",
                isCurrentPlan = true,
                hasDomainCredit = false
            )
        )

        assertFalse(isDomainCreditAvailable(plans))
    }

    @Test
    fun `isDomainCreditAvailable returns false when there are no current plans`() {
        val plans = listOf(
            PlanModel(
                1,
                "product-1",
                "Product 1",
                isCurrentPlan = false,
                hasDomainCredit = false
            ),
            PlanModel(
                2,
                "product-2",
                "Product 2",
                isCurrentPlan = false,
                hasDomainCredit = true
            )
        )

        assertFalse(isDomainCreditAvailable(plans))
    }

    @Test
    fun `isDomainCreditAvailable returns false when there are no plans`() {
        assertFalse(isDomainCreditAvailable(listOf()))
    }

    @Test
    fun `isDomainCreditAvailable returns false when plans is null`() {
        assertFalse(isDomainCreditAvailable(null))
    }
}
