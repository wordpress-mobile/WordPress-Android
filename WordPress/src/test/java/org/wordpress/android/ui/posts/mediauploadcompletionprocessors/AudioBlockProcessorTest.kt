package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.helpers.MediaFile

class AudioBlockProcessorTest {
    private val mediaFile: MediaFile = mock()
    private lateinit var processor: AudioBlockProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.optimalFileURL).thenReturn(TestContent.remoteAudioUrl)
        processor = AudioBlockProcessor(TestContent.localMediaId, mediaFile)
    }

    @Test
    fun `processBlock replaces id and src in matching block`() {
        val processedBlock = processor.processBlock(TestContent.oldAudioBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newAudioBlock)
    }

    @Test
    fun `processBlock leaves non-matching block unchanged`() {
        val nonMatchingId = "123"
        val processor = AudioBlockProcessor(nonMatchingId, mediaFile)
        val processedBlock = processor.processBlock(TestContent.oldFileBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.oldFileBlock)
    }
}
