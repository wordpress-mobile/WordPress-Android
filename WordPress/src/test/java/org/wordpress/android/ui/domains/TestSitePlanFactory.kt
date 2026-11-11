package org.wordpress.android.ui.domains

import uniffi.wp_api.SitePlan
import uniffi.wp_api.SitePlanCurrentPlanInfo

/**
 * Builds a [SitePlan] for tests. Only the credit group is exposed, because that
 * is all the domains dashboard reads; the rest are given empty or zero values.
 */
fun testSitePlan(
    productId: ULong = 0u,
    currentPlan: SitePlanCurrentPlanInfo? = null,
) = SitePlan(
    productId = productId,
    productSlug = "",
    productName = "",
    productTierId = 0u,
    productTierProductIds = emptyList(),
    currencyCode = "",
    rawPrice = 0L,
    rawPriceInteger = 0,
    formattedPrice = "",
    formattedOriginalPrice = "",
    rawDiscount = 0L,
    rawDiscountInteger = 0,
    formattedDiscount = "",
    discountReason = null,
    isDomainUpgrade = null,
    interval = 0,
    planCardOrder = null,
    planCardName = null,
    tagline = null,
    currentPlan = currentPlan,
    subscription = null,
    introductoryOffer = null,
    transition = null,
    canStartTrial = null,
    hasSaleCoupon = null,
)

/** A plan the site is currently on, carrying the credit flag. */
fun testCurrentPlan(hasDomainCredit: Boolean) = SitePlanCurrentPlanInfo(
    purchaseId = null,
    userIsOwner = null,
    hasDomainCredit = hasDomainCredit,
)
