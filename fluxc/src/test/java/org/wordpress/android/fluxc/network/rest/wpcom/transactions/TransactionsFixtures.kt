package org.wordpress.android.fluxc.network.rest.wpcom.transactions

import org.wordpress.android.fluxc.model.DomainContactModel
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse.Extra
import org.wordpress.android.fluxc.network.rest.wpcom.transactions.TransactionsRestClient.CreateShoppingCartResponse.Product

val SUPPORTED_COUNTRIES_MODEL = arrayOf(
        SupportedDomainCountry("US", "United State"), SupportedDomainCountry("NA", "Narnia")
)

val CREATE_SHOPPING_CART_RESPONSE = CreateShoppingCartResponse(
        76,
        22.toString(),
        listOf(
                Product(76, "superraredomainname156726.blog", Extra(true)),
                Product(1001, "other product", Extra(true))
        )
)

val CREATE_SHOPPING_CART_WITH_PLAN_RESPONSE = CreateShoppingCartResponse(
    76,
    22.toString(),
    listOf(
        Product(76, "superraredomainname156726.blog", Extra(true)),
        Product(1009, "Plan", Extra(true)),
        Product(1001, "other product", Extra(true))
    )
)

val CREATE_SHOPPING_CART_WITH_NO_SITE_RESPONSE = CreateShoppingCartResponse(
    0,
    22.toString(),
    listOf(
        Product(76, "superraredomainname156726.blog", Extra(true)),
        Product(1001, "other product", Extra(true))
    )
)

val DOMAIN_CONTACT_INFORMATION = DomainContactModel(
        "Wapu",
        "Wordpress",
        "WordPress",
        "7337 Publishing Row",
        "Apt 404",
        "90210",
        "Best City",
        "CA",
        "USA",
        "wapu@wordpress.org",
        "+1.3120000000",
        null
)
