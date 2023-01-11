package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.wordpress.android.util.helpers.MediaFile

class GalleryBlockProcessorTest {
    private val mediaFile: MediaFile = mock()
    private val mediaUploadCompletionProcessor: MediaUploadCompletionProcessor = mock()
    private lateinit var processor: GalleryBlockProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl)
        whenever(mediaFile.optimalFileURL).thenReturn(TestContent.remoteImageUrl)
        whenever(mediaFile.getAttachmentPageURL(any())).thenReturn(TestContent.attachmentPageUrl)
        processor = GalleryBlockProcessor(
            TestContent.localMediaId, mediaFile, TestContent.siteUrl,
            mediaUploadCompletionProcessor
        )
    }

    @Test
    fun `processBlock replaces temporary local id and url for gallery block`() {
        val processedBlock = processor.processBlock(TestContent.oldGalleryBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlock)
    }

    @Test
    fun `processBlock replaces temporary local id and url for gallery block when ids are not first`() {
        val processedBlock = processor.processBlock(TestContent.oldGalleryBlockIdsNotFirst)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlockIdsNotFirst)
    }

    @Test
    fun `processBlock can handle ids with mixed types`() {
        val processedBlock = processor.processBlock(TestContent.oldGalleryBlockMixTypeIds)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlock)
    }

    @Test
    fun `processBlock can handle ids with mixed types different order`() {
        val processedBlock = processor.processBlock(TestContent.oldGalleryBlockMixTypeIds2)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlockWithMixTypeIds2)
    }

    @Test
    fun `processBlock can handle Link To Media File setting`() {
        val processedBlock = processor.processBlock(TestContent.oldGalleryBlockLinkToMediaFile)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlockLinkToMediaFile)
    }

    @Test
    fun `processBlock can handle Link To Attachment Page setting`() {
        val processedBlock = processor.processBlock(TestContent.oldGalleryBlockLinkToAttachmentPage)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newGalleryBlockLinkToAttachmentPage)
    }

    @Test
    fun `processBlock defers the refactored gallery to inner blocks recursion`() {
        processor.processBlock(TestContent.oldRefactoredGalleryBlock)
        verify(mediaUploadCompletionProcessor).processContent(TestContent.oldRefactoredGalleryBlockInnerBlocks)
    }
}
