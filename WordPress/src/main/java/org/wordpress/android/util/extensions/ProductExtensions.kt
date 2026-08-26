package org.wordpress.android.util.extensions

import uniffi.wp_api.Product

/**
 * A product is on sale when the API reports a positive sale price. `saleCost`
 * is a `Decimal2`, which carries the amount in hundredths of the currency
 * unit, so `700` is `7.00`.
 */
fun Product?.isOnSale(): Boolean = this?.saleCost?.let { it > 0L } == true
