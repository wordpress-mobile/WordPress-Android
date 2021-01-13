package org.wordpress.android.ui.jetpack.common

import androidx.annotation.AttrRes
import androidx.annotation.ColorRes
import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.ui.jetpack.common.providers.JetpackAvailableItemsProvider.JetpackAvailableItemType
import org.wordpress.android.ui.utils.UiString

abstract class JetpackListItemState(open val type: ViewType) {
    open fun longId(): Long = hashCode().toLong()

    data class IconState(
        @DrawableRes val icon: Int,
        val contentDescription: UiString,
        @ColorRes val colorResId: Int? = null
    ) : JetpackListItemState(ViewType.ICON)

    data class HeaderState(val text: UiString, @AttrRes val textColorRes: Int = R.attr.colorOnSurface) :
        JetpackListItemState(ViewType.HEADER)

    data class DescriptionState(val text: UiString) : JetpackListItemState(ViewType.DESCRIPTION)

    data class ActionButtonState(
        val text: UiString,
        val contentDescription: UiString,
        val isSecondary: Boolean = false,
        val onClick: () -> Unit
    ) : JetpackListItemState(ViewType.ACTION_BUTTON)

    data class CheckboxState(
        val availableItemType: JetpackAvailableItemType,
        val label: UiString,
        val checked: Boolean = false,
        val onClick: (() -> Unit)
    ) : JetpackListItemState(ViewType.CHECKBOX)

    data class ProgressState(val progress: Int, val label: UiString) : JetpackListItemState(ViewType.PROGRESS)
}
