package org.wordpress.android.fluxc.model

class PlanModel (
    val productId: Long,
    val productSlug: String,
    val productName: String,
    val isCurrentPlan: Boolean,
    val hasDomainCredit: Boolean
)
