package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

import org.wordpress.android.util.helpers.MediaFile

@RunWith(MockitoJUnitRunner::class)
class MediaUploadCompletionProcessorTest {
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
    fun `processPost splices id and url for an image block`() {
        val blocks = processor.processContent(TestContent.oldPostImage)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newPostImage)
    }

    @Test
    fun `processPost splices id and url for a video block`() {
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteVideoUrl)
        processor = MediaUploadCompletionProcessor(TestContent.localMediaId, mediaFile, TestContent.siteUrl)
        val blocks = processor.processContent(TestContent.oldPostVideo)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newPostVideo)
    }

    @Test
    fun `processPost splices id and url for a media-text block`() {
        val blocks = processor.processContent(TestContent.oldPostMediaText)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newPostMediaText)
    }

    @Test
    fun `processPost splices id and url for a gallery block`() {
        val blocks = processor.processContent(TestContent.oldPostGallery)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newPostGallery)
    }

    @Test
    fun `processPost splices id and url for a cover block`() {
        val blocks = processor.processContent(TestContent.oldPostCover)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newPostCover)
    }

    @Test
    fun `processPost works for nested inner cover blocks`() {
        val blocks = processor.processContent(TestContent.oldCoverBlockWithNestedCoverBlockInner)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newCoverBlockWithNestedCoverBlockInner)
    }

    @Test
    fun `processPost works for nested outer cover blocks`() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId2)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl2)
        processor = MediaUploadCompletionProcessor(TestContent.localMediaId2, mediaFile, TestContent.siteUrl)
        val blocks = processor.processContent(TestContent.oldCoverBlockWithNestedCoverBlockOuter)
        Assertions.assertThat(blocks).isEqualTo(TestContent.newCoverBlockWithNestedCoverBlockOuter)
    }

    @Test
    fun `processPost works for image blocks nested within a cover block`() {
        val processedContent = processor.processContent(TestContent.oldImageBlockNestedInCoverBlock)
        Assertions.assertThat(processedContent).isEqualTo(TestContent.newImageBlockNestedInCoverBlock)
    }

    @Test
    fun `processPost leaves malformed cover block unchanged`() {
        val processedContent = processor.processContent(TestContent.malformedCoverBlock)
        Assertions.assertThat(processedContent).isEqualTo(TestContent.malformedCoverBlock)
    }
}
