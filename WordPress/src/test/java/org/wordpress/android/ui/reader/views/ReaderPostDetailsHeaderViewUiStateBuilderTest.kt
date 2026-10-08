package org.wordpress.android.ui.reader.views

import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.wordpress.android.models.ReaderPost
import org.wordpress.android.ui.reader.discover.ReaderPostTagsUiStateBuilder
import org.wordpress.android.ui.reader.discover.ReaderPostUiStateBuilder
import org.wordpress.android.ui.reader.utils.FeaturedImageUtils
import org.wordpress.android.ui.reader.utils.ReaderUtilsWrapper
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.DisplayUtilsWrapper
import org.wordpress.android.util.image.ImageType

@RunWith(MockitoJUnitRunner::class)
class ReaderPostDetailsHeaderViewUiStateBuilderTest {
    @Mock
    lateinit var postUiStateBuilder: ReaderPostUiStateBuilder

    @Mock
    lateinit var readerPostTagsUiStateBuilder: ReaderPostTagsUiStateBuilder

    @Mock
    lateinit var featuredImageUtils: FeaturedImageUtils

    @Mock
    lateinit var readerUtilsWrapper: ReaderUtilsWrapper

    @Mock
    lateinit var displayUtilsWrapper: DisplayUtilsWrapper

    private lateinit var builder: ReaderPostDetailsHeaderViewUiStateBuilder

    @Before
    fun setUp() {
        whenever(postUiStateBuilder.mapPostToBlogSectionUiState(any(), any())).thenReturn(
            ReaderBlogSectionUiState(
                postId = 0L,
                blogId = 0L,
                dateLine = "",
                blogName = UiStringText(""),
                blogUrl = null,
                avatarOrBlavatarUrl = null,
                authorAvatarUrl = null,
                isAuthorAvatarVisible = false,
                blavatarType = ImageType.BLAVATAR,
                blogSectionClickData = null,
            )
        )
        builder = ReaderPostDetailsHeaderViewUiStateBuilder(
            postUiStateBuilder,
            readerPostTagsUiStateBuilder,
            featuredImageUtils,
            readerUtilsWrapper,
            displayUtilsWrapper,
        )
    }

    @Test
    fun `given excerpt generated from br line breaks, when header is built, then excerpt is hidden`() {
        val post = post(
            text = "<p>I kept thinking that<br>the mistake is in the marble.</p>",
            excerpt = "I kept thinking thatthe mistake is in the marble."
        )

        assertThat(builder.mapPostToUiState(post) {}.excerpt).isNull()
    }

    @Test
    fun `given author-written excerpt, when header is built, then excerpt is shown`() {
        val excerpt = "A poem about marble."
        val post = post(text = "<p>I kept thinking that<br>the mistake is in the marble.</p>", excerpt = excerpt)

        assertThat(builder.mapPostToUiState(post) {}.excerpt).isEqualTo(UiStringText(excerpt))
    }

    private fun post(text: String, excerpt: String) = ReaderPost().apply {
        this.text = text
        this.excerpt = excerpt
    }
}
