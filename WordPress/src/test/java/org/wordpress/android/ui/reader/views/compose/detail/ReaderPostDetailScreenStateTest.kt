package org.wordpress.android.ui.reader.views.compose.detail

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.wordpress.android.ui.reader.discover.ReaderPostActions
import org.wordpress.android.ui.reader.discover.ReaderPostCardAction.PrimaryAction
import org.wordpress.android.ui.reader.discover.ReaderPostCardActionType
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ErrorUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.LoadingUiState
import org.wordpress.android.ui.reader.viewmodels.ReaderPostDetailViewModel.UiState.ReaderPostDetailsUiState
import org.wordpress.android.ui.reader.views.uistates.FollowButtonUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderBlogSectionUiState
import org.wordpress.android.ui.reader.views.uistates.ReaderPostDetailsHeaderUiState
import org.wordpress.android.ui.utils.UiString.UiStringText
import org.wordpress.android.util.image.ImageType.BLAVATAR_CIRCULAR

class ReaderPostDetailChromeStateTest {
    private val chrome = ReaderPostDetailChromeState(canScroll = { true })

    @Test
    fun `GIVEN nothing replaced WHEN details show THEN they're current`() {
        val details = detailsState(postId = 1)

        assertThat(chrome.currentDetailsState(details)).isSameAs(details)
    }

    @Test
    fun `GIVEN a post replaced in place WHEN a copy of its state arrives THEN it isn't current`() {
        val replaced = detailsState(postId = 1)
        chrome.replacedState = replaced

        assertThat(chrome.currentDetailsState(replaced.copy(globalRelatedPosts = null))).isNull()
    }

    @Test
    fun `GIVEN a post replaced in place WHEN the same post id arrives from another blog THEN it's current`() {
        chrome.replacedState = detailsState(postId = 1, blogId = 2)
        val details = detailsState(postId = 1, blogId = 3)

        assertThat(chrome.currentDetailsState(details)).isSameAs(details)
    }

    @Test
    fun `GIVEN a post replaced in place WHEN the new post's state arrives THEN it's current`() {
        chrome.replacedState = detailsState(postId = 1)
        val details = detailsState(postId = 2)

        assertThat(chrome.currentDetailsState(details)).isSameAs(details)
    }

    @Test
    fun `GIVEN the replaced state wasn't details WHEN details show THEN they're current`() {
        chrome.replacedState = LoadingUiState
        val details = detailsState(postId = 1)

        assertThat(chrome.currentDetailsState(details)).isSameAs(details)
    }

    @Test
    fun `WHEN the post isn't showing details THEN there are no current details`() {
        assertThat(chrome.currentDetailsState(null)).isNull()
        assertThat(chrome.currentDetailsState(LoadingUiState)).isNull()
        assertThat(chrome.currentDetailsState(ErrorUiState(message = null))).isNull()
    }

    @Test
    fun `WHEN the post fails to load THEN no top bar actions show`() {
        val state = topBarState(ErrorUiState(message = null), postHasUrl = true, isReadingPreferencesEnabled = true)

        assertThat(state).isEqualTo(
            ReaderPostDetailTopBarState(
                showReadingPreferences = false,
                showBrowse = false,
                showShare = false,
                showMore = false,
            )
        )
    }

    @Test
    fun `WHEN the post loads THEN only More shows`() {
        val state = topBarState(LoadingUiState, postHasUrl = true, isReadingPreferencesEnabled = true)

        assertThat(state).isEqualTo(ReaderPostDetailTopBarState(showMore = true))
    }

    @Test
    fun `WHEN a blocked post loads THEN no top bar actions show`() {
        val state = topBarState(LoadingUiState, isPostBlocked = true)

        assertThat(state.showMore).isFalse()
    }

    @Test
    fun `GIVEN a post with a URL WHEN it shows THEN every action shows`() {
        val state = topBarState(detailsState(postId = 1), postHasUrl = true, isReadingPreferencesEnabled = true)

        assertThat(state).isEqualTo(
            ReaderPostDetailTopBarState(
                showReadingPreferences = true,
                showBrowse = true,
                showShare = true,
                showMore = true,
            )
        )
    }

    @Test
    fun `GIVEN reading preferences are off WHEN a post shows THEN reading preferences hide`() {
        val state = topBarState(detailsState(postId = 1), postHasUrl = true, isReadingPreferencesEnabled = false)

        assertThat(state.showReadingPreferences).isFalse()
    }

    @Test
    fun `GIVEN a post without a URL WHEN it shows THEN browse and share hide`() {
        val state = topBarState(detailsState(postId = 1), postHasUrl = false)

        assertThat(state.showBrowse).isFalse()
        assertThat(state.showShare).isFalse()
    }

    @Test
    fun `GIVEN a post without a URL but an intercepted URI WHEN it shows THEN browse shows and share hides`() {
        val state = topBarState(detailsState(postId = 1), postHasUrl = false, hasInterceptedUri = true)

        assertThat(state.showBrowse).isTrue()
        assertThat(state.showShare).isFalse()
    }

    @Test
    fun `GIVEN a blocked post WHEN it shows THEN More hides`() {
        val state = topBarState(detailsState(postId = 1), isPostBlocked = true, postHasUrl = true)

        assertThat(state.showMore).isFalse()
        assertThat(state.showShare).isTrue()
    }

    private fun topBarState(
        uiState: UiState?,
        isPostBlocked: Boolean = false,
        postHasUrl: Boolean = false,
        hasInterceptedUri: Boolean = false,
        isReadingPreferencesEnabled: Boolean = false,
    ) = ReaderPostDetailTopBarState.from(
        uiState = uiState,
        isPostBlocked = isPostBlocked,
        postHasUrl = postHasUrl,
        hasInterceptedUri = hasInterceptedUri,
        isReadingPreferencesEnabled = isReadingPreferencesEnabled,
    )

    private fun detailsState(postId: Long, blogId: Long = BLOG_ID) = ReaderPostDetailsUiState(
        postId = postId,
        blogId = blogId,
        headerUiState = ReaderPostDetailsHeaderUiState(
            title = UiStringText(""),
            authorName = null,
            tagItems = emptyList(),
            blogSectionUiState = ReaderBlogSectionUiState(
                postId = postId,
                blogId = blogId,
                dateLine = "",
                blogName = UiStringText(""),
                blogUrl = null,
                avatarOrBlavatarUrl = null,
                authorAvatarUrl = null,
                isAuthorAvatarVisible = false,
                blavatarType = BLAVATAR_CIRCULAR,
                blogSectionClickData = null,
            ),
            followButtonUiState = FollowButtonUiState(onFollowButtonClicked = null, isFollowed = false),
        ),
        excerptFooterUiState = null,
        actions = ReaderPostActions(
            likeAction = PrimaryAction(isEnabled = true, type = ReaderPostCardActionType.LIKE),
            reblogAction = PrimaryAction(isEnabled = true, type = ReaderPostCardActionType.REBLOG),
            commentsAction = PrimaryAction(isEnabled = true, type = ReaderPostCardActionType.COMMENTS),
            bookmarkAction = PrimaryAction(isEnabled = true, type = ReaderPostCardActionType.BOOKMARK),
        ),
    )

    companion object {
        private const val BLOG_ID = 10L
    }
}
