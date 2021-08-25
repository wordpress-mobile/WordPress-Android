package org.wordpress.android.ui.mysite.quickactions

import org.wordpress.android.R.string
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class QuickActionsBlockBuilder @Inject constructor() {
    @Suppress("LongParameterList")
    fun build(
        onRemoveMenuItemClick: () -> Unit,
        onQuickActionStatsClick: () -> Unit,
        onQuickActionPagesClick: () -> Unit,
        onQuickActionPostsClick: () -> Unit,
        onQuickActionMediaClick: () -> Unit,
        showPages: Boolean,
        showStatsFocusPoint: Boolean,
        showPagesFocusPoint: Boolean
    ) = QuickActionsBlock(
            UiStringRes(string.my_site_quick_actions_title),
            ListItemInteraction.create { onRemoveMenuItemClick.invoke() },
            ListItemInteraction.create { onQuickActionStatsClick.invoke() },
            ListItemInteraction.create { onQuickActionPagesClick.invoke() },
            ListItemInteraction.create { onQuickActionPostsClick.invoke() },
            ListItemInteraction.create { onQuickActionMediaClick.invoke() },
            showPages,
            showStatsFocusPoint,
            showPagesFocusPoint
    )
}
