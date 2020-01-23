package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner

import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.TestContent
import org.wordpress.android.util.helpers.MediaFile

/* ktlint-disable max-line-length */
/* ktlint-disable parameter-list-wrapping */
@RunWith(MockitoJUnitRunner::class)
class PostUtilsUploadProcessingTest {
    private val mediaFile: MediaFile = mock()

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(TestContent.remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteImageUrl)
        whenever(mediaFile.getAttachmentPageURL(any())).thenReturn(TestContent.attachmentPageUrl)
    }

    @Test
    fun `replaceMediaFileWithUrlInGutenbergPost replaces temporary local id and url for image block`() {
        val processedContent = PostUtils.replaceMediaFileWithUrlInGutenbergPost(TestContent.oldImageBlock,
                TestContent.localMediaId, mediaFile, TestContent.siteUrl)
        Assertions.assertThat(processedContent).isEqualTo(TestContent.newImageBlock)
    }

    @Test
    fun `replaceMediaFileWithUrlInGutenbergPost replaces temporary local id and url for image block with colliding prefixes`() {
        val oldContent = TestContent.oldImageBlock + TestContent.imageBlockWithPrefixCollision
        val newContent = TestContent.newImageBlock + TestContent.imageBlockWithPrefixCollision
        val processedContent = PostUtils.replaceMediaFileWithUrlInGutenbergPost(oldContent, TestContent.localMediaId,
                mediaFile, TestContent.siteUrl)
        Assertions.assertThat(processedContent).isEqualTo(newContent)
    }

    @Test
    fun `replaceMediaFileWithUrlInGutenbergPost replaces temporary local id and url for media-text block`() {
        val processedContent = PostUtils.replaceMediaFileWithUrlInGutenbergPost(TestContent.oldMediaTextBlock,
                TestContent.localMediaId, mediaFile, TestContent.siteUrl)
        Assertions.assertThat(processedContent).isEqualTo(TestContent.newMediaTextBlock)
    }

    @Test
    fun `replaceMediaFileWithUrlInGutenbergPost also works with video`() {
        whenever(mediaFile.fileURL).thenReturn(TestContent.remoteVideoUrl)
        val processedContent = PostUtils.replaceMediaFileWithUrlInGutenbergPost(TestContent.oldMediaTextBlockWithVideo,
                TestContent.localMediaId, mediaFile, TestContent.siteUrl)
        Assertions.assertThat(processedContent).isEqualTo(TestContent.newMediaTextBlockWithVideo)
    }
}
