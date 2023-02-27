package org.wordpress.android.util.extensions

import org.wordpress.android.fluxc.model.products.Product

fun Product.isSaleDomain(): Boolean = this.saleCost?.let { it.compareTo(0.0) > 0 } == true

fun Product.saleCostForDisplay(): String = this.currencyCode + "%.2f".format(this.saleCost)
