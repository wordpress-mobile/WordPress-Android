package org.wordpress.android.util.extensions

import org.wordpress.android.fluxc.model.products.Product
import java.util.Currency

fun Product?.isOnSale(): Boolean = this?.saleCost?.let { it.compareTo(0.0) > 0 } == true
fun Product?.saleCostForDisplay() =
    this?.run { Currency.getInstance(currencyCode).symbol + "%.2f".format(saleCost) } ?: ""
