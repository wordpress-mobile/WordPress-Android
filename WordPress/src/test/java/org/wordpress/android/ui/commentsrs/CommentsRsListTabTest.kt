package org.wordpress.android.ui.commentsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uniffi.wp_api.WpApiParamCommentsStatus

class CommentsRsListTabTest {
    @Test
    fun `all tab queries status all which the server treats as approved plus hold`() {
        assertThat(CommentsRsListTab.ALL.queryStatus).isEqualTo(WpApiParamCommentsStatus.All)
    }

    @Test
    fun `approved tab queries the approve status`() {
        assertThat(CommentsRsListTab.APPROVED.queryStatus).isEqualTo(WpApiParamCommentsStatus.Approve)
    }

    @Test
    fun `unreplied tab queries status all and is threaded client-side`() {
        // Unreplied has no server status: it fetches approved + hold, like ALL, and
        // filterUnreplied() narrows it to comments the user hasn't replied to.
        assertThat(CommentsRsListTab.UNREPLIED.queryStatus).isEqualTo(WpApiParamCommentsStatus.All)
    }

    @Test
    fun `unreplied tab sits between pending and approved, matching the legacy order`() {
        assertThat(CommentsRsListTab.entries.map { it.name }).containsExactly(
            "ALL", "PENDING", "UNREPLIED", "APPROVED", "SPAM", "TRASHED"
        )
    }

    @Test
    fun `remaining tabs use the built-in statuses`() {
        assertThat(CommentsRsListTab.PENDING.queryStatus).isEqualTo(WpApiParamCommentsStatus.Hold)
        assertThat(CommentsRsListTab.SPAM.queryStatus).isEqualTo(WpApiParamCommentsStatus.Spam)
        assertThat(CommentsRsListTab.TRASHED.queryStatus).isEqualTo(WpApiParamCommentsStatus.Trash)
    }
}
