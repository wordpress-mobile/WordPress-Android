package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.wordpress.android.R

/** Footer buttons, in display order. Edit leads because it is also what tapping the card does. */
enum class ContentListQuickActionType(@StringRes val labelResId: Int) {
    EDIT(R.string.button_edit),
    VIEW(R.string.button_view),
    STATS(R.string.button_stats);

    // A getter rather than a constructor argument, so the vectors are only built when drawn.
    val icon: ImageVector
        get() = when (this) {
            VIEW -> Icons.Outlined.Visibility
            EDIT -> Icons.Outlined.Edit
            STATS -> Icons.Outlined.BarChart
        }
}

class ContentListQuickAction(
    val type: ContentListQuickActionType,
    val onClick: () -> Unit
)

class ContentListRowActions(
    val quickActions: List<ContentListQuickAction> = emptyList(),
    val menu: (@Composable () -> Unit)? = null
)

/**
 * Splits a row's actions into footer buttons (those with a [RsMenuAction.quickActionType]) and the
 * overflow menu. Edit is the row tap rather than a menu action, so callers pass [onEdit] or null.
 * Condensed rows pass [showQuickActions] false, since a 48dp footer would make them taller.
 */
fun <A : RsMenuAction> List<A>.toContentListRowActions(
    onEdit: (() -> Unit)?,
    showQuickActions: Boolean,
    onAction: (A) -> Unit
): ContentListRowActions {
    if (!showQuickActions) return ContentListRowActions(menu = overflowMenu(this, onAction))
    val editAction = onEdit?.let { ContentListQuickAction(ContentListQuickActionType.EDIT, it) }
    val quickActions = mapNotNull { action ->
        action.quickActionType?.let { type -> ContentListQuickAction(type) { onAction(action) } }
    }.plus(listOfNotNull(editAction)).sortedBy { it.type }
    return ContentListRowActions(
        quickActions = quickActions,
        menu = overflowMenu(filter { it.quickActionType == null }, onAction)
    )
}

private fun <A : RsMenuAction> overflowMenu(
    actions: List<A>,
    onAction: (A) -> Unit
): (@Composable () -> Unit)? = if (actions.isEmpty()) {
    null
} else {
    { ContentListOverflowMenu(actions = actions.toContentListMenuActions(onAction)) }
}

/** Projects a row's actions onto what [ContentListOverflowMenu] renders. */
private fun <A : RsMenuAction> List<A>.toContentListMenuActions(
    onAction: (A) -> Unit
): List<ContentListMenuAction> = map { action ->
    ContentListMenuAction(
        labelResId = action.labelResId,
        iconResId = action.iconResId,
        isDestructive = action.isDestructive
    ) { onAction(action) }
}
