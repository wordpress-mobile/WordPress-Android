package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.PlanOffersFeatureTable
import com.wellsql.generated.PlanOffersIdTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import org.wordpress.android.fluxc.model.plans.PlanOffersModel
import org.wordpress.android.fluxc.model.plans.PlanOffersModel.Feature
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlanOffersSqlUtils @Inject constructor() {
    fun storePlanOffers(planOffers: List<PlanOffersModel>) {
        WellSql.delete(PlanOffersBuilder::class.java).execute()
        WellSql.delete(PlanOffersIdBuilder::class.java).execute()
        WellSql.delete(PlanOffersFeatureBuilder::class.java).execute()

        planOffers.forEachIndexed { index, planModel ->
            val planIds = planModel.planIds?.map { PlanOffersIdBuilder(internalPlanId = index, productId = it) }
            val features = planModel.features?.map { it.toBuilder(index) }
            val plan = planModel.toBuilder(index)

            WellSql.insert<PlanOffersBuilder>(plan).execute()
            WellSql.insert<PlanOffersIdBuilder>(planIds).execute()
            WellSql.insert<PlanOffersFeatureBuilder>(features).execute()
        }
    }

    fun getPlanOffers(): List<PlanOffersModel> {
        return WellSql.select(PlanOffersBuilder::class.java).asModel.mapNotNull { plan ->
            val planFeatures = WellSql.select(PlanOffersFeatureBuilder::class.java)
                    .where().equals(PlanOffersFeatureTable.INTERNAL_PLAN_ID, plan.internalPlanId).endWhere()
                    .asModel.mapNotNull {
                it.build()
            }
            val planIds = WellSql.select(PlanOffersIdBuilder::class.java)
                    .where().equals(PlanOffersIdTable.INTERNAL_PLAN_ID, plan.internalPlanId).endWhere()
                    .asModel.mapNotNull { it.build() }
            plan.build(planIds, planFeatures)
        }
    }

    private fun Feature.toBuilder(internalPlanId: Int): PlanOffersFeatureBuilder {
        return PlanOffersFeatureBuilder(
                internalPlanId = internalPlanId,
                stringId = this.id,
                name = this.name,
                description = this.description
        )
    }

    private fun PlanOffersModel.toBuilder(internalPlanId: Int): PlanOffersBuilder {
        return PlanOffersBuilder(
                internalPlanId = internalPlanId,
                name = this.name,
                shortName = this.shortName,
                tagline = this.tagline,
                description = this.description,
                icon = this.iconUrl
        )
    }

    @Table(name = "PlanOffersFeature")
    data class PlanOffersFeatureBuilder(
        @PrimaryKey @Column private var id: Int = 0,
        @Column var internalPlanId: Int = 0,
        @Column var stringId: String? = null,
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

    @Table(name = "PlanOffersId")
    data class PlanOffersIdBuilder(
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

    @Table(name = "PlanOffers")
    data class PlanOffersBuilder(
        @PrimaryKey @Column private var id: Int = 0,
        @Column var internalPlanId: Int = 0,

        @Column var name: String? = null,
        @Column var shortName: String? = null,
        @Column var tagline: String? = null,
        @Column var description: String? = null,
        @Column var icon: String? = null
    ) : Identifiable {
        fun build(planIds: List<Int>, planFeatures: List<Feature>): PlanOffersModel {
            return PlanOffersModel(planIds, planFeatures, name, shortName, tagline, description, icon)
        }

        override fun getId(): Int {
            return this.id
        }

        override fun setId(id: Int) {
            this.id = id
        }
    }
}
