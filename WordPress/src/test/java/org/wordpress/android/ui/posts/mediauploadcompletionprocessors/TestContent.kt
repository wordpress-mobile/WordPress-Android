package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

/* ktlint-disable max-line-length */
/* ktlint-disable string-template */
object TestContent {
    const val siteUrl = "https://wordpress.org"
    private const val localImageUrl = "file://Screenshot-1-1.png"
    private const val localImageUrl2 = "file://Screenshot-1-2.png"
    const val remoteImageUrl = "https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg"
    const val remoteImageUrl2 = "https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg"
    private const val remoteImageUrlBlogLink = "http://onetwoonetwothisisjustatest.home.blog/pexels-photo-1671668/"
    private const val remoteImageUrlWithSize = "https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/11/pexels-photo-1671668.jpg?w=1024"
    private const val remoteImageUrl2BlogLink = "http://onetwoonetwothisisjustatest.home.blog/?attachment_id=369"
    private const val remoteImageUrl2WithSize = "https://onetwoonetwothisisjustatesthome.files.wordpress.com/2019/12/img_20191202_094944-19.jpg?w=768"
    private const val localVideoUrl = "file://local-video.mov"
    const val remoteVideoUrl = "https://videos.files.wordpress.com/qeJFeNa2/macintosh-plus-floral-shoppe-02-e383aae382b5e38395e383a9e383b3e382af420-e78fbee4bba3e381aee382b3e383b3e38394e383a5e383bc-1_hd.mp4"
    const val localMediaId = "112"
    const val localMediaId2 = "113"
    private const val collidingPrefixMediaId = "${localMediaId}42"
    private const val collidingSuffixMediaId = "42${localMediaId}"
    const val remoteMediaId = "97629"
    const val remoteMediaId2 = "97630"
    const val attachmentPageUrl = "https://wordpress.org?p=${remoteMediaId}"

    const val oldImageBlock = """<!-- wp:image {"id":${localMediaId},"align":"full"} -->
<figure class="wp-block-image alignfull">
  <img src="$localImageUrl" alt="" class="wp-image-${localMediaId}">
  <figcaption><em>Gutenberg</em> on web</figcaption>
</figure>
<!-- /wp:image -->
"""

    const val newImageBlock = """<!-- wp:image {"id":${remoteMediaId},"align":"full"} -->
<figure class="wp-block-image alignfull">
  <img src="$remoteImageUrl" alt="" class="wp-image-${remoteMediaId}">
  <figcaption><em>Gutenberg</em> on web</figcaption>
</figure>
<!-- /wp:image -->
"""

    const val oldImageBlockIdNotFirst = """<!-- wp:image {"align":"full","id":${localMediaId}} -->
<figure class="wp-block-image alignfull">
  <img src="$localImageUrl" alt="" class="wp-image-${localMediaId}">
  <figcaption><em>Gutenberg</em> on web</figcaption>
</figure>
<!-- /wp:image -->
"""

    const val newImageBlockIdNotFirst = """<!-- wp:image {"align":"full","id":${remoteMediaId}} -->
<figure class="wp-block-image alignfull">
  <img src="$remoteImageUrl" alt="" class="wp-image-${remoteMediaId}">
  <figcaption><em>Gutenberg</em> on web</figcaption>
</figure>
<!-- /wp:image -->
"""

    const val imageBlockWithPrefixCollision = """<!-- wp:image {"id":${collidingPrefixMediaId},"align":"full"} -->
<figure class="wp-block-image alignfull">
  <img src="$remoteImageUrl2" alt="" class="wp-image-${collidingPrefixMediaId}">
  <figcaption><em>Gutenberg</em> on web</figcaption>
</figure>
<!-- /wp:image -->
"""

    const val imageBlockWithSuffixCollision = """<!-- wp:image {"id":${collidingSuffixMediaId},"align":"full"} -->
<figure class="wp-block-image alignfull">
  <img src="$remoteImageUrl2" alt="" class="wp-image-${collidingSuffixMediaId}">
  <figcaption><em>Gutenberg</em> on web</figcaption>
</figure>
<!-- /wp:image -->
"""

    const val oldMediaTextBlock = """<!-- wp:media-text {"mediaId":${localMediaId},"mediaType":"image"} -->
<div class="wp-block-media-text alignwide is-stacked-on-mobile">
  <figure class="wp-block-media-text__media"><img src="$localImageUrl" alt="" class="wp-image-${localMediaId}"></figure>
  <div class="wp-block-media-text__content">
    <!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
    <p class="has-large-font-size"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:media-text -->
"""

