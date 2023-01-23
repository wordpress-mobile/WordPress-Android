package org.wordpress.android.ui.jetpack.common

import android.view.View
import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DimenRes
import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType
import org.wordpress.android.ui.utils.UiString

open class JetpackListItemState(open val type: ViewType) {
    open fun longId(): Long = hashCode().toLong()

    data class IconState(
        @DrawableRes val icon: Int,
        @ColorRes val colorResId: Int? = null,
        @DimenRes val sizeResId: Int = R.dimen.jetpack_icon_size,
        @DimenRes val marginResId: Int = R.dimen.jetpack_icon_margin,
        val contentDescription: UiString
    ) : JetpackListItemState(ViewType.ICON)

    data class HeaderState(val text: UiString, @AttrRes val textColorRes: Int = R.attr.colorOnSurface) :
        JetpackListItemState(ViewType.HEADER)

    data class DescriptionState(
        val text: UiString?,
        val clickableTextsInfo: List<ClickableTextInfo>? = null
    ) : JetpackListItemState(ViewType.DESCRIPTION) {
        data class ClickableTextInfo(
            val startIndex: Int,
            val endIndex: Int,
            val onClick: () -> Unit
        )
    }

    data class ActionButtonState(
        val text: UiString,
        val contentDescription: UiString,
        val isSecondary: Boolean = false,
        val isEnabled: Boolean = true,
        val isVisible: Boolean = true,
        @DrawableRes val iconRes: Int? = null,
        val onClick: () -> Unit
    ) : JetpackListItemState(if (isSecondary) ViewType.SECONDARY_ACTION_BUTTON else ViewType.PRIMARY_ACTION_BUTTON)

    data class CheckboxState(
        val availableItemType: JetpackAvailableItemType,
        val label: UiString,
        val labelSpannable: CharSequence? = null,
        val checked: Boolean = false,
        val isEnabled: Boolean = true,
        val onClick: (() -> Unit)
    ) : JetpackListItemState(ViewType.CHECKBOX)

    data class ProgressState(
        val progress: Int = 0,
        val progressLabel: UiString? = null,
        val progressStateLabel: UiString? = null,
        val progressInfoLabel: UiString? = null,
        val isIndeterminate: Boolean = false,
        val isVisible: Boolean = true
    ) : JetpackListItemState(ViewType.PROGRESS) {
        val progressStateLabelTextAlignment = if (progressLabel == null) {
            View.TEXT_ALIGNMENT_TEXT_START
        } else {
            View.TEXT_ALIGNMENT_TEXT_END
        }
    }
}
