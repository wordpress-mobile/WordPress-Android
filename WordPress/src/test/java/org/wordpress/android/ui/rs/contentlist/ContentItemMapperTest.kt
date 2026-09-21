package org.wordpress.android.ui.rs.contentlist

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.wordpress.android.R
import uniffi.wp_api.AnyPostWithEditContext
import uniffi.wp_api.PostCommentStatus
import uniffi.wp_api.PostStatus
import uniffi.wp_api.SparsePostExcerpt
import uniffi.wp_api.PostTitleWithEditContext
import uniffi.wp_mobile.FullEntityAnyPostWithEditContext
import uniffi.wp_mobile.PostItemState
import java.util.Date

/**
 * The mapper is the one place in the posts/pages merge where behaviour can change without a
 * compile error, so it is covered directly rather than through either view model.
 */
class ContentItemMapperTest {
    // region display state

    @Test
    fun `fresh and stale entities map to a normal row`() {
        assertThat(map(PostItemState.Fresh(entity())).displayState)
            .isEqualTo(ContentDisplayState.NORMAL)
        assertThat(map(PostItemState.Stale(entity())).displayState)
            .isEqualTo(ContentDisplayState.NORMAL)
    }

    @Test
    fun `an entity being refetched keeps its content and says so`() {
        val model = map(PostItemState.FetchingWithData(entity(title = "Kept")))

        assertThat(model.displayState).isEqualTo(ContentDisplayState.FETCHING_WITH_DATA)
        assertThat(model.title).isEqualTo("Kept")
    }

    @Test
    fun `a failed refetch keeps its content and says so`() {
        val model = map(PostItemState.FailedWithData("boom", entity(title = "Kept")))

        assertThat(model.displayState).isEqualTo(ContentDisplayState.FAILED_WITH_DATA)
        assertThat(model.title).isEqualTo("Kept")
    }

    /** The collection reports a row before it has the data for it; the list still renders there. */
    @Test
    fun `a row with no data yet is a blank placeholder carrying only its id`() {
        val model = map(PostItemState.Missing)

        assertThat(model.displayState).isEqualTo(ContentDisplayState.PLACEHOLDER)
        assertThat(model.remoteId).isEqualTo(REMOTE_ID)
        assertThat(model.title).isEmpty()
        assertThat(model.excerpt).isEmpty()
    }

    @Test
    fun `a row whose load failed is a blank error row carrying only its id`() {
        val model = map(PostItemState.Failed("boom"))

        assertThat(model.displayState).isEqualTo(ContentDisplayState.ERROR)
        assertThat(model.remoteId).isEqualTo(REMOTE_ID)
    }

    // endregion

    // region title and excerpt

    @Test
    fun `a blank raw title falls back to the rendered one`() {
        assertThat(map(PostItemState.Fresh(entity(title = "", rendered = "Rendered"))).title)
            .isEqualTo("Rendered")
    }

    @Test
    fun `a raw title wins over the rendered one`() {
        assertThat(map(PostItemState.Fresh(entity(title = "Raw", rendered = "Rendered"))).title)
            .isEqualTo("Raw")
    }

    @Test
    fun `the excerpt is stripped of html and trimmed`() {
        assertThat(map(PostItemState.Fresh(entity(excerpt = " <p>Hello</p> "))).excerpt)
            .isEqualTo("Hello")
    }

    // endregion

    // region badges

    @Test
    fun `a private item is badged as private`() {
        assertThat(map(PostItemState.Fresh(entity(status = PostStatus.Private))).badges)
            .containsExactly(R.string.post_status_post_private)
    }

    @Test
    fun `a pending item is badged as pending review`() {
        assertThat(map(PostItemState.Fresh(entity(status = PostStatus.Pending))).badges)
            .containsExactly(R.string.post_status_pending_review)
    }

    @Test
    fun `a sticky post is badged as sticky`() {
        assertThat(map(PostItemState.Fresh(entity(sticky = true))).badges)
            .containsExactly(R.string.post_status_sticky)
    }

    /**
     * The merge that produced this mapper took the union of the posts and pages badge rules, so
     * page-shaped entities now run the sticky check the pages mapper never had. WordPress does not
     * return a sticky flag for pages, which makes it inert - asserted rather than assumed.
     */
    @Test
    fun `a page-shaped entity with no sticky flag gets no sticky badge`() {
        assertThat(map(PostItemState.Fresh(entity(sticky = null))).badges).isEmpty()
    }

