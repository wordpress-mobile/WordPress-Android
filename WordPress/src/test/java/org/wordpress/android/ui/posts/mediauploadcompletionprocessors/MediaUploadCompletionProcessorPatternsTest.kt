package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import org.assertj.core.api.Assertions
import org.junit.Test
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK_CAPTURES

const val blockType = "image"
const val blockJson = """{"url":"file:///image.png","id":123,someObject:{a:1,b:2},someArray:[1,2,3]}"""
const val blockHTML = """<div class="wp-block-cover has-background-dim" style="background-image:url(file:///image.png)">
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
"""
const val blockHeader = """<!-- wp:$blockType $blockJson -->"""
const val blockClosingComment = """<!-- /wp:$blockType -->"""
const val rawBlock = """$blockHeader
$blockHTML
$blockClosingComment"""
const val nestedRawBlock = """$blockHeader
<div class="wp-block-cover has-background-dim" style="background-image:url(file:///image.png)">
  <div class="wp-block-cover__inner-container">
  $rawBlock
  </div>
</div>
$blockClosingComment
"""

class MediaUploadCompletionProcessorPatternsTest {
    @Test
    fun `PATTERN_BLOCK_CAPTURES captures the block type`() {
        val matcher = PATTERN_BLOCK_CAPTURES.matcher(rawBlock)
        val outcome = matcher.find()
        Assertions.assertThat(outcome).isEqualTo(true)
        Assertions.assertThat(matcher.group(1)).isEqualTo(blockType)
    }
    @Test
    fun `PATTERN_BLOCK_CAPTURES captures the block header json`() {
        val matcher = PATTERN_BLOCK_CAPTURES.matcher(rawBlock)
        Assertions.assertThat(matcher.find()).isEqualTo(true)
        Assertions.assertThat(matcher.group(2)).isEqualTo(blockJson)
    }
    @Test
    fun `PATTERN_BLOCK_CAPTURES captures the block content`() {
        val matcher = PATTERN_BLOCK_CAPTURES.matcher(rawBlock)
        Assertions.assertThat(matcher.find()).isEqualTo(true)
        Assertions.assertThat(matcher.group(3)).isEqualTo(blockHTML + "\n")
    }
}
