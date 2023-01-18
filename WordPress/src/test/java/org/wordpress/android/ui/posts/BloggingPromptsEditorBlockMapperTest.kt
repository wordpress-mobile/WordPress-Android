package org.wordpress.android.ui.posts

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.bloggingprompts.promptslist.BloggingPromptsListFixtures

class BloggingPromptsEditorBlockMapperTest {
    private val classToTest = BloggingPromptsEditorBlockMapper()

    @Test
    fun `Should return the mapped blogging prompt block when map is called`() {
        val prompt = BloggingPromptsListFixtures.DOMAIN_MODEL
        val actual = classToTest.map(prompt)
        val expected = """
        <!-- wp:pullquote -->
        <figure class="wp-block-pullquote"><blockquote><p>${prompt.text}</p></blockquote></figure>
        <!-- /wp:pullquote -->
        <!-- wp:paragraph -->
        <p></p>
        <!-- /wp:paragraph -->
        """
        assertThat(actual).isEqualTo(expected)
    }
}
