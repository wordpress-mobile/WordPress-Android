package org.wordpress.android.ui.domains

import uniffi.wp_api.CartKey
import uniffi.wp_api.ShoppingCart
import uniffi.wp_api.ShoppingCartMessages
import uniffi.wp_api.ShoppingCartTax
import uniffi.wp_api.ShoppingCartTaxLocation

/**
 * Builds a [ShoppingCart] for tests. Only the cart key is exposed: the app
 * reads nothing off a created cart, it hands the whole thing to the redeem
 * call, so every other field carries an empty or zero value and the key is
 * there to tell one cart from another.
 */
fun testShoppingCart(
    cartKey: CartKey = CartKey.NoSite,
) = ShoppingCart(
    cartGeneratedAtTimestamp = 0uL,
    blogId = 0uL,
    cartKey = cartKey,
    coupon = "",
    isCouponApplied = false,
    hasAutoRenewCouponBeenAutomaticallyApplied = false,
    nextDomainIsFree = false,
    nextDomainCondition = "",
    products = emptyList(),
    unmergedProducts = emptyList(),
    totalCost = 0L,
    currency = "",
    totalCostInteger = 0L,
    temporary = true,
    tax = ShoppingCartTax(displayTaxes = false, location = ShoppingCartTaxLocation()),
    couponSavingsTotalInteger = 0L,
    subTotalWithTaxesInteger = 0L,
    subTotalInteger = 0L,
    totalTax = 0L,
    totalTaxInteger = 0L,
    credits = 0L,
    creditsInteger = 0L,
    allowedPaymentMethods = emptyList(),
    isGiftPurchase = false,
    messages = ShoppingCartMessages(),
    bundledDomain = null,
)
