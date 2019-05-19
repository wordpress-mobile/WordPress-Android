package org.wordpress.android.ui.plans

import org.wordpress.android.fluxc.model.PlanModel

fun getCurrentPlan(plans: List<PlanModel>?): PlanModel? = plans?.find { it.isCurrentPlan }

fun isDomainCreditAvailable(plans: List<PlanModel>?): Boolean = getCurrentPlan(plans)?.hasDomainCredit ?: false
