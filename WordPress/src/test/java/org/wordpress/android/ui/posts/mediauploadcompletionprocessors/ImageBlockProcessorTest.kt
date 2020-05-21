package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Test
import org.junit.Before

import org.wordpress.android.util.helpers.MediaFile

class ImageBlockProcessorTest {
    private val mediaFile: MediaFile = mock()
    private lateinit var processor: BlockProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl)
        processor = ImageBlockProcessor(TestContent.localMediaId, mediaFile)
    }

    @Test
    fun `processBlock replaces id and url in matching block`() {
        val processedBlock = processor.processBlock(TestContent.oldImageBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newImageBlock)
    }

    @Test
    fun `processBlock replaces id and url in matching block when id is not first`() {
        val processedBlock = processor.processBlock(TestContent.oldImageBlockIdNotFirst)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.newImageBlockIdNotFirst)
    }

    @Test
    fun `processBlock leaves non-matching block unchanged`() {
        val nonMatchingId = "123"
        val imageBlockProcessor = ImageBlockProcessor(nonMatchingId, mediaFile)
        val processedBlock = imageBlockProcessor.processBlock(TestContent.oldImageBlock)
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.oldImageBlock)
    }

    @Test
    fun `processBlock leaves image block with colliding prefix unchanged`() {
        val processedBlock = processor.processBlock(
                TestContent.imageBlockWithPrefixCollision
        )
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.imageBlockWithPrefixCollision)
    }

    @Test
    fun `processBlock leaves image block with colliding suffix unchanged`() {
        val processedBlock = processor.processBlock(
                TestContent.imageBlockWithSuffixCollision
        )
        Assertions.assertThat(processedBlock).isEqualTo(TestContent.imageBlockWithSuffixCollision)
    }
}
