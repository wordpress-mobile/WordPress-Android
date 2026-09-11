package org.wordpress.android.ui.domains

import uniffi.wp_api.TransactionFailedPurchase
import uniffi.wp_api.TransactionReceipt

/**
 * Builds a [TransactionReceipt] for tests. Only the success flag and the failed
 * purchases are exposed, because that is all the registration screen reads; the
 * rest carry empty or zero values.
 */
fun testTransactionReceipt(
    success: Boolean = true,
    failedPurchases: Map<ULong, List<TransactionFailedPurchase>> = emptyMap(),
) = TransactionReceipt(
    receiptId = 0uL,
    orderId = null,
    success = success,
    purchases = emptyMap(),
    failedPurchases = failedPurchases,
    displayPrice = "",
    priceInteger = 0L,
    priceFloat = 0L,
    currency = "",
    isGiftPurchase = false,
    isGravatarDomain = false,
)
