package org.wordpress.android.fluxc.planoffers

import org.junit.Test
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PLAN_OFFER_MODELS
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PlanOffersModelTest {
    @Test
    fun testRevisionModelEquals() {
        val samplePlanOfferModel1 = PLAN_OFFER_MODELS[0]
        val samplePlanOfferModel2 = PLAN_OFFER_MODELS[0].copy()

        assertEquals(samplePlanOfferModel1, samplePlanOfferModel2)
        assertEquals(samplePlanOfferModel1.hashCode(), samplePlanOfferModel2.hashCode())

        samplePlanOfferModel2.description = "mismatched description"
        assertNotEquals(samplePlanOfferModel1, samplePlanOfferModel2)
        assertNotEquals(samplePlanOfferModel1.hashCode(), samplePlanOfferModel2.hashCode())
    }
}
