package org.wordpress.android.fluxc.network.rest.wpapi.media

import org.wordpress.android.fluxc.model.MediaModel
import org.wordpress.android.fluxc.model.SiteModel
import org.wordpress.android.fluxc.utils.MimeType

interface MediaRsClient {
    fun fetchMediaList(
        site: SiteModel,
        number: Int,
        offset: Int,
        mimeType: MimeType.Type?,
        searchTerm: String? = null
    )
    fun fetchMedia(site: SiteModel, media: MediaModel?)
    fun deleteMedia(site: SiteModel, media: MediaModel?)
    fun uploadMedia(site: SiteModel, media: MediaModel?)
    fun cancelUpload(media: MediaModel?)
    fun pushMedia(site: SiteModel, media: MediaModel?)
}
