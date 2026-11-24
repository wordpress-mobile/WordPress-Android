package org.wordpress.android.ui.media.library

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.fluxc.model.MediaModel.MediaUploadState

class MediaThumbnailUrlTest {
    private val noPhoton = MediaThumbnailSource(usePhoton = false)

    private fun item(
        mediumLargeUrl: String? = null,
        mediumUrl: String? = null,
        largeUrl: String? = null,
        thumbnailUrl: String? = null
    ) = MediaGridItemUiState(
        localId = 1,
        type = MediaGridItemType.IMAGE,
        fileName = "file.jpg",
        mimeType = "image/jpeg",
        displayName = "file.jpg",
        localFilePath = null,
        url = "https://example.com/file.jpg",
        mediumLargeUrl = mediumLargeUrl,
        mediumUrl = mediumUrl,
        largeUrl = largeUrl,
        thumbnailUrl = thumbnailUrl,
        uploadState = MediaUploadState.UPLOADED
    )

    @Test
    fun `without photon the medium-large size wins over the others`() {
        val url = item(mediumLargeUrl = "ml", mediumUrl = "m", largeUrl = "l", thumbnailUrl = "t")
            .bestImageUrl(noPhoton, 300, 225)

        assertThat(url).isEqualTo("ml")
    }

    @Test
    fun `without photon medium is preferred to large`() {
        val url = item(mediumUrl = "m", largeUrl = "l").bestImageUrl(noPhoton, 300, 225)

        assertThat(url).isEqualTo("m")
    }

    @Test
    fun `without photon or generated sizes the thumbnail is used`() {
        val url = item(mediumLargeUrl = "", thumbnailUrl = "t").bestImageUrl(noPhoton, 300, 225)

        assertThat(url).isEqualTo("t")
    }
}