    // endregion

    // region fields

    @Test
    fun `the status label is only resolved while searching`() {
        val entity = entity(status = PostStatus.Draft)

        assertThat(map(PostItemState.Fresh(entity), showStatus = false).statusLabelResId).isZero
        assertThat(map(PostItemState.Fresh(entity), showStatus = true).statusLabelResId)
            .isEqualTo(R.string.post_status_draft)
    }

    @Test
    fun `isTrashed is derived from the status rather than stored`() {
        assertThat(map(PostItemState.Fresh(entity(status = PostStatus.Trash))).isTrashed).isTrue
        assertThat(map(PostItemState.Fresh(entity(status = PostStatus.Draft))).isTrashed).isFalse
    }

    @Test
    fun `a parentless item reports a parent of zero`() {
        assertThat(map(PostItemState.Fresh(entity(parent = null))).parentId).isZero
        assertThat(map(PostItemState.Fresh(entity(parent = 7L))).parentId).isEqualTo(7L)
    }

    @Test
    fun `an open comment status maps to commentsOpen`() {
        assertThat(map(PostItemState.Fresh(entity(commentStatus = PostCommentStatus.Open))).commentsOpen)
            .isTrue
        assertThat(map(PostItemState.Fresh(entity(commentStatus = PostCommentStatus.Closed))).commentsOpen)
            .isFalse
    }

    @Test
    fun `a password protected item reports hasPassword`() {
        assertThat(map(PostItemState.Fresh(entity(password = "secret"))).hasPassword).isTrue
        assertThat(map(PostItemState.Fresh(entity(password = ""))).hasPassword).isFalse
    }

    @Test
    fun `the raw publish date is carried through for date grouping`() {
        assertThat(map(PostItemState.Fresh(entity())).dateGmtMillis).isEqualTo(PUBLISHED_AT.time)
    }

    // endregion

    private fun map(state: PostItemState, showStatus: Boolean = false) =
        state.toContentItemUiModel<TestAction>(REMOTE_ID, NOW_LABEL, showStatus)

    // Only the fields the mapper reads are stubbed; mocking avoids building the ~30-field data
    // class by hand, and the same trick is used in RsToFluxCMapperTest.
    @Suppress("DoNotMockDataClass", "LongParameterList")
    private fun entity(
        title: String = "Title",
        rendered: String = "Rendered",
        excerpt: String = "Excerpt",
        status: PostStatus = PostStatus.Publish,
        sticky: Boolean? = false,
        parent: Long? = null,
        password: String? = null,
        commentStatus: PostCommentStatus = PostCommentStatus.Closed,
    ): FullEntityAnyPostWithEditContext {
        val post: AnyPostWithEditContext = mock()
        whenever(post.id).thenReturn(REMOTE_ID)
        whenever(post.title).thenReturn(PostTitleWithEditContext(title, rendered))
        whenever(post.excerpt).thenReturn(SparsePostExcerpt(excerpt, excerpt, false))
        whenever(post.dateGmt).thenReturn(PUBLISHED_AT)
        whenever(post.modifiedGmt).thenReturn(PUBLISHED_AT)
        whenever(post.link).thenReturn("https://example.com/p")
        whenever(post.status).thenReturn(status)
        whenever(post.sticky).thenReturn(sticky)
        whenever(post.parent).thenReturn(parent)
        whenever(post.password).thenReturn(password)
        whenever(post.commentStatus).thenReturn(commentStatus)
        whenever(post.author).thenReturn(3L)
        whenever(post.featuredMedia).thenReturn(0L)
        return FullEntityAnyPostWithEditContext(entityId = mock(), data = post)
    }

    private enum class TestAction(
        override val labelResId: Int,
        override val iconResId: Int,
        override val isDestructive: Boolean = false
    ) : RsMenuAction {
        ONLY(0, 0)
    }

    companion object {
        private const val REMOTE_ID = 42L
        private const val NOW_LABEL = "now"
        private val PUBLISHED_AT = Date(1_700_000_000_000L)
    }
}
