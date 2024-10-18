package org.wordpress.android.fluxc.persistence

import androidx.room.Room
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.wordpress.android.fluxc.model.plans.PlanOffersMapper
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.areSame
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.getDatabaseModel

@RunWith(RobolectricTestRunner::class)
class PlanOffersDaoTest {
    private lateinit var database: WPAndroidDatabase
    private lateinit var planOffersDao: PlanOffersDao
    private lateinit var mapper: PlanOffersMapper

    @Before
    fun setUp() {
        val appContext = RuntimeEnvironment.application.applicationContext

        database = Room.inMemoryDatabaseBuilder(
                appContext,
                WPAndroidDatabase::class.java
        ).allowMainThreadQueries().build()

        planOffersDao = database.planOffersDao()
        mapper = PlanOffersMapper()
    }

    @Test
    fun `dao inserted data are correct`() {
        val databaseModel = getDatabaseModel()

        planOffersDao.insertPlanOfferWithDetails(databaseModel)

        val domainModelsFromCache = planOffersDao.getPlanOfferWithDetails()

        assertThat(domainModelsFromCache).hasSize(1)
        assertThat(areSame(mapper.toDomainModel(domainModelsFromCache[0]), databaseModel)).isTrue
    }
}
