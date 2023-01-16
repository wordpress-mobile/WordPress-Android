package org.wordpress.android.ui.posts

import org.wordpress.android.fluxc.model.bloggingprompts.BloggingPromptModel
import javax.inject.Inject

class BloggingPromptsEditorBlockMapper @Inject constructor() {
    fun map(bloggingPromptModel: BloggingPromptModel): String =
        """
        <!-- wp:pullquote -->
        <figure class="wp-block-pullquote"><blockquote><p>${bloggingPromptModel.text}</p></blockquote></figure>
        <!-- /wp:pullquote -->
        <!-- wp:paragraph -->
        <p></p>
        <!-- /wp:paragraph -->
        """
}
