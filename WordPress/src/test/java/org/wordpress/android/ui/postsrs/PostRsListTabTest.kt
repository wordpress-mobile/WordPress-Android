package org.wordpress.android.ui.postsrs

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import uniffi.wp_api.PostStatus
import uniffi.wp_api.WpApiParamOrder

class PostRsListTabTest {
    @Test
    fun `PUBLISHED tab contains Publish and Private statuses`() {
        assertThat(PostRsListTab.PUBLISHED.statuses)
            .containsExactly(PostStatus.Publish, PostStatus.Private)
    }

    @Test
    fun `DRAFTS tab contains Draft and Pending statuses`() {
        assertThat(PostRsListTab.DRAFTS.statuses)
            .containsExactly(PostStatus.Draft, PostStatus.Pending)
    }

    @Test
    fun `SCHEDULED tab contains only Future status`() {
        assertThat(PostRsListTab.SCHEDULED.statuses)
            .containsExactly(PostStatus.Future)
    }

    @Test
    fun `TRASHED tab contains only Trash status`() {
        assertThat(PostRsListTab.TRASHED.statuses)
            .containsExactly(PostStatus.Trash)
    }

    @Test
    fun `SCHEDULED tab uses ascending order`() {
        assertThat(PostRsListTab.SCHEDULED.order)
            .isEqualTo(WpApiParamOrder.ASC)
    }

    @Test
    fun `non-SCHEDULED tabs use descending order`() {
        assertThat(PostRsListTab.PUBLISHED.order)
            .isEqualTo(WpApiParamOrder.DESC)
        assertThat(PostRsListTab.DRAFTS.order)
            .isEqualTo(WpApiParamOrder.DESC)
        assertThat(PostRsListTab.TRASHED.order)
            .isEqualTo(WpApiParamOrder.DESC)
    }

    @Test
    fun `all tabs have exactly four entries`() {
        assertThat(PostRsListTab.entries).hasSize(4)
    }

    @Test
    fun `all statuses across tabs are unique`() {
        val allStatuses = PostRsListTab.entries
            .flatMap { it.statuses }
        assertThat(allStatuses).doesNotHaveDuplicates()
    }
}
