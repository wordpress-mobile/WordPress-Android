package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.util.helpers.MediaFile

@RunWith(MockitoJUnitRunner::class)
class PostUtilsUploadProcessingTest {
    private val siteUrl = "https://wordpress.org"
    private val remoteImageUrl = "https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png"
    private val remoteVideoUrl = "https://videos.files.wordpress.com/qeJFeNa2/macintosh-plus-floral-shoppe-02-e383aae382b5e38395e383a9e383b3e382af420-e78fbee4bba3e381aee382b3e383b3e38394e383a5e383bc-1_hd.mp4"
    private val localMediaId = "112"
    private val remoteMediaId = "97629"
    private val attachmentPageUrl = "https://wordpress.org?p=97629"
    private val oldImageBlock = """<!-- wp:image {"id":112,"align":"full"} -->
<figure class="wp-block-image alignfull"><img src="file://Screenshot-1-1.png" alt="" class="wp-image-112"><figcaption><em>Gutenberg</em> on web</figcaption></figure>
<!-- /wp:image -->
"""
    private val newImageBlock = """<!-- wp:image {"id":97629,"align":"full"} -->
<figure class="wp-block-image alignfull"><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" class="wp-image-97629"><figcaption><em>Gutenberg</em> on web</figcaption></figure>
<!-- /wp:image -->
"""
    private val imageBlockWithPrefixCollision = """<!-- wp:image {"id":11242,"align":"full"} -->
<figure class="wp-block-image alignfull"><img src="https://wordpress.org/gutenberg/files/2019/07/Screenshot-4-2.png" alt="" class="wp-image-11242"><figcaption><em>Gutenberg</em> on web</figcaption></figure>
<!-- /wp:image -->
"""
    private val oldMediaTextBlock = """<!-- wp:media-text {"mediaId":112,"mediaType":"image"} -->
<div class="wp-block-media-text alignwide is-stacked-on-mobile"><figure class="wp-block-media-text__media"><img src="file://img_20191202_094944-18.jpg" alt="" class="wp-image-112"></figure><div class="wp-block-media-text__content"><!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
<p class="has-large-font-size"></p>
<!-- /wp:paragraph --></div></div>
<!-- /wp:media-text -->
"""
    private val newMediaTextBlock = """<!-- wp:media-text {"mediaId":97629,"mediaType":"image"} -->
<div class="wp-block-media-text alignwide is-stacked-on-mobile"><figure class="wp-block-media-text__media"><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" class="wp-image-97629"></figure><div class="wp-block-media-text__content"><!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
<p class="has-large-font-size"></p>
<!-- /wp:paragraph --></div></div>
<!-- /wp:media-text -->
"""
    private val oldMediaTextBlockWithVideo = """<!-- wp:media-text {"mediaId":112,"mediaType":"video"} -->
<div class="wp-block-media-text alignwide"><figure class="wp-block-media-text__media"><video controls src="file://local-video.mov"></video></figure><div class="wp-block-media-text__content"><!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
<p class="has-large-font-size"></p>
<!-- /wp:paragraph --></div></div>
<!-- /wp:media-text -->
"""
    private val newMediaTextBlockWithVideo = """<!-- wp:media-text {"mediaId":97629,"mediaType":"video"} -->
<div class="wp-block-media-text alignwide"><figure class="wp-block-media-text__media"><video controls src="https://videos.files.wordpress.com/qeJFeNa2/macintosh-plus-floral-shoppe-02-e383aae382b5e38395e383a9e383b3e382af420-e78fbee4bba3e381aee382b3e383b3e38394e383a5e383bc-1_hd.mp4"></video></figure><div class="wp-block-media-text__content"><!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
<p class="has-large-font-size"></p>
<!-- /wp:paragraph --></div></div>
<!-- /wp:media-text -->
"""

    val mediaFile: MediaFile = mock()

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(remoteImageUrl)
        whenever(mediaFile.getAttachmentPageURL(any())).thenReturn(attachmentPageUrl)
    }


    @Test
    fun `replaceMediaFileWithUrlInGutenbergPost replaces temporary local id and url for image block`() {
        val processedContent = PostUtils.replaceMediaFileWithUrlInGutenbergPost(oldImageBlock, localMediaId, mediaFile,
                siteUrl)
        Assertions.assertThat(processedContent).isEqualTo(newImageBlock)
    }

    @Test
    fun `replaceMediaFileWithUrlInGutenbergPost replaces temporary local id and url for image block with colliding prefixes`() {
        val oldContent = oldImageBlock + imageBlockWithPrefixCollision
        val newContent = newImageBlock + imageBlockWithPrefixCollision
        val processedContent = PostUtils.replaceMediaFileWithUrlInGutenbergPost(oldContent, localMediaId, mediaFile,
                siteUrl)
        Assertions.assertThat(processedContent).isEqualTo(newContent)
    }

    @Test
    fun `replaceMediaFileWithUrlInGutenbergPost replaces temporary local id and url for media-text block`() {
        val processedContent = PostUtils.replaceMediaFileWithUrlInGutenbergPost(oldMediaTextBlock, localMediaId,
                mediaFile, siteUrl)
        Assertions.assertThat(processedContent).isEqualTo(newMediaTextBlock)
    }

    @Test
    fun `replaceMediaFileWithUrlInGutenbergPost also works with video`() {
        whenever(mediaFile.fileURL).thenReturn(remoteVideoUrl)
        val processedContent = PostUtils.replaceMediaFileWithUrlInGutenbergPost(oldMediaTextBlockWithVideo,
                localMediaId, mediaFile, siteUrl)
        Assertions.assertThat(processedContent).isEqualTo(newMediaTextBlockWithVideo)
    }
}
