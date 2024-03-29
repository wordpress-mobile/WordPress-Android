package org.wordpress.android.util.extensions

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.wordpress.android.R
import android.R as AndroidR
import com.google.android.material.R as MaterialR

fun Dialog.getPreferenceDialogContainerView(): View? {
    var view: View? = findViewById(AndroidR.id.list_container)

    // just in case, try to find a container of our own custom dialog
    if (view == null) {
        view = findViewById(R.id.list_editor_parent)
    }

    return view
}

fun Dialog.setStatusBarAsSurfaceColor() {
    val statusBarColor = context.getColorFromAttribute(MaterialR.attr.colorSurface)
    window?.setWindowStatusBarColor(statusBarColor)
}


fun BottomSheetDialog.fillScreen(isDraggable: Boolean = false) {
    setOnShowListener {
        val bottomSheet: FrameLayout = findViewById(
            MaterialR.id.design_bottom_sheet
        ) ?: return@setOnShowListener

        bottomSheet.layoutParams?.let { layoutParams ->
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            bottomSheet.layoutParams = layoutParams
        }

        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.maxWidth = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheetBehavior.isDraggable = isDraggable
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED
    }
}
