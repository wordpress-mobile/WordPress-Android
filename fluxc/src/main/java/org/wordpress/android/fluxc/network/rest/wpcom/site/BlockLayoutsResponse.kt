package org.wordpress.android.fluxc.network.rest.wpcom.site

import org.wordpress.android.fluxc.network.Response

data class BlockLayoutsResponse(
    val layouts: List<GutenbergLayout>,
    val categories: List<GutenbergLayoutCategory>
) : Response

data class GutenbergLayout(
    val slug: String,
    val title: String,
    val preview: String,
    val content: String,
    val categories: List<GutenbergLayoutCategory>
)

data class GutenbergLayoutCategory(
    val slug: String,
    val title: String,
    val description: String,
    val emoji: String
)
