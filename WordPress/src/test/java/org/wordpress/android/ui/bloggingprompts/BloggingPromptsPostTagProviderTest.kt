package org.wordpress.android.ui.bloggingprompts

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.BaseUnitTest
import org.wordpress.android.models.ReaderTag
import org.wordpress.android.models.ReaderTagType
import org.wordpress.android.ui.reader.repository.ReaderPostRepository
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class BloggingPromptsPostTagProviderTest : BaseUnitTest() {
    @Mock
    private lateinit var readerUtilsWrapper: ReaderUtilsWrapper

    lateinit var tagProvider: BloggingPromptsPostTagProvider

    @Before
    fun setUp() {
        tagProvider = BloggingPromptsPostTagProvider(readerUtilsWrapper)
    }

    @Test
    fun `Should return the expected tag when promptIdTag is called given valid id`() {
        val actual = tagProvider.promptIdTag(1234)

        assertThat(actual).isEqualTo(BLOGGING_PROMPT_ID_TAG)
    }

    @Test
    fun `Should return the generic tag when promptIdTag is called given invalid id`() {
        val actual = tagProvider.promptIdTag(0)

        assertThat(actual).isEqualTo(BloggingPromptsPostTagProvider.BLOGGING_PROMPT_TAG)
    }

    @Test
    fun `Should return the expected ReaderTag when promptSearchReaderTag is called`() {
        whenever(readerUtilsWrapper.getTagFromTagUrl(any())).thenReturn(BLOGGING_PROMPT_ID_TAG)
        val expected = ReaderTag(
            BLOGGING_PROMPT_ID_TAG,
            BLOGGING_PROMPT_ID_TAG,
            BLOGGING_PROMPT_ID_TAG,
            ReaderPostRepository.formatFullEndpointForTag(BLOGGING_PROMPT_ID_TAG),
            ReaderTagType.FOLLOWED,
        )
        val actual = tagProvider.promptSearchReaderTag("valid-url")
        assertEquals(expected, actual)
    }

    @Test
    fun `Should return the base Prompt ReaderTag when promptSearchReaderTag is called`() {
        whenever(readerUtilsWrapper.getTagFromTagUrl(any())).thenReturn("")
        val expected = ReaderTag(
            BLOGGING_PROMPT_TAG,
            BLOGGING_PROMPT_TAG,
            BLOGGING_PROMPT_TAG,
            ReaderPostRepository.formatFullEndpointForTag(BLOGGING_PROMPT_TAG),
            ReaderTagType.FOLLOWED,
        )
        val actual = tagProvider.promptSearchReaderTag("invalid-url")
        assertEquals(expected, actual)
    }

    companion object {
        private const val BLOGGING_PROMPT_ID_TAG = "dailyprompt-1234"
        private const val BLOGGING_PROMPT_TAG = "dailyprompt"
    }
}
