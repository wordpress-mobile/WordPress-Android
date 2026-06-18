package org.wordpress.android.support.unified.model

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class UnifiedAttachmentTest {
    @Test
    fun `image content types map to Image`() {
        assertThat(attachment("image/png").type).isEqualTo(AttachmentType.Image)
        assertThat(attachment("image/jpeg").type).isEqualTo(AttachmentType.Image)
        assertThat(attachment("image/png").isImage).isTrue()
    }

    @Test
    fun `video content types map to Video`() {
        assertThat(attachment("video/mp4").type).isEqualTo(AttachmentType.Video)
    }

    @Test
    fun `text html maps to Link`() {
        assertThat(attachment("text/html").type).isEqualTo(AttachmentType.Link)
    }

    @Test
    fun `text html is matched case insensitively`() {
        assertThat(attachment("TEXT/HTML").type).isEqualTo(AttachmentType.Link)
    }

    @Test
    fun `text html with charset parameter maps to Link`() {
        assertThat(attachment("text/html; charset=utf-8").type).isEqualTo(AttachmentType.Link)
    }

    @Test
    fun `other text and binary content types map to Other`() {
        assertThat(attachment("text/plain").type).isEqualTo(AttachmentType.Other)
        assertThat(attachment("application/pdf").type).isEqualTo(AttachmentType.Other)
        assertThat(attachment("application/octet-stream").type).isEqualTo(AttachmentType.Other)
    }

    private fun attachment(contentType: String) = UnifiedAttachment(
        id = 1L,
        filename = "file",
        contentType = contentType,
        url = "https://example.com",
        botCitationScore = null,
    )
}
