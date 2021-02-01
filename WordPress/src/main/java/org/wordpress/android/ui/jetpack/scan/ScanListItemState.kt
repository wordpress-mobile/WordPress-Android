package org.wordpress.android.ui.jetpack.scan

import androidx.annotation.AttrRes
import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class ScanListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class ThreatsHeaderItemState(val text: UiString = UiStringRes(R.string.threats_found)) : ScanListItemState(
        ViewType.THREATS_HEADER
    )

    data class ThreatItemState(
        val threatId: Long,
        val isFixable: Boolean = true,
        val header: UiString,
        val subHeader: UiString?,
        @AttrRes val subHeaderColor: Int,
        @DrawableRes val icon: Int,
        @DrawableRes val iconBackground: Int,
        val onClick: () -> Unit
    ) : ScanListItemState(
        ViewType.THREAT_ITEM
    ) {
        override fun longId() = threatId.hashCode().toLong()
    }

    object ThreatItemLoadingSkeletonState : ScanListItemState(ViewType.THREAT_ITEM_LOADING_SKELETON)
}
