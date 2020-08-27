package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.Before

import org.wordpress.android.util.helpers.MediaFile

class CoverBlockProcessorTest {
    private val mediaFile: MediaFile = mock()
    private val mediaUploadCompletionProcessor: MediaUploadCompletionProcessor = mock()
    private lateinit var processor: CoverBlockProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl)
        processor = CoverBlockProcessor(TestContent.localMediaId, mediaFile, mediaUploadCompletionProcessor)
    }

    @Test
    fun `processBlock replaces temporary local id and url for cover block`() {
        val processedBlock = processor.processBlock(TestContent.oldCoverBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newCoverBlock)
    }

    @Test
    fun `processBlock works with nested image blocks`() {
        val processedBlock = processor.processBlock(TestContent.oldCoverBlockWithNestedImageBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newCoverBlockWithNestedImageBlock)
    }

    @Test
    fun `processBlock does not process inner nested cover blocks`() {
        whenever(mediaUploadCompletionProcessor.processContent(any())).thenReturn(TestContent.oldCoverBlock + "\n  ")
        val processedBlock = processor.processBlock(TestContent.oldCoverBlockWithNestedCoverBlockInner)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.oldCoverBlockWithNestedCoverBlockInner)
    }

    @Test
    fun `processBlock works with outer nested cover blocks`() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId2)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl2)
        processor = CoverBlockProcessor(TestContent.localMediaId2, mediaFile, mediaUploadCompletionProcessor)
        val processedBlock = processor.processBlock(TestContent.oldCoverBlockWithNestedCoverBlockOuter)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newCoverBlockWithNestedCoverBlockOuter)
    }

    @Test
    fun `processBlock works with different inline style order`() {
        val processedBlock = processor.processBlock(TestContent.oldCoverBlockDifferentStyleOrder)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newCoverBlockDifferentStyleOrder)
    }

    @Test
    fun `processBlock works with a space in inline styles`() {
        val processedBlock = processor.processBlock(TestContent.oldCoverBlockStyleOrderWithSpace)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newCoverBlockStyleOrderWithoutSpace)
    }

    @Test
    fun `processBlock works with videos`() {
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteVideoUrl)
        processor = CoverBlockProcessor(TestContent.localMediaId, mediaFile, mediaUploadCompletionProcessor)
        val processedBlock = processor.processBlock(TestContent.oldCoverBlockWithVideo)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newCoverBlockWithVideo)
    }
}
