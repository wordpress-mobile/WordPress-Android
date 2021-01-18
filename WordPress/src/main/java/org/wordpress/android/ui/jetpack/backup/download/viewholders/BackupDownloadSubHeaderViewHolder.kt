package org.wordpress.android.ui.jetpack.backup.download.viewholders

import android.view.ViewGroup
import kotlinx.android.synthetic.main.jetpack_backup_restore_list_subheader_item.*
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.backup.download.BackupDownloadListItemState.SubHeaderState
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.viewholders.JetpackViewHolder
import org.wordpress.android.ui.utils.UiHelpers

// todo: Annmarie - can this viewHolder be common to restore and backup download?
class BackupDownloadSubHeaderViewHolder(
    private val uiHelpers: UiHelpers,
    parent: ViewGroup
) : JetpackViewHolder(R.layout.jetpack_backup_restore_list_subheader_item, parent) {
    override fun onBind(itemUiState: JetpackListItemState) {
        val subHeaderItemState = itemUiState as SubHeaderState
        subheader.text = uiHelpers.getTextOfUiString(itemView.context, subHeaderItemState.text)
    }
}
