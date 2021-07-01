package org.wordpress.android.fluxc.utils

import androidx.room.Room
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.model.plans.PlanOffersMapper
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.PLAN_OFFER_MODELS
import org.wordpress.android.fluxc.persistence.PlanOffersDao
import org.wordpress.android.fluxc.persistence.PlanOffersSqlUtils
import org.wordpress.android.fluxc.persistence.WPAndroidDatabase
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(RobolectricTestRunner::class)
class PlanOffersSqlUtilsTest {
    private lateinit var planOffersDao: PlanOffersDao
    private lateinit var planOffersMapper: PlanOffersMapper

    private lateinit var planOffersSqlUtils: PlanOffersSqlUtils
    private lateinit var database: WPAndroidDatabase

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext

        database = Room.inMemoryDatabaseBuilder(
                appContext,
                WPAndroidDatabase::class.java
        ).allowMainThreadQueries().build()

        planOffersDao = database.planOffersDao()
        planOffersMapper = PlanOffersMapper()

        planOffersSqlUtils = PlanOffersSqlUtils(planOffersDao, planOffersMapper)
    }

    @Test
    fun testStoringAndRetrievingPlanOffers() {
        planOffersSqlUtils.storePlanOffers(PLAN_OFFER_MODELS)

        val cachedPlanOffers = planOffersSqlUtils.getPlanOffers()

        assertNotNull(cachedPlanOffers)
        assertEquals(PLAN_OFFER_MODELS, cachedPlanOffers)
    }
}
