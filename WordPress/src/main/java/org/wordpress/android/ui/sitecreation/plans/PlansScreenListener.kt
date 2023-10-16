package org.wordpress.android.ui.sitecreation.plans

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface PlansScreenListener {
    fun onPlanSelected(plan: PlanModel)
}

@Parcelize
data class PlanModel(
    val productId: Int?,
    val productSlug: String?,
    val isCurrentPlan: Boolean,
    val hasDomainCredit: Boolean
) : Parcelable
