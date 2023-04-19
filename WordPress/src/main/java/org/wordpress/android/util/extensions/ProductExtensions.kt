package org.wordpress.android.util.extensions

import org.wordpress.android.fluxc.model.products.Product

fun Product?.isOnSale(): Boolean = this?.saleCost?.let { it > 0.0 } == true
