package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.Before

import org.wordpress.android.util.helpers.MediaFile

class GalleryBlockProcessorTest {
    private val mediaFile: MediaFile = mock()
    private lateinit var processor: GalleryBlockProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl)
        whenever(mediaFile.getAttachmentPageURL(any())).thenReturn(TestContent.attachmentPageUrl)
        processor = GalleryBlockProcessor(TestContent.localMediaId, mediaFile, TestContent.siteUrl)
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
}