    const val newMediaTextBlock = """<!-- wp:media-text {"mediaId":${remoteMediaId},"mediaType":"image"} -->
<div class="wp-block-media-text alignwide is-stacked-on-mobile">
  <figure class="wp-block-media-text__media"><img src="$remoteImageUrl" alt="" class="wp-image-${remoteMediaId}"></figure>
  <div class="wp-block-media-text__content">
    <!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
    <p class="has-large-font-size"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:media-text -->
"""

    const val oldMediaTextBlockIdNotFirst = """<!-- wp:media-text {"mediaType":"image","mediaId":${localMediaId}} -->
<div class="wp-block-media-text alignwide is-stacked-on-mobile">
  <figure class="wp-block-media-text__media"><img src="$localImageUrl" alt="" class="wp-image-${localMediaId}"></figure>
  <div class="wp-block-media-text__content">
    <!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
    <p class="has-large-font-size"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:media-text -->
"""

    const val newMediaTextBlockIdNotFirst = """<!-- wp:media-text {"mediaType":"image","mediaId":${remoteMediaId}} -->
<div class="wp-block-media-text alignwide is-stacked-on-mobile">
  <figure class="wp-block-media-text__media"><img src="$remoteImageUrl" alt="" class="wp-image-${remoteMediaId}"></figure>
  <div class="wp-block-media-text__content">
    <!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
    <p class="has-large-font-size"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:media-text -->
"""

    const val oldMediaTextBlockWithVideo = """<!-- wp:media-text {"mediaId":${localMediaId},"mediaType":"video"} -->
<div class="wp-block-media-text alignwide">
  <figure class="wp-block-media-text__media"><video controls src="$localVideoUrl"></video></figure>
  <div class="wp-block-media-text__content">
    <!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
    <p class="has-large-font-size"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:media-text -->
"""

    const val newMediaTextBlockWithVideo = """<!-- wp:media-text {"mediaId":${remoteMediaId},"mediaType":"video"} -->
<div class="wp-block-media-text alignwide">
  <figure class="wp-block-media-text__media"><video controls src="$remoteVideoUrl"></video></figure>
  <div class="wp-block-media-text__content">
    <!-- wp:paragraph {"placeholder":"Content…","fontSize":"large"} -->
    <p class="has-large-font-size"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:media-text -->
"""

    const val oldGalleryBlock = """<!-- wp:gallery {"ids":[203,${localMediaId},369]} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$localImageUrl" alt="" data-id="$localMediaId" data-full-url="$localImageUrl" data-link="$localImageUrl" class="wp-image-${localMediaId}"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val newGalleryBlock = """<!-- wp:gallery {"ids":[203,${remoteMediaId},369]} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrl" alt="" data-id="$remoteMediaId" data-full-url="$remoteImageUrl" data-link="$attachmentPageUrl" class="wp-image-${remoteMediaId}"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val oldGalleryBlockIdsNotFirst = """<!-- wp:gallery {"linkTo":"attachment","ids":[203,${localMediaId},369]} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$localImageUrl" alt="" data-id="$localMediaId" data-full-url="$localImageUrl" data-link="$localImageUrl" class="wp-image-${localMediaId}"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val newGalleryBlockIdsNotFirst = """<!-- wp:gallery {"linkTo":"attachment","ids":[203,${remoteMediaId},369]} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrl" alt="" data-id="$remoteMediaId" data-full-url="$remoteImageUrl" data-link="$attachmentPageUrl" class="wp-image-${remoteMediaId}"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val oldGalleryBlockMixTypeIds = """<!-- wp:gallery {"ids":[203,"$localMediaId",369]} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$localImageUrl" alt="" data-id="$localMediaId" data-full-url="$localImageUrl" data-link="$localImageUrl" class="wp-image-${localMediaId}"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val oldGalleryBlockMixTypeIds2 = """<!-- wp:gallery {"ids":[203,"369",${localMediaId}]} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$localImageUrl" alt="" data-id="$localMediaId" data-full-url="$localImageUrl" data-link="$localImageUrl" class="wp-image-${localMediaId}"></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val newGalleryBlockWithMixTypeIds2 = """<!-- wp:gallery {"ids":[203,"369",${remoteMediaId}]} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><img src="$remoteImageUrl" alt="" data-id="$remoteMediaId" data-full-url="$remoteImageUrl" data-link="$attachmentPageUrl" class="wp-image-${remoteMediaId}"></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val paragraphBlock = """<!-- wp:paragraph {"align":"center","fontSize":"small","className":"gutenberg-landing\u002d\u002dbutton-disclaimer"} -->
<p class="has-text-align-center has-small-font-size gutenberg-landing--button-disclaimer"><em>Gutenberg is available as a plugin today, and will be included in version 5.0 of WordPress. The <a href="https://wordpress.org/plugins/classic-editor/">classic editor</a> will be available as a plugin if needed.</em></p>
<!-- /wp:paragraph -->
""""

