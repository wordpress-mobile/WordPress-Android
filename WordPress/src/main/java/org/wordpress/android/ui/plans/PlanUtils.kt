package org.wordpress.android.ui.plans

import org.wordpress.android.fluxc.model.PlanModel
import org.wordpress.android.fluxc.network.rest.wpcom.site.Domain

fun getCurrentPlan(plans: List<PlanModel>?): PlanModel? = plans?.find { it.isCurrentPlan }

fun isDomainCreditAvailable(plans: List<PlanModel>?): Boolean = getCurrentPlan(plans)?.hasDomainCredit ?: false

fun hasSiteDomains(domains: List<Domain>?): Boolean = domains?.isNotEmpty() ?: false
