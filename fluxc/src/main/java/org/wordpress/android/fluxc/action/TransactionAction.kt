package org.wordpress.android.fluxc.action

import org.wordpress.android.fluxc.annotations.Action
import org.wordpress.android.fluxc.annotations.ActionEnum
import org.wordpress.android.fluxc.annotations.action.IAction
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartPayload
import org.wordpress.android.fluxc.store.TransactionsStore.CreateShoppingCartWithDomainAndPlanPayload
import org.wordpress.android.fluxc.store.TransactionsStore.RedeemShoppingCartPayload

@ActionEnum
enum class TransactionAction : IAction {
    // Remote actions
    FETCH_SUPPORTED_COUNTRIES,
    @Action(payloadType = CreateShoppingCartPayload::class)
    CREATE_SHOPPING_CART,
    @Action(payloadType = CreateShoppingCartWithDomainAndPlanPayload::class)
    CREATE_SHOPPING_CART_WITH_DOMAIN_AND_PLAN,
    @Action(payloadType = RedeemShoppingCartPayload::class)
    REDEEM_CART_WITH_CREDITS
}
