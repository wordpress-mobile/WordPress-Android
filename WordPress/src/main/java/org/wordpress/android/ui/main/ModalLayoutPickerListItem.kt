package org.wordpress.android.ui.main

import androidx.annotation.StringRes
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.ViewType.CATEGORIES
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.ViewType.LAYOUTS
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.ViewType.SUBTITLE
import org.wordpress.android.ui.main.ModalLayoutPickerListItem.ViewType.TITLE

/**
 * Represents the Modal Layout Picker list items
 */
sealed class ModalLayoutPickerListItem(val type: ViewType) {
    enum class ViewType(val id: Int) {
        TITLE(0),
        SUBTITLE(1),
        CATEGORIES(2),
        LAYOUTS(3)
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
     * The categories row list item
     */
    class Categories : ModalLayoutPickerListItem(CATEGORIES)

    /**
     * The layouts row list item
     * @param title the layout title
     */
    data class Layouts(val title: String) : ModalLayoutPickerListItem(LAYOUTS)
}
