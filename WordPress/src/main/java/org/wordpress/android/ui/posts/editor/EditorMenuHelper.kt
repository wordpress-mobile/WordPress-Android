package org.wordpress.android.ui.posts.editor

import android.view.Menu
import org.wordpress.android.R
import org.wordpress.android.ui.posts.navigation.EditPostDestination

/**
 * Helper class to reduce code duplication for editor menu preparation between
 * EditPostActivity and GutenbergKitActivity.
 */
object EditorMenuHelper {
    /**
     * Data class holding the state needed for menu preparation.
     */
    data class MenuState(
        val currentDestination: EditPostDestination,
        val hasPost: Boolean,
        val menuHasUndo: Boolean,
        val menuHasRedo: Boolean,
        val htmlModeMenuStateOn: Boolean,
        val isNewPost: Boolean,
        val isPage: Boolean,
        val isUsingWpComRestApi: Boolean,
        val secondaryActionVisible: Boolean,
        val secondaryActionText: String?,
        val primaryActionText: String?,
        val isModalDialogOpen: Boolean = false
    )

    /**
     * Prepares common menu items shared between EditPostActivity and GutenbergKitActivity.
     * Returns true if menu items were found and configured.
     *
     * @param menu The options menu
     * @param state The current menu state
     * @param checkModalForUndoRedo If true, undo/redo enabled state also checks isModalDialogOpen
     * @param disablePrimaryWhenModal If true, primary action is disabled when modal is open
     */
    @Suppress("LongMethod", "CyclomaticComplexMethod")
    fun prepareMenu(
        menu: Menu,
        state: MenuState,
        checkModalForUndoRedo: Boolean = false,
        disablePrimaryWhenModal: Boolean = false
    ) {
        val showMenuItems = state.currentDestination == EditPostDestination.Editor

        val undoItem = menu.findItem(R.id.menu_undo_action)
        val redoItem = menu.findItem(R.id.menu_redo_action)
        val secondaryAction = menu.findItem(R.id.menu_secondary_action)
        val previewMenuItem = menu.findItem(R.id.menu_preview_post)
        val viewHtmlModeMenuItem = menu.findItem(R.id.menu_html_mode)
        val historyMenuItem = menu.findItem(R.id.menu_history)
        val settingsMenuItem = menu.findItem(R.id.menu_post_settings)

        if (undoItem != null) {
            val undoEnabled = if (checkModalForUndoRedo) {
                state.menuHasUndo && !state.isModalDialogOpen
            } else {
                state.menuHasUndo
            }
            undoItem.isEnabled = undoEnabled
            undoItem.isVisible = !state.htmlModeMenuStateOn
        }

        if (redoItem != null) {
            val redoEnabled = if (checkModalForUndoRedo) {
                state.menuHasRedo && !state.isModalDialogOpen
            } else {
                state.menuHasRedo
            }
            redoItem.isEnabled = redoEnabled
            redoItem.isVisible = !state.htmlModeMenuStateOn
        }

        if (secondaryAction != null && state.hasPost) {
            secondaryAction.isVisible = showMenuItems && state.secondaryActionVisible
            secondaryAction.title = state.secondaryActionText ?: ""
        }

        previewMenuItem?.isVisible = showMenuItems

        if (viewHtmlModeMenuItem != null) {
            viewHtmlModeMenuItem.isVisible = showMenuItems
            viewHtmlModeMenuItem.setTitle(
                if (state.htmlModeMenuStateOn) R.string.menu_visual_mode else R.string.menu_html_mode
            )
        }

        if (historyMenuItem != null) {
            val hasHistory = !state.isNewPost && state.isUsingWpComRestApi
            historyMenuItem.isVisible = showMenuItems && hasHistory
        }

        if (settingsMenuItem != null) {
            settingsMenuItem.setTitle(
                if (state.isPage) R.string.page_settings else R.string.post_settings
            )
            settingsMenuItem.isVisible = showMenuItems
        }

        // Set text of the primary action button in the ActionBar
        if (state.hasPost) {
            val primaryAction = menu.findItem(R.id.menu_primary_action)
            if (primaryAction != null) {
                primaryAction.title = state.primaryActionText ?: ""
                primaryAction.isVisible =
                    state.currentDestination != EditPostDestination.History &&
                    state.currentDestination != EditPostDestination.PublishSettings
                if (disablePrimaryWhenModal) {
                    primaryAction.isEnabled = !state.isModalDialogOpen
                }
            }
        }
    }
}
