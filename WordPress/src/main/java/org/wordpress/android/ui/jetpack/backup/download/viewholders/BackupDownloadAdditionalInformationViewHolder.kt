package org.wordpress.android.ui.jetpack.backup.download.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_backup_restore_list_additional_information_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadListItemState.AdditionalInformationState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.utils.UiHelpers

class BackupDownloadAdditionalInformationViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_backup_restore_list_additional_information_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val state = itemUiState as AdditionalInformationState
        additional_information.text = uiHelpers.getTextOfUiString(itemView.context, state.text)
    }
}
