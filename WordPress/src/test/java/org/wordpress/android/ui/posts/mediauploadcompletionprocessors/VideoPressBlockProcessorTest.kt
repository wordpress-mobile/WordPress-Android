package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.util.helpers.MediaFile

@RunWith(MockitoJUnitRunner::class)
class VideoPressBlockProcessorTest {
    private val mediaFile: MediaFile = mock()
    private lateinit var processor: VideoPressBlockProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.videoPressGuid).thenReturn(TestContent.videoPressGuid)

        processor = VideoPressBlockProcessor(TestContent.localMediaId, mediaFile)
    }

    @Test
    fun `processBlock replaces id in VideoPress block with default attributes`() {
        val processedBlock = processor.processBlock(TestContent.oldVideoPressBlockWithDefaultAttrs, true)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newVideoPressBlockWithDefaultAttrs)
    }

    @Test
    fun `processBlock replaces id in VideoPress block with different attributes to the default`() {
        val processedBlock = processor.processBlock(TestContent.oldVideoPressBlockWithAttrs, true)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newVideoPressBlockWithAttrs)
    }

    @Test
    fun `processBlock leaves Video block unchanged`() {
        val nonMatchingId = "123"
        val processor = VideoPressBlockProcessor(nonMatchingId, mediaFile)
        val processedBlock = processor.processBlock(TestContent.oldVideoBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.oldVideoBlock)
    }
}
