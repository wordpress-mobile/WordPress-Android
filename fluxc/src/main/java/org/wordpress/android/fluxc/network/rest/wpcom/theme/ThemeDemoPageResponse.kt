package org.wordpress.android.fluxc.network.rest.wpcom.theme

data class DemoPageResponse(
    val link: String,
    val slug: String,
    val title: PageTitle,
)

data class PageTitle(
    val rendered: String,
)
