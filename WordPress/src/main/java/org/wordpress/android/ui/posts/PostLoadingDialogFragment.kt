package org.wordpress.android.ui.posts

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import androidx.appcompat.app.AppCompatDialog
import androidx.appcompat.app.AppCompatDialogFragment
import org.wordpress.android.R

class PostLoadingDialogFragment : AppCompatDialogFragment() {
    override fun getTheme() = R.style.WordPress_FullscreenDialog

    override fun setupDialog(dialog: Dialog, style: Int) {
        (dialog as AppCompatDialog).supportRequestWindowFeature(Window.FEATURE_NO_TITLE)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.post_loading_dialog_fragment, container, false)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setCanceledOnTouchOutside(false)
        dialog.setCancelable(false)
        return dialog
    }
}
