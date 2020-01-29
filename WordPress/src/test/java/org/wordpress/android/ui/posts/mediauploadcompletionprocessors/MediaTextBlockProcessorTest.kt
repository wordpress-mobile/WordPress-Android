package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.Before

import org.wordpress.android.util.helpers.MediaFile

class MediaTextBlockProcessorTest {
    private val mediaFile: MediaFile = mock()
    private lateinit var processor: MediaTextBlockProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl)
        processor = MediaTextBlockProcessor(TestContent.localMediaId, mediaFile)
    }

    @Test
    fun `processBlock replaces temporary local id and url for media-text block`() {
        val processedBlock = processor.processBlock(TestContent.oldMediaTextBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newMediaTextBlock)
    }

    @Test
    fun `processBlock replaces temporary local id and url for media-text block when id is not first`() {
        val processedBlock = processor.processBlock(TestContent.oldMediaTextBlockIdNotFirst)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newMediaTextBlockIdNotFirst)
    }

    @Test
    fun `processBlock also works for video`() {
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteVideoUrl)
        processor = MediaTextBlockProcessor(TestContent.localMediaId, mediaFile)
        val processedBlock = processor.processBlock(TestContent.oldMediaTextBlockWithVideo)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newMediaTextBlockWithVideo)
    }
}
