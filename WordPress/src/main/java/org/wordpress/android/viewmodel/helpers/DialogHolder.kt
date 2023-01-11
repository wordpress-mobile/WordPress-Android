package org.wordpress.android.viewmodel.helpers

import android.content.Context
import androidx.fragment.app.FragmentManager
import org.wordpress.android.ui.posts.BasicFragmentDialog
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.ui.utils.UiString

class DialogHolder(
    private val tag: String,
    private val title: UiString?,
    private val message: UiString,
    private val positiveButton: UiString,
    private val negativeButton: UiString
) {
    fun show(context: Context, fragmentManager: FragmentManager, uiHelpers: UiHelpers) {
        val dialog = BasicFragmentDialog()
        dialog.initialize(
            tag = tag,
            title = title?.let { uiHelpers.getTextOfUiString(context, title) },
            message = uiHelpers.getTextOfUiString(context, message),
            positiveButtonLabel = uiHelpers.getTextOfUiString(context, positiveButton),
            negativeButtonLabel = uiHelpers.getTextOfUiString(context, negativeButton),
            cancelButtonLabel = null
        )
        dialog.show(fragmentManager, tag)
    }
}
