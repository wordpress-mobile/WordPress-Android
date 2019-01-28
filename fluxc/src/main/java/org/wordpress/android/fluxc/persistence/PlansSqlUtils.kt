package org.wordpress.android.fluxc.persistence

import com.wellsql.generated.PlanFeatureTable
import com.wellsql.generated.PlanIdTable
import com.yarolegovich.wellsql.WellSql
import com.yarolegovich.wellsql.core.Identifiable
import com.yarolegovich.wellsql.core.annotation.Column
import com.yarolegovich.wellsql.core.annotation.PrimaryKey
import com.yarolegovich.wellsql.core.annotation.Table
import com.yarolegovich.wellsql.core.annotation.Unique
import org.wordpress.android.fluxc.model.plans.PlanModel
import org.wordpress.android.fluxc.model.plans.PlanModel.Feature
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlansSqlUtils @Inject constructor() {
    fun storePlans(plans: List<PlanModel>) {
        WellSql.delete(PlanBuilder::class.java).execute()
        WellSql.delete(PlanIdBuilder::class.java).execute()
        WellSql.delete(PlanFeatureBuilder::class.java).execute()

        plans.forEachIndexed { index, planModel ->
            val planIds = planModel.planIds?.map { PlanIdBuilder(internalPlanId = index, productId = it) }
            val features = planModel.features?.map { it.toBuilder(index) }
            val plan = planModel.toBuilder(index)

            WellSql.insert<PlanBuilder>(plan).execute()
            WellSql.insert<PlanIdBuilder>(planIds).execute()
            WellSql.insert<PlanFeatureBuilder>(features).execute()
        }
    }

    fun getPlans(): List<PlanModel> {
        return WellSql.select(PlanBuilder::class.java).asModel.mapNotNull { plan ->
            val planFeatures = WellSql.select(PlanFeatureBuilder::class.java)
                    .where().equals(PlanFeatureTable.INTERNAL_PLAN_ID, plan.internalPlanId).endWhere()
                    .asModel.mapNotNull {
                it.build()
            }
            val planIds = WellSql.select(PlanIdBuilder::class.java)
                    .where().equals(PlanIdTable.INTERNAL_PLAN_ID, plan.internalPlanId).endWhere()
                    .asModel.mapNotNull { it.build() }



            plan.build(planIds, planFeatures)
        }
    }

    private fun Feature.toBuilder(internalPlanId: Int): PlanFeatureBuilder {
        return PlanFeatureBuilder(
                internalPlanId = internalPlanId,
                stringId = this.id,
                name = this.name,
                description = this.description
        )
    }

    private fun PlanModel.toBuilder(internalPlanId: Int): PlanBuilder {
        return PlanBuilder(
                internalPlanId = internalPlanId,
                name = this.name,
                shortName = this.shortName,
                tagline = this.tagline,
                description = this.description,
                icon = this.iconUrl
        )
    }

    @Table(name = "PlanFeature")
    data class PlanFeatureBuilder(
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

    @Table(name = "PlanId")
    data class PlanIdBuilder(
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

    @Table(name = "Plan")
    data class PlanBuilder(
        @PrimaryKey @Column private var id: Int = 0,
        @Column var internalPlanId: Int = 0,

        @Column var name: String? = null,
        @Column var shortName: String? = null,
        @Column var tagline: String? = null,
        @Column var description: String? = null,
        @Column var icon: String? = null
    ) : Identifiable {
        fun build(planIds: List<Int>, planFeatures: List<Feature>): PlanModel {
            return PlanModel(planIds, planFeatures, name, shortName, tagline, description, icon)
        }

        override fun getId(): Int {
            return this.id
        }

        override fun setId(id: Int) {
            this.id = id
        }
    }
}


