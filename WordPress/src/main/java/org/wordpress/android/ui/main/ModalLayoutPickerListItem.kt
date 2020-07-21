package org.wordpress.android.ui.main

import androidx.annotation.StringRes
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.ViewType.OTHER
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.ViewType.SUBTITLE
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.ViewType.TITLE

/**
 * Represents the Modal Layout Picker list items
 */
sealed class ModalLayoutPickerListItem(val type: ViewType) {
    enum class ViewType(val id: Int) {
        TITLE(0),
        SUBTITLE(1),
        OTHER(2)
    }

    /**
     * The title list item
     * @param labelRes the string resource that the lis item should render
     * @param visible sets the title row visibility (default true)
     */
    data class Title(@StringRes val labelRes: Int, val visible: Boolean = true) : ModalLayoutPickerListItem(TITLE)

    /**
     * The subtitle list item
     * @param labelRes the string resource that the lis item should render
     */
    data class Subtitle(@StringRes val labelRes: Int) : ModalLayoutPickerListItem(SUBTITLE)

    /**
     * A dummy other list item used to test the list behavior
     * WILL BE REMOVED in next iterations
     * @param labelRes the string resource that the lis item should render
     */
    data class Other(@StringRes val labelRes: Int) : ModalLayoutPickerListItem(OTHER)
}
