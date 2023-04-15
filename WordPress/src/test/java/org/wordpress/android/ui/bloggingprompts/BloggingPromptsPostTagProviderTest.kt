package org.wordpress.android.ui.bloggingprompts

import org.junit.Test
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.services.post.ReaderPostLogic
import kotlin.test.assertEquals

class BloggingPromptsPostTagProviderTest {
    @Test
    fun `Should return the expected ReaderTag when promptIdSearchReaderTag is called`() {
        val promptId = 1234
        val expected = ReaderTag(
            BloggingPromptsPostTagProvider.promptIdTag(promptId),
            BloggingPromptsPostTagProvider.promptIdTag(promptId),
            BloggingPromptsPostTagProvider.promptIdTag(promptId),
            ReaderPostLogic.formatFullEndpointForTag(BloggingPromptsPostTagProvider.promptIdTag(promptId)),
            ReaderTagType.FOLLOWED,
        )
        val actual = BloggingPromptsPostTagProvider.promptIdSearchReaderTag(promptId)
        assertEquals(expected, actual)
    }
}
