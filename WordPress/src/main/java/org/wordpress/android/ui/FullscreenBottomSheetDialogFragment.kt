package org.wordpress.android.ui

import android.content.DialogInterface
import android.os.Bundle
import android.view.WindowManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import org.wordpress.android.util.extensions.fillScreen

/**
 * Customises [BottomSheetDialogFragment] for fullscreen
 */
abstract class FullscreenBottomSheetDialogFragment : BottomSheetDialogFragment() {
    abstract fun closeModal()

    override fun onCancel(dialog: DialogInterface) {
        super.onCancel(dialog)
        closeModal()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?) = BottomSheetDialog(requireContext(), getTheme()).apply {
        this.fillScreen(isDraggable = true)
        window?.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
    }
}
