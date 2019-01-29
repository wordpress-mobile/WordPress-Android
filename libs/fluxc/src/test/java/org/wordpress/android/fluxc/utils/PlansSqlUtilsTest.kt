package org.wordpress.android.fluxc.utils

import com.yarolegovich.wellsql.WellSql
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.SingleStoreWellSqlConfigForTests
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PLAN_MODELS
import org.wordpress.android.fluxc.persistence.PlansSqlUtils
import org.wordpress.android.fluxc.persistence.PlansSqlUtils.PlanBuilder
import org.wordpress.android.fluxc.persistence.PlansSqlUtils.PlanFeatureBuilder
import org.wordpress.android.fluxc.persistence.PlansSqlUtils.PlanIdBuilder
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class PlansSqlUtilsTest {
    private lateinit var plansSqlUtils: PlansSqlUtils

    @Before
    fun setUp() {
        plansSqlUtils = PlansSqlUtils()

        val appContext = RuntimeEnvironment.application.applicationContext
        val config = SingleStoreWellSqlConfigForTests(
                appContext,
                listOf(PlanBuilder::class.java, PlanIdBuilder::class.java, PlanFeatureBuilder::class.java), ""
        )
        WellSql.init(config)
        config.reset()
    }

    @Test
    fun testStoreAndRetrievePlans() {
        plansSqlUtils.storePlans(PLAN_MODELS)

        val cachedPlans = plansSqlUtils.getPlans()

        assertNotNull(cachedPlans)
        assertEquals(PLAN_MODELS, cachedPlans)
    }
}
