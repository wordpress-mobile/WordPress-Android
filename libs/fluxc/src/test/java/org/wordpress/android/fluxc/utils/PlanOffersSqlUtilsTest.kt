package org.wordpress.android.fluxc.utils

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PLAN_OFFERS_MODELS
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils.PlanOffersBuilder
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils.PlanOfferFeatureBuilder
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils.PlanOfferIdBuilder
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
                listOf(PlanOffersBuilder::class.java, PlanOfferIdBuilder::class.java, PlanOfferFeatureBuilder::class.java), ""
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testStoreAndRetrievePlans() {
        planOffersSqlUtils.storePlanOffers(PLAN_OFFERS_MODELS)

        val cachedPlanOffers = planOffersSqlUtils.getPlanOffers()

        assertNotNull(cachedPlanOffers)
        assertEquals(PLAN_OFFERS_MODELS, cachedPlanOffers)
    }
}
