package org.wordpress.android.ui.prefs.appicon

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import dagger.hilt.android.AndroidEntryPoint
import org.wordpress.android.ui.compose.theme.AppTheme
import org.wordpress.android.ui.prefs.appicon.compose.AppIconSelector
import javax.inject.Inject

@AndroidEntryPoint
class AppIconSelectorBottomSheet : BottomSheetDialogFragment() {
    @Inject
    lateinit var appIconHelper: AppIconHelper

    private var callback: AppIconSelectorCallback? = null

    fun setAppIconSelectorCallback(callback: AppIconSelectorCallback?) {
        this.callback = callback
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return ComposeView(inflater.context).apply {
            setContent {
                AppTheme {
                    AppIconSelector(
                        icons = appIconHelper.appIcons,
                        currentIcon = appIconHelper.getCurrentIcon(),
                        onIconSelected = { appIcon ->
                            callback?.onAppIconSelected(appIcon)
                            this@AppIconSelectorBottomSheet.dismiss()
                        },
                        onDismiss = this@AppIconSelectorBottomSheet::dismiss,
                    )
                }
            }
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.setOnShowListener {
            val sheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            BottomSheetBehavior.from(sheet).apply {
                skipCollapsed = true
                state = BottomSheetBehavior.STATE_EXPANDED
                isDraggable = false
            }
        }
        return dialog
    }

    companion object {
        const val TAG = "APP_ICON_SELECTOR_BOTTOM_SHEET"
    }

}