    const val oldVideoBlock = """<!-- wp:video {"id":${localMediaId}} -->
<figure class="wp-block-video"><video controls src="$localVideoUrl"></video>
  <figcaption>Videos too!</figcaption>
</figure>
<!-- /wp:video -->
"""

    const val newVideoBlock = """<!-- wp:video {"id":${remoteMediaId}} -->
<figure class="wp-block-video"><video controls src="$remoteVideoUrl"></video>
  <figcaption>Videos too!</figcaption>
</figure>
<!-- /wp:video -->
"""

    const val oldVideoBlockIdNotFirst = """<!-- wp:video {"preload":"none","id":${localMediaId}} -->
<figure class="wp-block-video"><video controls src="$localVideoUrl"></video>
  <figcaption>Videos too!</figcaption>
</figure>
<!-- /wp:video -->
"""

    const val newVideoBlockIdNotFirst = """<!-- wp:video {"preload":"none","id":${remoteMediaId}} -->
<figure class="wp-block-video"><video controls src="$remoteVideoUrl"></video>
  <figcaption>Videos too!</figcaption>
</figure>
<!-- /wp:video -->
"""

    const val oldGalleryBlockLinkToMediaFile = """<!-- wp:gallery {"ids":[203,${localMediaId},369],"linkTo":"media"} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><a href="$remoteImageUrl"><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></a></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><a href="$localImageUrl"><img src="$localImageUrl" alt="" data-id="$localMediaId" data-full-url="$localImageUrl" data-link="$localImageUrl" class="wp-image-${localMediaId}"></a></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><a href="$remoteImageUrl2"><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></a></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val newGalleryBlockLinkToMediaFile = """<!-- wp:gallery {"ids":[203,${remoteMediaId},369],"linkTo":"media"} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><a href="$remoteImageUrl"><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></a></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><a href="$remoteImageUrl"><img src="$remoteImageUrl" alt="" data-id="$remoteMediaId" data-full-url="$remoteImageUrl" data-link="$attachmentPageUrl" class="wp-image-${remoteMediaId}"></a></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><a href="$remoteImageUrl2"><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></a></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val oldGalleryBlockLinkToAttachmentPage = """<!-- wp:gallery {"ids":[203,${localMediaId},369],"linkTo":"attachment"} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><a href="$remoteImageUrl"><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></a></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><a href="$localImageUrl"><img src="$localImageUrl" alt="" data-id="$localMediaId" data-full-url="$localImageUrl" data-link="$localImageUrl" class="wp-image-${localMediaId}"></a></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><a href="$remoteImageUrl2"><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></a></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val newGalleryBlockLinkToAttachmentPage = """<!-- wp:gallery {"ids":[203,${remoteMediaId},369],"linkTo":"attachment"} -->
<figure class="wp-block-gallery columns-3 is-cropped">
  <ul class="blocks-gallery-grid">
    <li class="blocks-gallery-item">
      <figure><a href="$remoteImageUrl"><img src="$remoteImageUrlWithSize" alt="" data-id="203" data-full-url="$remoteImageUrl" data-link="$remoteImageUrlBlogLink" class="wp-image-203"></a></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><a href="$attachmentPageUrl"><img src="$remoteImageUrl" alt="" data-id="$remoteMediaId" data-full-url="$remoteImageUrl" data-link="$attachmentPageUrl" class="wp-image-${remoteMediaId}"></a></figure>
    </li>
    <li class="blocks-gallery-item">
      <figure><a href="$remoteImageUrl2"><img src="$remoteImageUrl2WithSize" alt="" data-id="369" data-full-url="$remoteImageUrl2" data-link="$remoteImageUrl2BlogLink" class="wp-image-369"></a></figure>
    </li>
  </ul>
</figure>
<!-- /wp:gallery -->
"""

    const val oldCoverBlock = """<!-- wp:cover {"url":"$localImageUrl","id":$localMediaId,"someOtherAttribute":5} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($localImageUrl)">
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:cover -->
"""

    const val newCoverBlock = """<!-- wp:cover {"url":"$remoteImageUrl","id":$remoteMediaId,"someOtherAttribute":5} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($remoteImageUrl)">
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:cover -->
"""
    const val oldCoverBlockWithNestedImageBlock = """<!-- wp:cover {"url":"$localImageUrl","id":$localMediaId} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($localImageUrl)">
  <div class="wp-block-cover__inner-container">
  $newImageBlock
  </div>
</div>
<!-- /wp:cover -->
"""
    const val newCoverBlockWithNestedImageBlock = """<!-- wp:cover {"url":"$remoteImageUrl","id":$remoteMediaId} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($remoteImageUrl)">
  <div class="wp-block-cover__inner-container">
  $newImageBlock
  </div>
</div>
<!-- /wp:cover -->
"""
    const val oldCoverBlockWithNestedCoverBlockOuter = """<!-- wp:cover {"url":"$localImageUrl2","id":$localMediaId2} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($localImageUrl2)">
  <div class="wp-block-cover__inner-container">
  $oldCoverBlock
  </div>
</div>
<!-- /wp:cover -->
"""
    const val newCoverBlockWithNestedCoverBlockOuter = """<!-- wp:cover {"url":"$remoteImageUrl2","id":$remoteMediaId2} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($remoteImageUrl2)">
  <div class="wp-block-cover__inner-container">
  $oldCoverBlock
  </div>
</div>
<!-- /wp:cover -->
"""
    const val oldCoverBlockWithNestedCoverBlockInner = """<!-- wp:cover {"url":"$localImageUrl2","id":$localMediaId2} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($localImageUrl2)">
  <div class="wp-block-cover__inner-container">
  $oldCoverBlock
  </div>
</div>
<!-- /wp:cover -->
"""
    const val newCoverBlockWithNestedCoverBlockInner = """<!-- wp:cover {"url":"$localImageUrl2","id":$localMediaId2} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($localImageUrl2)">
  <div class="wp-block-cover__inner-container">
  $newCoverBlock
  </div>
</div>
<!-- /wp:cover -->
"""
    const val oldImageBlockNestedInCoverBlock = """<!-- wp:cover {"url":"$remoteImageUrl","id":$remoteMediaId} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($remoteImageUrl)">
  <div class="wp-block-cover__inner-container">
  $oldImageBlock
  </div>
</div>
<!-- /wp:cover -->
"""
    const val newImageBlockNestedInCoverBlock = """<!-- wp:cover {"url":"$remoteImageUrl","id":$remoteMediaId} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($remoteImageUrl)">
  <div class="wp-block-cover__inner-container">
  $newImageBlock
  </div>
</div>
<!-- /wp:cover -->
"""
    const val malformedCoverBlock = """<!-- wp:cover {"url":"$localImageUrl","id":$localMediaId,"someOtherAttribute":5} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($localImageUrl)">
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
"""
    const val oldCoverBlockDifferentStyleOrder = """<!-- wp:cover {"url":"$localImageUrl","id":$localMediaId,"someOtherAttribute":5} -->
<div class="wp-block-cover has-background-dim" style="color:black;background-image:url($localImageUrl)">
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:cover -->
"""

    const val newCoverBlockDifferentStyleOrder = """<!-- wp:cover {"url":"$remoteImageUrl","id":$remoteMediaId,"someOtherAttribute":5} -->
<div class="wp-block-cover has-background-dim" style="color:black;background-image:url($remoteImageUrl)">
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:cover -->
"""
    const val oldCoverBlockStyleOrderWithSpace = """<!-- wp:cover {"url":"$localImageUrl","id":$localMediaId,"someOtherAttribute":5} -->
<div class="wp-block-cover has-background-dim" style="background-image: url($localImageUrl)">
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:cover -->
"""

    const val newCoverBlockStyleOrderWithoutSpace = """<!-- wp:cover {"url":"$remoteImageUrl","id":$remoteMediaId,"someOtherAttribute":5} -->
<div class="wp-block-cover has-background-dim" style="background-image:url($remoteImageUrl)">
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:cover -->
"""

    const val oldCoverBlockWithVideo = """<!-- wp:cover {"url":"$localVideoUrl","id":$localMediaId,"backgroundType":"video"} -->
<div class="wp-block-cover has-background-dim">
  <video class="wp-block-cover__video-background" autoplay muted loop playsinline src="$localVideoUrl"></video>
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:cover -->
"""

    const val newCoverBlockWithVideo = """<!-- wp:cover {"url":"$remoteVideoUrl","id":$remoteMediaId,"backgroundType":"video"} -->
<div class="wp-block-cover has-background-dim">
  <video class="wp-block-cover__video-background" autoplay muted loop playsinline src="$remoteVideoUrl"></video>
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
<!-- /wp:cover -->
"""

    const val oldPostImage = paragraphBlock + oldImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    const val newPostImage = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    const val oldPostVideo = paragraphBlock + newImageBlock + oldVideoBlock + newMediaTextBlock + newGalleryBlock
    const val newPostVideo = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    const val oldPostMediaText = paragraphBlock + newImageBlock + newVideoBlock + oldMediaTextBlock + newGalleryBlock
    const val newPostMediaText = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    const val oldPostGallery = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + oldGalleryBlock
    const val newPostGallery = paragraphBlock + newImageBlock + newVideoBlock + newMediaTextBlock + newGalleryBlock
    const val oldPostCover = paragraphBlock + newImageBlock + oldCoverBlock + newMediaTextBlock + oldGalleryBlock
    const val newPostCover = paragraphBlock + newImageBlock + newCoverBlock + newMediaTextBlock + newGalleryBlock
}
