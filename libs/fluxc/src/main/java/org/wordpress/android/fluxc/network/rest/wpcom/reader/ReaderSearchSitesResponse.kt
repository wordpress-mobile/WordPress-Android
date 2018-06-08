package org.wordpress.android.fluxc.network.rest.wpcom.reader

import com.google.gson.annotations.JsonAdapter

import org.wordpress.android.fluxc.model.ReaderSiteModel

@JsonAdapter(ReaderSearchSitesDeserializer::class)
class ReaderSearchSitesResponse (
    val offset: Int,
    val sites: List<ReaderSiteModel>
)
