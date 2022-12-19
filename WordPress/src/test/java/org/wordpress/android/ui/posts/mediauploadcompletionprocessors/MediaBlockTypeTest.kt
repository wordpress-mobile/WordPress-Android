package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.helpers.MediaFile

class MediaBlockTypeTest {
    private val mediaFile: MediaFile = mock()
    private lateinit var processor: MediaUploadCompletionProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl)
        whenever(mediaFile.getAttachmentPageURL(any())).thenReturn(TestContent.attachmentPageUrl)
        processor = MediaUploadCompletionProcessor(TestContent.localMediaId, mediaFile, TestContent.siteUrl)
    }

    @Test
    fun `detectBlockType works for image blocks`() {
        val blockType = MediaBlockType.detectBlockType(TestContent.newImageBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.IMAGE)
    }

    @Test
    fun `detectBlockType works for video blocks`() {
        val blockType = MediaBlockType.detectBlockType(TestContent.newVideoBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.VIDEO)
    }

    @Test
    fun `detectBlockType works for media-text blocks`() {
        val blockType = MediaBlockType.detectBlockType(TestContent.newMediaTextBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.MEDIA_TEXT)
    }

    @Test
    fun `detectBlockType works for gallery blocks`() {
        val blockType = MediaBlockType.detectBlockType(TestContent.newGalleryBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.GALLERY)
    }

    @Test
    fun `detectBlockType returns null for non-media blocks`() {
        val blockType = MediaBlockType.detectBlockType(TestContent.paragraphBlock)
        Assertions.assertThat(blockType).isNull()
    }
}
