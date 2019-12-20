package org.wordpress.android.ui.posts

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions
import org.junit.Test

import org.junit.Before
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.wordpress.android.ui.posts.MediaUploadCompletionProcessorPatterns.Helpers
import org.wordpress.android.ui.posts.MediaUploadCompletionProcessorPatterns.MediaBlockType
import org.wordpress.android.util.helpers.MediaFile

@RunWith(MockitoJUnitRunner::class)
class MediaUploadCompletionProcessorTest {

    // TODO: extract the test content and prettify it
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
    private val oldGalleryBlock = """<!-- wp:gallery {"ids":[203,112,369]} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></figure></li><li class="blocks-gallery-item"><figure><img src="file://pexels-photo-1260968.jpeg?w=683" alt="" data-id="112" data-full-url="file://pexels-photo-1260968.jpeg" data-link="file://pexels-photo-1260968/" class="wp-image-112"></figure></li><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    private val oldGalleryBlockMixTypeIds = """<!-- wp:gallery {"ids":[203,"112",369]} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></figure></li><li class="blocks-gallery-item"><figure><img src="file://pexels-photo-1260968.jpeg?w=683" alt="" data-id="112" data-full-url="file://pexels-photo-1260968.jpeg" data-link="file://pexels-photo-1260968/" class="wp-image-112"></figure></li><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    private val newGalleryBlock = """<!-- wp:gallery {"ids":[203,97629,369]} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></figure></li><li class="blocks-gallery-item"><figure><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" data-id="97629" data-full-url="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" data-link="https://wordpress.org?p=97629" class="wp-image-97629"></figure></li><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    private val oldGalleryBlockMixTypeIds2 = """<!-- wp:gallery {"ids":[203,"369",112]} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></figure></li><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></figure></li><li class="blocks-gallery-item"><figure><img src="file://pexels-photo-1260968.jpeg?w=683" alt="" data-id="112" data-full-url="file://pexels-photo-1260968.jpeg" data-link="file://pexels-photo-1260968/" class="wp-image-112"></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    private val newGalleryBlockWithMixTypeIds2 = """<!-- wp:gallery {"ids":[203,"369",97629]} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></figure></li><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></figure></li><li class="blocks-gallery-item"><figure><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" data-id="97629" data-full-url="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" data-link="https://wordpress.org?p=97629" class="wp-image-97629"></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    private val paragraphBlock = """<!-- wp:paragraph {"align":"center","fontSize":"small","className":"gutenberg-landing\u002d\u002dbutton-disclaimer"} -->
<p class="has-text-align-center has-small-font-size gutenberg-landing--button-disclaimer"><em>Gutenberg is available as a plugin today, and will be included in version 5.0 of WordPress. The <a href="https://wordpress.org/plugins/classic-editor/">classic editor</a> will be available as a plugin if needed.</em></p>
<!-- /wp:paragraph -->
""""
    private val oldVideoBlock = """<!-- wp:video {"id":112} -->
<figure class="wp-block-video"><video controls src="file://local-video.mov"></video><figcaption>Videos too!</figcaption></figure>
<!-- /wp:video -->
"""
    private val newVideoBlock = """<!-- wp:video {"id":97629} -->
<figure class="wp-block-video"><video controls src="https://videos.files.wordpress.com/qeJFeNa2/macintosh-plus-floral-shoppe-02-e383aae382b5e38395e383a9e383b3e382af420-e78fbee4bba3e381aee382b3e383b3e38394e383a5e383bc-1_hd.mp4"></video><figcaption>Videos too!</figcaption></figure>
<!-- /wp:video -->
"""
    private val oldGalleryBlockLinkToMediaFile = """<!-- wp:gallery {"ids":[203,112,369],"linkTo":"media"} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></a></figure></li><li class="blocks-gallery-item"><figure><a href="file://local-image.jpg"><img src="file://local-image.jpg" alt="" data-id="112" data-full-url="file://local-image.jpg" data-link="file://local-image.jpg" class="wp-image-112"></a></figure></li><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></a></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    private val newGalleryBlockLinkToMediaFile = """<!-- wp:gallery {"ids":[203,97629,369],"linkTo":"media"} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></a></figure></li><li class="blocks-gallery-item"><figure><a href="https://wordpress.org?p=97629"><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" data-id="97629" data-full-url="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" data-link="https://wordpress.org?p=97629" class="wp-image-97629"></a></figure></li><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></a></figure></li></ul></figure>
<!-- /wp:gallery -->
"""

    private val oldPostImage = paragraphBlock + oldImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    private val newPostImage = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    private val oldPostVideo = paragraphBlock + newImageBlock + oldVideoBlock + newMediaTextBlock + newGalleryBlock
    private val newPostVideo = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    private val oldPostMediaText = paragraphBlock + newImageBlock + newVideoBlock + oldMediaTextBlock + newGalleryBlock
    private val newPostMediaText = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    private val oldPostGallery = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + oldGalleryBlock
    private val newPostGallery = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock

    private val mediaFile: MediaFile = mock()
    private lateinit var processor : MediaUploadCompletionProcessor

    @Before
    fun before() {
        whenever(mediaFile.mediaId).thenReturn(remoteMediaId)
        whenever(mediaFile.fileURL).thenReturn(remoteImageUrl)
        whenever(mediaFile.getAttachmentPageURL(any())).thenReturn(attachmentPageUrl)
        processor = MediaUploadCompletionProcessor(localMediaId, mediaFile, siteUrl)
    }

