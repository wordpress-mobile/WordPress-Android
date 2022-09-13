package org.wordpress.android.ui.posts.mediauploadcompletionprocessors

import org.assertj.core.api.Assertions
import org.junit.Test
import org.wordpress.android.ui.posts.mediauploadcompletionprocessors.MediaUploadCompletionProcessorPatterns.PATTERN_BLOCK_CAPTURES

const val BLOCK_TYPE = "image"
const val BLOCK_JSON = """{"url":"file:///image.png","id":123,someObject:{a:1,b:2},someArray:[1,2,3]}"""
const val BLOCK_HTML = """
<div class="wp-block-cover has-background-dim" style="background-image:url(file:///image.png)">
  <div class="wp-block-cover__inner-container">
    <!-- wp:paragraph {"align":"center","placeholder":"Write title…"} -->
    <p class="has-text-align-center"></p>
    <!-- /wp:paragraph -->
  </div>
</div>
"""
const val BLOCK_HEADER = """<!-- wp:$BLOCK_TYPE $BLOCK_JSON -->"""
const val BLOCK_CLOSING_COMMENT = """<!-- /wp:$BLOCK_TYPE -->"""
const val RAW_BLOCK = """$BLOCK_HEADER
$BLOCK_HTML
$BLOCK_CLOSING_COMMENT"""

class MediaUploadCompletionProcessorPatternsTest {
    @Test
    fun `PATTERN_BLOCK_CAPTURES captures the block type`() {
        val matcher = PATTERN_BLOCK_CAPTURES.matcher(RAW_BLOCK)
        val outcome = matcher.find()
        Assertions.assertThat(outcome).isEqualTo(true)
        Assertions.assertThat(matcher.group(1)).isEqualTo(BLOCK_TYPE)
    }
    @Test
    fun `PATTERN_BLOCK_CAPTURES captures the block header json`() {
        val matcher = PATTERN_BLOCK_CAPTURES.matcher(RAW_BLOCK)
        Assertions.assertThat(matcher.find()).isEqualTo(true)
        Assertions.assertThat(matcher.group(2)).isEqualTo(BLOCK_JSON)
    }
    @Test
    fun `PATTERN_BLOCK_CAPTURES captures the block content`() {
        val matcher = PATTERN_BLOCK_CAPTURES.matcher(RAW_BLOCK)
        Assertions.assertThat(matcher.find()).isEqualTo(true)
        Assertions.assertThat(matcher.group(3)).isEqualTo(BLOCK_HTML + "\n")
    }
}
