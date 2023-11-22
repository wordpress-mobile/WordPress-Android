package org.wordpress.android.ui.sitecreation.plans

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

interface PlansScreenListener {
    fun onPlanSelected(plan: PlanModel, domainName: String?)
}

@Parcelize
data class PlanModel(val productId: Int?, val productSlug: String?) : Parcelable
