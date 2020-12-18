package org.wordpress.android.ui.jetpack.scan.adapters.viewholders

import android.view.ViewGroup
import com.google.android.material.button.MaterialButton
import kotlinx.android.synthetic.main.scan_list_scan_state_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.scan.ScanListItemState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState
import org.wordpress.android.ui.jetpack.scan.ScanListItemState.ScanState.ButtonAction
import org.wordpress.android.ui.utils.UiHelpers
import org.wordpress.android.util.image.ImageManager

class ScanStateViewHolder(
    private val imageManager: ImageManager,
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : ScanViewHolder(R.layout.scan_list_scan_state_item, parent) {
    override fun onBind(itemUiState: ScanListItemState) {
        val scanState = itemUiState as ScanState
        val context = itemView.context

        imageManager.load(scan_state_icon, scanState.scanIcon)
        bindButtonAction(scan_button, scanState.scanAction)
        bindButtonAction(fix_all_button, scanState.fixAllAction)
        with(uiHelpers) {
            scan_state_title.text = getTextOfUiString(context, scanState.scanTitle)
            scan_state_description.text = getTextOfUiString(context, scanState.scanDescription)
        }
    }

    private fun bindButtonAction(
        button: MaterialButton,
        action: ButtonAction?
    ) {
        uiHelpers.setTextOrHide(button, action?.title)
        button.setOnClickListener { action?.onClicked?.invoke() }
    }
}
