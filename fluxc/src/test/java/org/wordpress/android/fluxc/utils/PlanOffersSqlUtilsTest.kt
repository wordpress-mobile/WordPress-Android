package org.wordpress.android.fluxc.utils

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PLAN_OFFER_MODELS
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils.PlanOffersBuilder
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils.PlanOffersFeatureBuilder
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils.PlanOffersIdBuilder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class PlanOffersSqlUtilsTest {
    private lateinit var planOffersSqlUtils: PlanOffersSqlUtils

    @Before
    fun setUp() {
        planOffersSqlUtils = PlanOffersSqlUtils()

        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(
                        PlanOffersBuilder::class.java,
                        PlanOffersIdBuilder::class.java,
                        PlanOffersFeatureBuilder::class.java
                ), ""
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testStoringAndRetrievingPlanOffers() {
        planOffersSqlUtils.storePlanOffers(PLAN_OFFER_MODELS)

        val cachedPlanOffers = planOffersSqlUtils.getPlanOffers()

        assertNotNull(cachedPlanOffers)
        assertEquals(PLAN_OFFER_MODELS, cachedPlanOffers)
    }
}
