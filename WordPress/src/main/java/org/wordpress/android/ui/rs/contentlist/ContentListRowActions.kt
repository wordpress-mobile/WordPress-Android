package org.wordpress.android.ui.rs.contentlist

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import org.wordpress.android.R

/**
 * The buttons a redesigned row can show in its footer, declared in the order they appear. Shared
 * so the posts and pages lists draw the same icon and label for the same action.
 *
 * Edit leads because it is also what tapping the card does.
 */
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

/** One footer button on a [ContentListRow] or [ContentListHeroRow]. */
data class ContentListQuickAction(
    val type: ContentListQuickActionType,
    val onClick: () -> Unit
)

/** A row's actions, split between its footer buttons and its overflow menu. */
class ContentListRowActions(
    val quickActions: List<ContentListQuickAction> = emptyList(),
    val menu: (@Composable () -> Unit)? = null
)

/**
 * Splits a row's actions: those with a [RsMenuAction.quickActionType] become footer buttons and
 * leave the overflow menu, the rest stay in it.
 *
 * Edit is not a menu action - it is what tapping the row does - so the caller supplies [onEdit], or
 * null for a row that can't be edited, such as a trashed one.
 */
fun <A : RsMenuAction> List<A>.toContentListRowActions(
    onEdit: (() -> Unit)?,
    onAction: (A) -> Unit
): ContentListRowActions {
    val editAction = onEdit?.let { ContentListQuickAction(ContentListQuickActionType.EDIT, it) }
    val quickActions = mapNotNull { action ->
        action.quickActionType?.let { type -> ContentListQuickAction(type) { onAction(action) } }
    }.plus(listOfNotNull(editAction)).sortedBy { it.type }
    val menuActions = filter { it.quickActionType == null }
    val menu: (@Composable () -> Unit)? = if (menuActions.isEmpty()) {
        null
    } else {
        { ContentListOverflowMenu(actions = menuActions.toContentListMenuActions(onAction)) }
    }
    return ContentListRowActions(quickActions = quickActions, menu = menu)
}
