package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

object TestContent {
    // TODO: prettify the test content
    const val siteUrl = "https://wordpress.org"
    const val remoteImageUrl = "https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png"
    const val remoteVideoUrl = "https://videos.files.wordpress.com/qeJFeNa2/macintosh-plus-floral-shoppe-02-e383aae382b5e38395e383a9e383b3e382af420-e78fbee4bba3e381aee382b3e383b3e38394e383a5e383bc-1_hd.mp4"
    const val localMediaId = "112"
    const val remoteMediaId = "97629"
    const val attachmentPageUrl = "https://wordpress.org?p=97629"
    const val oldImageBlock = """<!-- wp:image {"id":112,"align":"full"} -->
<figure class="wp-block-image alignfull"><img src="file://Screenshot-1-1.png" alt="" class="wp-image-112"><figcaption><em>Gutenberg</em> on web</figcaption></figure>
<!-- /wp:image -->
"""
    const val newImageBlock = """<!-- wp:image {"id":97629,"align":"full"} -->
<figure class="wp-block-image alignfull"><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" class="wp-image-97629"><figcaption><em>Gutenberg</em> on web</figcaption></figure>
<!-- /wp:image -->
"""
    const val imageBlockWithPrefixCollision = """<!-- wp:image {"id":11242,"align":"full"} -->
<figure class="wp-block-image alignfull"><img src="https://wordpress.org/gutenberg/files/2019/07/Screenshot-4-2.png" alt="" class="wp-image-11242"><figcaption><em>Gutenberg</em> on web</figcaption></figure>
<!-- /wp:image -->
"""
    const val oldMediaTextBlock = """<!-- wp:media-text {"mediaId":112,"mediaType":"image"} -->
<div class="wp-block-media-text alignwide is-stacked-on-mobile"><figure class="wp-block-media-text__media"><img src="file://img_20191202_094944-18.jpg" alt="" class="wp-image-112"></figure><div class="wp-block-media-text__content"><!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
<p class="has-large-font-size"></p>
<!-- /wp:paragraph --></div></div>
<!-- /wp:media-text -->
"""
    const val newMediaTextBlock = """<!-- wp:media-text {"mediaId":97629,"mediaType":"image"} -->
<div class="wp-block-media-text alignwide is-stacked-on-mobile"><figure class="wp-block-media-text__media"><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" class="wp-image-97629"></figure><div class="wp-block-media-text__content"><!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
<p class="has-large-font-size"></p>
<!-- /wp:paragraph --></div></div>
<!-- /wp:media-text -->
"""
    const val oldMediaTextBlockWithVideo = """<!-- wp:media-text {"mediaId":112,"mediaType":"video"} -->
<div class="wp-block-media-text alignwide"><figure class="wp-block-media-text__media"><video controls src="file://local-video.mov"></video></figure><div class="wp-block-media-text__content"><!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
<p class="has-large-font-size"></p>
<!-- /wp:paragraph --></div></div>
<!-- /wp:media-text -->
"""
    const val newMediaTextBlockWithVideo = """<!-- wp:media-text {"mediaId":97629,"mediaType":"video"} -->
<div class="wp-block-media-text alignwide"><figure class="wp-block-media-text__media"><video controls src="https://videos.files.wordpress.com/qeJFeNa2/macintosh-plus-floral-shoppe-02-e383aae382b5e38395e383a9e383b3e382af420-e78fbee4bba3e381aee382b3e383b3e38394e383a5e383bc-1_hd.mp4"></video></figure><div class="wp-block-media-text__content"><!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
<p class="has-large-font-size"></p>
<!-- /wp:paragraph --></div></div>
<!-- /wp:media-text -->
"""
    const val oldGalleryBlock = """<!-- wp:gallery {"ids":[203,112,369]} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></figure></li><li class="blocks-gallery-item"><figure><img src="file://pexels-photo-1260968.jpeg?w=683" alt="" data-id="112" data-full-url="file://pexels-photo-1260968.jpeg" data-link="file://pexels-photo-1260968/" class="wp-image-112"></figure></li><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    const val oldGalleryBlockMixTypeIds = """<!-- wp:gallery {"ids":[203,"112",369]} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></figure></li><li class="blocks-gallery-item"><figure><img src="file://pexels-photo-1260968.jpeg?w=683" alt="" data-id="112" data-full-url="file://pexels-photo-1260968.jpeg" data-link="file://pexels-photo-1260968/" class="wp-image-112"></figure></li><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    const val newGalleryBlock = """<!-- wp:gallery {"ids":[203,97629,369]} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></figure></li><li class="blocks-gallery-item"><figure><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" data-id="97629" data-full-url="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" data-link="https://wordpress.org?p=97629" class="wp-image-97629"></figure></li><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    const val oldGalleryBlockMixTypeIds2 = """<!-- wp:gallery {"ids":[203,"369",112]} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></figure></li><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></figure></li><li class="blocks-gallery-item"><figure><img src="file://pexels-photo-1260968.jpeg?w=683" alt="" data-id="112" data-full-url="file://pexels-photo-1260968.jpeg" data-link="file://pexels-photo-1260968/" class="wp-image-112"></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    const val newGalleryBlockWithMixTypeIds2 = """<!-- wp:gallery {"ids":[203,"369",97629]} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></figure></li><li class="blocks-gallery-item"><figure><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></figure></li><li class="blocks-gallery-item"><figure><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" data-id="97629" data-full-url="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" data-link="https://wordpress.org?p=97629" class="wp-image-97629"></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    const val paragraphBlock = """<!-- wp:paragraph {"align":"center","fontSize":"small","className":"gutenberg-landing\u002d\u002dbutton-disclaimer"} -->
<p class="has-text-align-center has-small-font-size gutenberg-landing--button-disclaimer"><em>Gutenberg is available as a plugin today, and will be included in version 5.0 of WordPress. The <a href="https://wordpress.org/plugins/classic-editor/">classic editor</a> will be available as a plugin if needed.</em></p>
<!-- /wp:paragraph -->
""""
    const val oldVideoBlock = """<!-- wp:video {"id":112} -->
<figure class="wp-block-video"><video controls src="file://local-video.mov"></video><figcaption>Videos too!</figcaption></figure>
<!-- /wp:video -->
"""
    const val newVideoBlock = """<!-- wp:video {"id":97629} -->
<figure class="wp-block-video"><video controls src="https://videos.files.wordpress.com/qeJFeNa2/macintosh-plus-floral-shoppe-02-e383aae382b5e38395e383a9e383b3e382af420-e78fbee4bba3e381aee382b3e383b3e38394e383a5e383bc-1_hd.mp4"></video><figcaption>Videos too!</figcaption></figure>
<!-- /wp:video -->
"""
    const val oldGalleryBlockLinkToMediaFile = """<!-- wp:gallery {"ids":[203,112,369],"linkTo":"media"} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></a></figure></li><li class="blocks-gallery-item"><figure><a href="file://local-image.jpg"><img src="file://local-image.jpg" alt="" data-id="112" data-full-url="file://local-image.jpg" data-link="file://local-image.jpg" class="wp-image-112"></a></figure></li><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></a></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    const val newGalleryBlockLinkToMediaFile = """<!-- wp:gallery {"ids":[203,97629,369],"linkTo":"media"} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></a></figure></li><li class="blocks-gallery-item"><figure><a href="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png"><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" data-id="97629" data-full-url="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" data-link="https://wordpress.org?p=97629" class="wp-image-97629"></a></figure></li><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></a></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    const val oldGalleryBlockLinkToAttachmentPage = """<!-- wp:gallery {"ids":[203,112,369],"linkTo":"attachment"} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></a></figure></li><li class="blocks-gallery-item"><figure><a href="file://local-image.jpg"><img src="file://local-image.jpg" alt="" data-id="112" data-full-url="file://local-image.jpg" data-link="file://local-image.jpg" class="wp-image-112"></a></figure></li><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></a></figure></li></ul></figure>
<!-- /wp:gallery -->
"""
    const val newGalleryBlockLinkToAttachmentPage = """<!-- wp:gallery {"ids":[203,97629,369],"linkTo":"attachment"} -->
<figure class="wp-block-gallery columns-3 is-cropped"><ul class="blocks-gallery-grid"><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024" alt="" data-id="203" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/" class="wp-image-203"></a></figure></li><li class="blocks-gallery-item"><figure><a href="https://wordpress.org?p=97629"><img src="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" alt="" data-id="97629" data-full-url="https://wordpress.org/gutenberg/files/2018/07/Screenshot-1-1.png" data-link="https://wordpress.org?p=97629" class="wp-image-97629"></a></figure></li><li class="blocks-gallery-item"><figure><a href="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg"><img src="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768" alt="" data-id="369" data-full-url="https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg" data-link="http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369" class="wp-image-369"></a></figure></li></ul></figure>
<!-- /wp:gallery -->
"""

    const val oldPostImage = paragraphBlock + oldImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    const val newPostImage = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    const val oldPostVideo = paragraphBlock + newImageBlock + oldVideoBlock + newMediaTextBlock + newGalleryBlock
    const val newPostVideo = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    const val oldPostMediaText = paragraphBlock + newImageBlock + newVideoBlock + oldMediaTextBlock + newGalleryBlock
    const val newPostMediaText = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    const val oldPostGallery = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + oldGalleryBlock
    const val newPostGallery = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
}
