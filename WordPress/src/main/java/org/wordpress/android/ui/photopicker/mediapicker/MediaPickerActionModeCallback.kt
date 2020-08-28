package org.wordpress.android.ui.photopicker.mediapicker

import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.view.ActionMode
import androidx.appcompat.view.ActionMode.Callback
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.Lifecycle.Event.ON_START
import androidx.lifecycle.Lifecycle.Event.ON_STOP
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.Observer
import org.wordpress.android.R
import org.wordpress.android.ui.photopicker.mediapicker.MediaPickerViewModel.ActionModeUiModel
import org.wordpress.android.ui.utils.UiString.UiStringRes
import org.wordpress.android.ui.utils.UiString.UiStringText

class MediaPickerActionModeCallback(private val viewModel: MediaPickerViewModel) : Callback,
        LifecycleOwner {
    private lateinit var lifecycleRegistry: LifecycleRegistry
    override fun onCreateActionMode(
        actionMode: ActionMode,
        menu: Menu
    ): Boolean {
        lifecycleRegistry = LifecycleRegistry(this)
        lifecycleRegistry.handleLifecycleEvent(ON_START)
        val inflater = actionMode.menuInflater
        inflater.inflate(R.menu.photo_picker_action_mode, menu)
        viewModel.uiState.observe(this, Observer { uiState ->
            when (val uiModel = uiState.actionModeUiModel) {
                is ActionModeUiModel.Hidden -> {
                    actionMode.finish()
                }
                is ActionModeUiModel.Visible -> {
                    val editItem = menu.findItem(R.id.mnu_edit_item)
                    if (editItem.isVisible != uiModel.showEditAction) {
                        editItem.isVisible = uiModel.showEditAction
                    }

                    if (uiModel.actionModeTitle is UiStringText) {
                        actionMode.title = uiModel.actionModeTitle.text
                    } else if (uiModel.actionModeTitle is UiStringRes) {
                        actionMode.setTitle(uiModel.actionModeTitle.stringRes)
                    }
                }
            }
        })
        return true
    }

    override fun onPrepareActionMode(mode: ActionMode?, menu: Menu?): Boolean {
        return false
    }

    override fun onActionItemClicked(
        mode: ActionMode,
        item: MenuItem
    ): Boolean {
        return when (item.itemId) {
            R.id.mnu_confirm_selection -> {
                viewModel.performInsertAction()
                true
            }
            R.id.mnu_edit_item -> {
                viewModel.performEditAction()
                true
            }
            else -> false
        }
    }

    override fun onDestroyActionMode(mode: ActionMode) {
        viewModel.clearSelection()

        lifecycleRegistry.handleLifecycleEvent(ON_STOP)
    }

    override fun getLifecycle(): Lifecycle = lifecycleRegistry
}
