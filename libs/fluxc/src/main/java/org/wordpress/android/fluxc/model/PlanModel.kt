package org.wordpress.android.fluxc.model

class PlanModel {
    val productId: Long
    val productSlug: String
    val productName: String
    val isCurrentPlan: Boolean
    val hasDomainCredit: Boolean

    constructor(
        productId: Long,
        productSlug: String,
        productName: String,
        isCurrentPlan: Boolean,
        hasDomainCredit: Boolean) {
        this.productId = productId
        this.productSlug = productSlug
        this.productName = productName
        this.isCurrentPlan = isCurrentPlan
        this.hasDomainCredit = hasDomainCredit
    }
}