    @Test
    fun `processPost splices id and url for an image block`() {
        val blocks = processor.processPost(oldPostImage)
        Assertions.assertThat(blocks).isEqualTo(newPostImage)
    }

    @Test
    fun `processPost splices id and url for a video block`() {
        whenever(mediaFile.fileURL).thenReturn(remoteVideoUrl)
        processor = MediaUploadCompletionProcessor(localMediaId, mediaFile, siteUrl)
        val blocks = processor.processPost(oldPostVideo)
        Assertions.assertThat(blocks).isEqualTo(newPostVideo)
    }

    @Test
    fun `processPost splices id and url for a media-text block`() {
        val blocks = processor.processPost(oldPostMediaText)
        Assertions.assertThat(blocks).isEqualTo(newPostMediaText)
    }

    @Test
    fun `processPost splices id and url for a gallery block`() {
        val blocks = processor.processPost(oldPostGallery)
        Assertions.assertThat(blocks).isEqualTo(newPostGallery)
    }

    @Test
    fun `detectBlockType works for image blocks`() {
        val blockType = Helpers.detectBlockType(newImageBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.IMAGE)
    }

    @Test
    fun `detectBlockType works for video blocks`() {
        val blockType = Helpers.detectBlockType(newVideoBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.VIDEO)
    }

    @Test
    fun `detectBlockType works for media-text blocks`() {
        val blockType = Helpers.detectBlockType(newMediaTextBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.MEDIA_TEXT)
    }

    @Test
    fun `detectBlockType works for gallery blocks`() {
        val blockType = Helpers.detectBlockType(newGalleryBlock)
        Assertions.assertThat(blockType).isEqualTo(MediaBlockType.GALLERY)
    }

    @Test
    fun `detectBlockType returns null for non-media blocks`() {
        val blockType = Helpers.detectBlockType(paragraphBlock)
        Assertions.assertThat(blockType).isNull()
    }

    @Test
    fun `processImageBlock replaces id and url in matching block`() {
        val processedBlock = processor.processImageBlock(oldImageBlock)
        Assertions.assertThat(processedBlock).isEqualTo(newImageBlock)
    }

    @Test
    fun `processImageBlock leaves non-matching block unchanged`() {
        val nonMatchingId = "123"
        val mediaUploadCompletionProcessor = MediaUploadCompletionProcessor(nonMatchingId, mediaFile, siteUrl)
        val processedBlock = mediaUploadCompletionProcessor.processImageBlock(oldImageBlock)
        Assertions.assertThat(processedBlock).isEqualTo(oldImageBlock)
    }

    @Test
    fun `processImageBlock leaves image block with colliding prefix unchanged`() {
        val processedBlock = processor.processImageBlock(
                imageBlockWithPrefixCollision
        )
        Assertions.assertThat(processedBlock).isEqualTo(imageBlockWithPrefixCollision)
    }

    @Test
    fun `processVideoBlock replaces id and url in matching block`() {
        whenever(mediaFile.fileURL).thenReturn(remoteVideoUrl)
        processor = MediaUploadCompletionProcessor(localMediaId, mediaFile, siteUrl)
        val processedBlock = processor.processVideoBlock(oldVideoBlock)
        Assertions.assertThat(processedBlock).isEqualTo(newVideoBlock)
    }


    @Test
    fun `processMediaTextBlock replaces temporary local id and url for media-text block`() {
        val processedBlock = processor.processMediaTextBlock(oldMediaTextBlock)
        Assertions.assertThat(processedBlock).isEqualTo(newMediaTextBlock)
    }

    @Test
    fun `processMediaTextBlock also works for video`() {
        whenever(mediaFile.fileURL).thenReturn(remoteVideoUrl)
        processor = MediaUploadCompletionProcessor(localMediaId, mediaFile, siteUrl)
        val processedBlock = processor.processMediaTextBlock(oldMediaTextBlockWithVideo)
        Assertions.assertThat(processedBlock).isEqualTo(newMediaTextBlockWithVideo)
    }

    @Test
    fun `processGalleryBlock replaces temporary local id and url for gallery block`() {
        val processedBlock = processor.processGalleryBlock(oldGalleryBlock)
        Assertions.assertThat(processedBlock).isEqualTo(newGalleryBlock)
    }

    @Test
    fun `processGalleryBlock can handle ids with mixed types`() {
        val processedBlock = processor.processGalleryBlock(oldGalleryBlockMixTypeIds)
        Assertions.assertThat(processedBlock).isEqualTo(newGalleryBlock)
    }

    @Test
    fun `processGalleryBlock can handle ids with mixed types different order`() {
        val processedBlock = processor.processGalleryBlock(oldGalleryBlockMixTypeIds2)
        Assertions.assertThat(processedBlock).isEqualTo(newGalleryBlockWithMixTypeIds2)
    }

    @Test
    fun `processGalleryBlock can handle LinkTo MediaFile setting`() {
        val processedBlock = processor.processGalleryBlock(oldGalleryBlockLinkToMediaFile)
        Assertions.assertThat(processedBlock).isEqualTo(newGalleryBlockLinkToMediaFile)
    }
}