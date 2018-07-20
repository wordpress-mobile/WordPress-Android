package org.wordpress.android.fluxc.model

class PlanModel {
    var id: String? = null
    var slug: String? = null
    var name: String? = null
    var isCurrentPlan: Boolean = false
    var hasDomainCredit: Boolean = false
    override fun toString(): String {
        return "PlanModel(id=$id, slug=$slug, name=$name, isCurrentPlan=$isCurrentPlan, hasDomainCredit=$hasDomainCredit)"
    }

}
