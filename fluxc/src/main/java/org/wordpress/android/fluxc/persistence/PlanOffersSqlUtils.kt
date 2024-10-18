package org.wordpress.android.fluxc.persistence

import org.wordpress.android.fluxc.model.plans.PlanOffersMapper
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanOffersSqlUtils @Inject constructor(
    private val planOffersDao: PlanOffersDao,
    private val planOffersMapper: PlanOffersMapper
) {
    @Suppress("SpreadOperator")
    fun storePlanOffers(planOffers: List<PlanOffersModel>) {
        planOffersDao.clearPlanOffers()

        planOffersDao.insertPlanOfferWithDetails(
                *(planOffers.mapIndexed { index, planOffersModel ->
                    planOffersMapper.toDatabaseModel(index, planOffersModel)
                }).toTypedArray()
        )
    }

    fun getPlanOffers(): List<PlanOffersModel> {
        return planOffersDao.getPlanOfferWithDetails().map { planOffersMapper.toDomainModel(it) }
    }
}
