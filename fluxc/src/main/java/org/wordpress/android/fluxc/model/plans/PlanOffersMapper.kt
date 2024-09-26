package org.wordpress.android.fluxc.model.plans

import dagger.Reusable
import org.wordpress.android.fluxc.model.plans.PlanOffersModel.Feature
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOffer
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOfferFeature
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOfferId
import org.wordpress.android.fluxc.persistence.PlanOffersDao.PlanOfferWithDetails
import javax.inject.Inject

@Reusable
class PlanOffersMapper @Inject constructor() {
    fun toDatabaseModel(
        internalPlanId: Int,
        domainModel: PlanOffersModel
    ): PlanOfferWithDetails = with(domainModel) {
        return PlanOfferWithDetails(
                planOffer = PlanOffer(
                        internalPlanId = internalPlanId,
                        name = this.name,
                        shortName = this.shortName,
                        tagline = this.tagline,
                        description = this.description,
                        icon = this.iconUrl
                ),
                planIds = this.planIds?.map {
                    PlanOfferId(
                            productId = it,
                            internalPlanId = internalPlanId
                    )
                } ?: emptyList(),
                planFeatures = this.features?.map {
                    PlanOfferFeature(
                            internalPlanId = internalPlanId,
                            stringId = it.id,
                            name = it.name,
                            description = it.description
                    )
                } ?: emptyList()
        )
    }

    fun toDomainModel(
        databaseModel: PlanOfferWithDetails
    ): PlanOffersModel = with(databaseModel) {
        return PlanOffersModel(
                planIds = this.planIds.map {
                    it.productId
                },
                features = this.planFeatures.map {
                    Feature(id = it.stringId, name = it.name, description = it.description)
                },
                name = this.planOffer.name,
                shortName = this.planOffer.shortName,
                tagline = this.planOffer.tagline,
                description = this.planOffer.description,
                iconUrl = this.planOffer.icon
        )
    }
}
