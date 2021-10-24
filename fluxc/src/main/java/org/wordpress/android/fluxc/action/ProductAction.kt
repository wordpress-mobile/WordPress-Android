package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction

@ActionEnum
enum class ProductAction : IAction {
    FETCH_PRODUCTS
}
