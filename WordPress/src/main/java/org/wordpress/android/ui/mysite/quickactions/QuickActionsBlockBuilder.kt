package org.wordpress.android.ui.mysite.quickactions

import org.wordpress.android.R
import org.wordpress.android.ui.mysite.MySiteItem.QuickActionsBlock
import org.wordpress.android.ui.utils.ListItemInteraction
import org.wordpress.android.ui.utils.UiString.UiStringRes
import javax.inject.Inject

class QuickActionsBlockBuilder @Inject constructor() {
    @Suppress("LongParameterList")
    fun build(
        onQuickActionStatsClick: () -> Unit,
        onQuickActionPagesClick: () -> Unit,
        onQuickActionPostsClick: () -> Unit,
        onQuickActionMediaClick: () -> Unit,
        showPages: Boolean,
        showStatsFocusPoint: Boolean,
        showPagesFocusPoint: Boolean
    ) = QuickActionsBlock(
            title = UiStringRes(R.string.my_site_quick_actions_title),
            onStatsClick = ListItemInteraction.create { onQuickActionStatsClick.invoke() },
            onPagesClick = ListItemInteraction.create { onQuickActionPagesClick.invoke() },
            onPostsClick = ListItemInteraction.create { onQuickActionPostsClick.invoke() },
            onMediaClick = ListItemInteraction.create { onQuickActionMediaClick.invoke() },
            showPages = showPages,
            showStatsFocusPoint = showStatsFocusPoint,
            showPagesFocusPoint = showPagesFocusPoint
    )
}
