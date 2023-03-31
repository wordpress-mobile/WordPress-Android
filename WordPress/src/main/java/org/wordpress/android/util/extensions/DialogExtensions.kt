package org.wordpress.android.util.extensions

import android.app.Dialog
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import org.wordpress.android.R
import org.wordpress.android.R.attr

fun Dialog.getPreferenceDialogContainerView(): View? {
    var view: View? = findViewById(android.R.id.list_container)

    // just in case, try to find a container of our own custom dialog
    if (view == null) {
        view = findViewById(R.id.list_editor_parent)
    }

    return view
}

@Suppress("DEPRECATION")
fun Dialog.setStatusBarAsSurfaceColor() {
    window?.apply {
        statusBarColor = context.getColorFromAttribute(attr.colorSurface)
        if (!context.resources.configuration.isDarkTheme()) {
            decorView.systemUiVisibility = decorView
                .systemUiVisibility or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
        }
    }
}

fun BottomSheetDialog.fillScreen(isDraggable: Boolean = false) {
    setOnShowListener {
        val bottomSheet: FrameLayout = findViewById(
            com.google.android.material.R.id.design_bottom_sheet
        ) ?: return@setOnShowListener

        val bottomSheetBehavior = BottomSheetBehavior.from(bottomSheet)
        bottomSheetBehavior.maxWidth = ViewGroup.LayoutParams.MATCH_PARENT
        bottomSheetBehavior.isDraggable = isDraggable
        bottomSheetBehavior.skipCollapsed = true
        bottomSheetBehavior.state = BottomSheetBehavior.STATE_EXPANDED

        bottomSheet.layoutParams?.let { layoutParams ->
            layoutParams.height = WindowManager.LayoutParams.MATCH_PARENT
            bottomSheet.layoutParams = layoutParams
        }
    }
}
