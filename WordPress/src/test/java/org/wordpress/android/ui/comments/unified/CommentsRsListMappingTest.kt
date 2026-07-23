package org.wordpress.android.ui.comments.unified

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.wordpress.android.fluxc.model.CommentStatus.APPROVED
import uniffi.wp_api.CommentContentWithViewContext
import uniffi.wp_api.CommentType
import uniffi.wp_api.CommentWithViewContext
import uniffi.wp_api.UserAvatarSize
import uniffi.wp_api.WpAdditionalFields
import uniffi.wp_api.WpApiParamCommentsOrderBy
import uniffi.wp_api.WpApiParamOrder
import java.util.Date
import uniffi.wp_api.CommentStatus as RsCommentStatus

class CommentsRsListMappingTest {
    @Test
    fun `toRsComment maps the fields the detail and list screens need`() {
        val item = rsComment().toRsComment()

        assertThat(item.remoteCommentId).isEqualTo(COMMENT_ID)
        assertThat(item.authorId).isEqualTo(7L)
        assertThat(item.parentId).isEqualTo(0L)
        assertThat(item.authorName).isEqualTo("Jane")
        assertThat(item.authorAvatarUrl).isEqualTo("https://example.com/avatar96.png")
        assertThat(item.dateGmt).isEqualTo(DATE_GMT)
        assertThat(item.contentHtml).isEqualTo("<p>hello</p>")
        assertThat(item.url).isEqualTo("https://example.com/post/#comment-42")
        assertThat(item.postId).isEqualTo(POST_ID)
        assertThat(item.status).isEqualTo(APPROVED)
    }

    @Test
    fun `toRsComment carries the parent id used for Unreplied threading`() {
        assertThat(rsComment(parent = 42L).toRsComment().parentId).isEqualTo(42L)
    }

    @Test
    fun `pickAvatarUrl prefers size 96`() {
        val comment = rsComment(
            avatarUrls = mapOf(
                UserAvatarSize.Size24 to "https://example.com/avatar24.png",
                UserAvatarSize.Size96 to "https://example.com/avatar96.png"
            )
        )

        assertThat(comment.pickAvatarUrl()).isEqualTo("https://example.com/avatar96.png")
    }

    @Test
    fun `pickAvatarUrl falls back to the first non-empty url`() {
        val comment = rsComment(
            avatarUrls = mapOf(
                UserAvatarSize.Size24 to "",
                UserAvatarSize.Size48 to "https://example.com/avatar48.png"
            )
        )

        assertThat(comment.pickAvatarUrl()).isEqualTo("https://example.com/avatar48.png")
    }

    @Test
    fun `pickAvatarUrl returns empty when there are no avatar urls`() {
        assertThat(rsComment(avatarUrls = emptyMap()).pickAvatarUrl()).isEmpty()
    }

    @Test
    fun `firstPageParams requests newest comments with the given status and search`() {
        val dataSource = CommentsRsDataSource(mock())

        val params = dataSource.firstPageParams(RsCommentStatus.Spam, search = "query")

        assertThat(params.perPage).isEqualTo(CommentsRsDataSource.COMMENTS_PAGE_SIZE)
        assertThat(params.status).isEqualTo(RsCommentStatus.Spam)
        assertThat(params.search).isEqualTo("query")
        assertThat(params.orderby).isEqualTo(WpApiParamCommentsOrderBy.DATE_GMT)
        assertThat(params.order).isEqualTo(WpApiParamOrder.DESC)
    }

    private fun rsComment(
        status: RsCommentStatus = RsCommentStatus.Approved,
        parent: Long = 0L,
        avatarUrls: Map<UserAvatarSize, String> = mapOf(
            UserAvatarSize.Size96 to "https://example.com/avatar96.png"
        )
    ) = CommentWithViewContext(
        id = COMMENT_ID,
        author = 7L,
        authorName = "Jane",
        authorUrl = "https://example.com",
        content = CommentContentWithViewContext(rendered = "<p>hello</p>"),
        date = "2026-07-01T12:00:00",
        dateGmt = DATE_GMT,
        link = "https://example.com/post/#comment-42",
        parent = parent,
        post = POST_ID,
        status = status,
        commentType = CommentType.Comment,
        authorAvatarUrls = avatarUrls,
        // Mocked: the real WpAdditionalFields constructor loads the uniffi native library,
        // which isn't available in local unit tests.
        additionalFields = mock<WpAdditionalFields>()
    )

    companion object {
        private const val COMMENT_ID = 42L
        private const val POST_ID = 99L
        private val DATE_GMT = Date(1_700_000_000_000)
    }
}
