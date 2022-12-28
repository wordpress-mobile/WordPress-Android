package org.wordpress.android.ui.jetpack.scan

import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.JetpackListItemState
import org.wordpress.android.ui.jetpack.common.ViewType
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class ScanListItemState(override val type: ViewType) : JetpackListItemState(type) {
    data class ThreatsHeaderItemState(val threatsCount: Int) : ScanListItemState(
        ViewType.THREATS_HEADER
    ) {
        val text = UiStringRes(if (threatsCount > 1) R.string.threats_found_plural else R.string.threats_found_singular)
    }

    data class ThreatDateItemState(val text: UiString) : ScanListItemState(ViewType.THREAT_DETECTED_DATE)

    data class ThreatItemState(
        val threatId: Long,
        val isFixing: Boolean = false,
        val firstDetectedDate: UiString,
        val header: UiString,
        val subHeader: UiString?,
        @AttrRes val subHeaderColor: Int,
        @DrawableRes val icon: Int,
        @DrawableRes val iconBackground: Int,
        val onClick: () -> Unit
    ) : ScanListItemState(
        ViewType.THREAT_ITEM
    ) {
        val isLoadingVisible = isFixing
        val isIconVisible = !isFixing
        override fun longId() = threatId.hashCode().toLong()
    }

    object ThreatItemLoadingSkeletonState : ScanListItemState(ViewType.THREAT_ITEM_LOADING_SKELETON)

    data class FootnoteState(
        @DrawableRes val iconResId: Int? = null,
        @ColorRes val iconColorResId: Int? = null,
        val text: UiString,
        val isVisible: Boolean = true,
        val onIconClick: (() -> Unit)? = null
    ) : JetpackListItemState(ViewType.SCAN_FOOTNOTE)
}
