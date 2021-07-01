package org.wordpress.android.fluxc.planoffers

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.wordpress.android.fluxc.model.plans.PlanOffersMapper
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.areSame
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.getDatabaseModel
import org.wordpress.android.fluxc.network.rest.wpcom.planoffers.getDomainModel

class PlanOffersMapperTest {
    private lateinit var mapper: PlanOffersMapper

    @Before
    fun setUp() {
        mapper = PlanOffersMapper()
    }

    @Test
    fun `model mapped to database`() {
        val domainModel = getDomainModel()

        val databaseModel = mapper.toDatabaseModel(20, domainModel)

        assertThat(areSame(domainModel, databaseModel)).isTrue
    }

    @Test
    fun `model mapped to database with empty planIds`() {
        val domainModel = getDomainModel(emptyPlanIds = true, emptyFeatures = false)

        val databaseModel = mapper.toDatabaseModel(20, domainModel)

        assertThat(areSame(domainModel, databaseModel)).isTrue
    }

    @Test
    fun `model mapped to database with empty features`() {
        val domainModel = getDomainModel(emptyPlanIds = false, emptyFeatures = true)

        val databaseModel = mapper.toDatabaseModel(20, domainModel)

        assertThat(areSame(domainModel, databaseModel)).isTrue
    }

    @Test
    fun `model mapped from database`() {
        val databaseModel = getDatabaseModel()

        val domainModel = mapper.toDomainModel(databaseModel)

        assertThat(areSame(domainModel, databaseModel)).isTrue
    }

    @Test
    fun `model mapped from database with empty planIds`() {
        val databaseModel = getDatabaseModel(emptyPlanIds = true, emptyPlanFeatures = false)

        val domainModel = mapper.toDomainModel(databaseModel)

        assertThat(areSame(domainModel, databaseModel)).isTrue
    }

    @Test
    fun `model mapped from database with empty planFatures`() {
        val databaseModel = getDatabaseModel(emptyPlanIds = false, emptyPlanFeatures = true)

        val domainModel = mapper.toDomainModel(databaseModel)

        assertThat(areSame(domainModel, databaseModel)).isTrue
    }
}
