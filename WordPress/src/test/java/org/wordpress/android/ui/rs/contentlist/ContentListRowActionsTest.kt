package org.wordpress.android.ui.rs.contentlist

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ContentListRowActionsTest {
    @Test
    fun `tagged actions become buttons in type order with edit first`() {
        val actions = listOf(TestAction.STATS, TestAction.SHARE, TestAction.VIEW)
            .toContentListRowActions(onEdit = {}, onAction = {})

        assertThat(actions.quickActions.map { it.type }).containsExactly(
            ContentListQuickActionType.EDIT,
            ContentListQuickActionType.VIEW,
            ContentListQuickActionType.STATS
        )
    }

    @Test
    fun `no edit button without an edit handler`() {
        val actions = listOf(TestAction.VIEW).toContentListRowActions(onEdit = null, onAction = {})

        assertThat(actions.quickActions.map { it.type }).containsExactly(ContentListQuickActionType.VIEW)
    }

    @Test
    fun `a button invokes its own action`() {
        val invoked = mutableListOf<TestAction>()
        val actions = listOf(TestAction.VIEW, TestAction.STATS)
            .toContentListRowActions(onEdit = null, onAction = { invoked += it })

        actions.quickActions.forEach { it.onClick() }

        assertThat(invoked).containsExactly(TestAction.VIEW, TestAction.STATS)
    }

    @Test
    fun `no menu when every action is a button`() {
        val actions = listOf(TestAction.VIEW, TestAction.STATS).toContentListRowActions(onEdit = {}, onAction = {})

        assertThat(actions.menu).isNull()
    }

    @Test
    fun `untagged actions keep the menu`() {
        val actions = listOf(TestAction.SHARE).toContentListRowActions(onEdit = null, onAction = {})

        assertThat(actions.quickActions).isEmpty()
        assertThat(actions.menu).isNotNull
    }

    private enum class TestAction(
        override val quickActionType: ContentListQuickActionType? = null
    ) : RsMenuAction {
        VIEW(ContentListQuickActionType.VIEW),
        STATS(ContentListQuickActionType.STATS),
        SHARE;

        override val labelResId = 0
        override val iconResId = 0
        override val isDestructive = false
    }
}
