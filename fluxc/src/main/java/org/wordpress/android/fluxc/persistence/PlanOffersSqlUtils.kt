package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.PlanOfferFeatureTable
import com.wellsql.generated.PlanOfferIdTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import com.yarolegovich.wellsql.core.annotation.Unique
import org.wordpress.android.fluxc.model.plans.PlanOfferModel
import org.wordpress.android.fluxc.model.plans.PlanOfferModel.Feature
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanOffersSqlUtils @Inject constructor() {
    fun storePlanOffers(planOffers: List<PlanOfferModel>) {
        WellSql.delete(PlanOffersBuilder::class.java).execute()
        WellSql.delete(PlanOfferIdBuilder::class.java).execute()
        WellSql.delete(PlanOfferFeatureBuilder::class.java).execute()

        planOffers.forEachIndexed { index, planModel ->
            val planIds = planModel.planIds?.map { PlanOfferIdBuilder(internalPlanId = index, productId = it) }
            val features = planModel.features?.map { it.toBuilder(index) }
            val plan = planModel.toBuilder(index)

            WellSql.insert<PlanOffersBuilder>(plan).execute()
            WellSql.insert<PlanOfferIdBuilder>(planIds).execute()
            WellSql.insert<PlanOfferFeatureBuilder>(features).execute()
        }
    }

    fun getPlanOffers(): List<PlanOfferModel> {
        return WellSql.select(PlanOffersBuilder::class.java).asModel.mapNotNull { plan ->
            val planFeatures = WellSql.select(PlanOfferFeatureBuilder::class.java)
                    .where().equals(PlanOfferFeatureTable.INTERNAL_PLAN_ID, plan.internalPlanId).endWhere()
                    .asModel.mapNotNull {
                it.build()
            }
            val planIds = WellSql.select(PlanOfferIdBuilder::class.java)
                    .where().equals(PlanOfferIdTable.INTERNAL_PLAN_ID, plan.internalPlanId).endWhere()
                    .asModel.mapNotNull { it.build() }



            plan.build(planIds, planFeatures)
        }
    }

    private fun Feature.toBuilder(internalPlanId: Int): PlanOfferFeatureBuilder {
        return PlanOfferFeatureBuilder(
                internalPlanId = internalPlanId,
                stringId = this.id,
                name = this.name,
                description = this.description
        )
    }

    private fun PlanOfferModel.toBuilder(internalPlanId: Int): PlanOffersBuilder {
        return PlanOffersBuilder(
                internalPlanId = internalPlanId,
                name = this.name,
                shortName = this.shortName,
                tagline = this.tagline,
                description = this.description,
                icon = this.iconUrl
        )
    }

    @Table(name = "PlanOfferFeature")
    data class PlanOfferFeatureBuilder(
        @PrimaryKey @Column private var id: Int = 0,
        @Column var internalPlanId: Int = 0,
        @Column @Unique var stringId: String? = null,
        @Column var name: String? = null,
        @Column var description: String? = null
    ) : Identifiable {
        fun build(): Feature {
            return Feature(stringId, name, description)
        }

        override fun getId(): Int {
            return this.id
        }

        override fun setId(id: Int) {
            this.id = id
        }
    }

    @Table(name = "PlanOfferId")
    data class PlanOfferIdBuilder(
        @PrimaryKey @Column private var id: Int = 0,
        @Column var productId: Int = 0,
        @Column var internalPlanId: Int = 0
    ) : Identifiable {
        fun build(): Int {
            return productId
        }

        override fun getId(): Int {
            return this.id
        }

        override fun setId(id: Int) {
            this.id = id
        }
    }

    @Table(name = "PlanOffer")
    data class PlanOffersBuilder(
        @PrimaryKey @Column private var id: Int = 0,
        @Column var internalPlanId: Int = 0,

        @Column var name: String? = null,
        @Column var shortName: String? = null,
        @Column var tagline: String? = null,
        @Column var description: String? = null,
        @Column var icon: String? = null
    ) : Identifiable {
        fun build(planIds: List<Int>, planFeatures: List<Feature>): PlanOfferModel {
            return PlanOfferModel(planIds, planFeatures, name, shortName, tagline, description, icon)
        }

        override fun getId(): Int {
            return this.id
        }

        override fun setId(id: Int) {
            this.id = id
        }
    }
}
