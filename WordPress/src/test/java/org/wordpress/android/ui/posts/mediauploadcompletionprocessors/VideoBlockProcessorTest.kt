package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.helpers.MediaFile

class VideoBlockProcessorTest {
    private val mediaFile: MediaFile = mock()
    private lateinit var processor: BlockProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.optimalFileURL).thenReturn(TestContent.remoteVideoUrl)
        processor = VideoBlockProcessor(TestContent.localMediaId, mediaFile)
    }

    @Test
    fun `processBlock replaces id and url in matching block`() {
        val processedBlock = processor.processBlock(TestContent.oldVideoBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newVideoBlock)
    }

    @Test
    fun `processBlock replaces id and url in matching block when id is not first`() {
        val processedBlock = processor.processBlock(TestContent.oldVideoBlockIdNotFirst)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newVideoBlockIdNotFirst)
    }
}
