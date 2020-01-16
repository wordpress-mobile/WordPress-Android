package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Test

import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.Helpers
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.MediaBlockType
import org.wordpress.android.util.helpers.MediaFile

@RunWith(MockitoJUnitRunner::class)
class MediaUploadCompletionProcessorTest {
    private val mediaFile: MediaFile = mock()
    private lateinit var processor : MediaUploadCompletionProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl)
        whenever(mediaFile.getAttachmentPageURL(any())).thenReturn(TestContent.attachmentPageUrl)
        processor = MediaUploadCompletionProcessor(TestContent.localMediaId, mediaFile, TestContent.siteUrl)
    }

    @Test
    fun `processPost splices id and url for an image block`() {
        val blocks = processor.processPost(TestContent.oldPostImage)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newPostImage)
    }

    @Test
    fun `processPost splices id and url for a video block`() {
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteVideoUrl)
        processor = MediaUploadCompletionProcessor(TestContent.localMediaId, mediaFile, TestContent.siteUrl)
        val blocks = processor.processPost(TestContent.oldPostVideo)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newPostVideo)
    }

    @Test
    fun `processPost splices id and url for a media-text block`() {
        val blocks = processor.processPost(TestContent.oldPostMediaText)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newPostMediaText)
    }

    @Test
    fun `processPost splices id and url for a gallery block`() {
        val blocks = processor.processPost(TestContent.oldPostGallery)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newPostGallery)
    }

    @Test
    fun `detectBlockType works for image blocks`() {
        val blockType = Helpers.detectBlockType(TestContent.newImageBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.IMAGE)
    }

    @Test
    fun `detectBlockType works for video blocks`() {
        val blockType = Helpers.detectBlockType(TestContent.newVideoBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.VIDEO)
    }

    @Test
    fun `detectBlockType works for media-text blocks`() {
        val blockType = Helpers.detectBlockType(TestContent.newMediaTextBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.MEDIA_TEXT)
    }

    @Test
    fun `detectBlockType works for gallery blocks`() {
        val blockType = Helpers.detectBlockType(TestContent.newGalleryBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.GALLERY)
    }

    @Test
    fun `detectBlockType returns null for non-media blocks`() {
        val blockType = Helpers.detectBlockType(TestContent.paragraphBlock)
        Assertions.assertThat(blockType).isNull()
    }

    @Test
    fun `processGalleryBlock replaces temporary local id and url for gallery block`() {
        val processedBlock = processor.processGalleryBlock(TestContent.oldGalleryBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlock)
    }

    @Test
    fun `processGalleryBlock can handle ids with mixed types`() {
        val processedBlock = processor.processGalleryBlock(TestContent.oldGalleryBlockMixTypeIds)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlock)
    }

    @Test
    fun `processGalleryBlock can handle ids with mixed types different order`() {
        val processedBlock = processor.processGalleryBlock(TestContent.oldGalleryBlockMixTypeIds2)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlockWithMixTypeIds2)
    }

    @Test
    fun `processGalleryBlock can handle Link To Media File setting`() {
        val processedBlock = processor.processGalleryBlock(TestContent.oldGalleryBlockLinkToMediaFile)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlockLinkToMediaFile)
    }

    @Test
    fun `processGalleryBlock can handle Link To Attachment Page setting`() {
        val processedBlock = processor.processGalleryBlock(TestContent.oldGalleryBlockLinkToAttachmentPage)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlockLinkToAttachmentPage)
    }
}
