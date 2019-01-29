package org.wordpress.android.fluxc.network.rest.wpcom.plans

import org.wordpress.android.fluxc.model.plans.PlanModel
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PlansRestClient.PlansResponse
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PlansRestClient.PlansResponse.Feature
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PlansRestClient.PlansResponse.Group
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PlansRestClient.PlansResponse.Plan
import org.wordpress.android.fluxc.network.rest.wpcom.plans.PlansRestClient.PlansResponse.PlanId

val PLAN_MODELS = listOf(
        PlanModel(
                listOf(1), listOf(
                PlanModel.Feature("subdomain", "WordPress.com Subdomain", "Subdomain Description"),
                PlanModel.Feature("jetpack-essentials", "JE Features", "JE Description")
        ), "WordPress.com Free",
                "Free",
                "Best for Getting Started",
                "Free description",
                "https://s0.wordpress.com/i/store/mobile/plans-free.png"
        ), PlanModel(
        listOf(1003, 1023), listOf(
        PlanModel.Feature("custom-domain", "Custom Domain Name", "CDN Description"),
        PlanModel.Feature("support-live", "Email & Live Chat Support", "LS Description"),
        PlanModel.Feature("no-ads", "Remove WordPress.com Ads", "No Ads Description")
), "WordPress.com Premium",
        "Premium",
        "Best for Entrepreneurs and Freelancers",
        "Premium description",
        "https://s0.wordpress.com/i/store/mobile/plans-premium.png"
)
)

val PLANS_RESPONSE = PlansResponse(
        listOf(Group("personal", "Personal"), Group("business", "Business")), listOf(
        Plan(
                listOf("personal", "too personal"),
                listOf(PlanId(1)),
                listOf("subdomain", "jetpack-essentials"),
                "WordPress.com Free",
                "Free",
                "Best for Getting Started",
                "Free description",
                "https://s0.wordpress.com/i/store/mobile/plans-free.png"
        ), Plan(
        listOf("business"),
        listOf(PlanId(1003), PlanId(1023)),
        listOf("custom-domain", "support-live", "no-ads"),
        "WordPress.com Premium",
        "Premium",
        "Best for Entrepreneurs and Freelancers",
        "Premium description",
        "https://s0.wordpress.com/i/store/mobile/plans-premium.png"
)
), listOf(
        Feature("subdomain", "WordPress.com Subdomain", "Subdomain Description"),
        Feature("jetpack-essentials", "JE Features", "JE Description"),
        Feature("custom-domain", "Custom Domain Name", "CDN Description"),
        Feature("support-live", "Email & Live Chat Support", "LS Description"),
        Feature("no-ads", "Remove WordPress.com Ads", "No Ads Description")
)
)