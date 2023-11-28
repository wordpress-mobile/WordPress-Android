package org.wordpress.android.fluxc.network.rest.wpcom.theme

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import org.wordpress.android.fluxc.network.Response

data class DemoThemePagesResponse(
    val demoPages: List<DemoPage>,
) : Response

@Parcelize
data class DemoPage(
    val link: String,
    val title: PageTitle,
) : Parcelable

@Parcelize
data class PageTitle(
    val rendered: String,
) : Parcelable