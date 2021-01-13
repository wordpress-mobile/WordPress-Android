package org.wordpress.android.ui.jetpack.restore

import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.utils.UiString

sealed class RestoreListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class SubHeaderState(val text: UiString) :
            RestoreListItemState(ViewType.RESTORE_SUB_HEADER)
}
