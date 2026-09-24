package org.wordpress.android.ui.rs.contentlist

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class ContentListRowActionsTest {
    @Test
    fun `tagged actions become buttons in type order with edit first`() {
        val actions = split(TestAction.STATS, TestAction.SHARE, TestAction.VIEW, onEdit = {})

        assertThat(actions.quickActions.map { it.type }).containsExactly(
            ContentListQuickActionType.EDIT,
            ContentListQuickActionType.VIEW,
            ContentListQuickActionType.STATS
        )
    }

    @Test
    fun `no edit button without an edit handler`() {
        val actions = split(TestAction.VIEW)

        assertThat(actions.quickActions.map { it.type }).containsExactly(ContentListQuickActionType.VIEW)
    }

    @Test
    fun `a button invokes its own action`() {
        val invoked = mutableListOf<TestAction>()
        val actions = split(TestAction.VIEW, TestAction.STATS, onAction = { invoked += it })

        actions.quickActions.forEach { it.onClick() }

        assertThat(invoked).containsExactly(TestAction.VIEW, TestAction.STATS)
    }

    @Test
    fun `no menu when every action is a button`() {
        val actions = split(TestAction.VIEW, TestAction.STATS, onEdit = {})

        assertThat(actions.menu).isNull()
    }

    @Test
    fun `untagged actions keep the menu`() {
        val actions = split(TestAction.SHARE)

        assertThat(actions.quickActions).isEmpty()
        assertThat(actions.menu).isNotNull
    }

    @Test
    fun `without quick actions every action stays in the menu`() {
        val actions = split(TestAction.VIEW, TestAction.SHARE, onEdit = {}, showQuickActions = false)

        assertThat(actions.quickActions).isEmpty()
        assertThat(actions.menu).isNotNull
    }

    private fun split(
        vararg actions: TestAction,
        onEdit: (() -> Unit)? = null,
        showQuickActions: Boolean = true,
        onAction: (TestAction) -> Unit = {}
    ) = actions.toList().toContentListRowActions(onEdit, showQuickActions, onAction)

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
