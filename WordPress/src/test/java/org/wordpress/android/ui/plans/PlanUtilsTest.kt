package org.wordpress.android.ui.plans

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import org.wordpress.android.fluxc.model.PlanModel

class PlanUtilsTest {
    @Test
    fun `get current plan returns the current plan`() {
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
    fun `get current plan returns null when there are no current plans`() {
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
    fun `get current plan returns null when there are no plans`() {
        assertNull(getCurrentPlan(listOf()))
    }

    @Test
    fun `get current plan returns null when plans is null`() {
        assertNull(getCurrentPlan(null))
    }

    @Test
    fun `is domain credit available returns true when there is a current plan with domain credit available`() {
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
    fun `is domain credit available return false when current plan has no domain credit`() {
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
    fun `is domain credit available returns false when there are no current plans`() {
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
    fun `is domain credit available returns false when there are no plans`() {
        assertFalse(isDomainCreditAvailable(listOf()))
    }

    @Test
    fun `is domain credit available returns false when plans is null`() {
        assertFalse(isDomainCreditAvailable(null))
    }
}
