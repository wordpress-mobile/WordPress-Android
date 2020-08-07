package org.wordpress.android.ui.mlp

import androidx.annotation.StringRes
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.CATEGORIES
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.LAYOUTS
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.SUBTITLE
import org.wordpress.android.ui.mlp.ModalLayoutPickerListItem.ViewType.TITLE

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
     * @param categories the categories list
     */
    data class Categories(val categories: List<CategoryListItem>) : ModalLayoutPickerListItem(CATEGORIES)

    /**
     * The layout category row list item
     * @param slug the layout category slug
     * @param title the layout category title
     * @param description the layout category description
     * @param layouts the layouts list
     */
    data class LayoutCategory(
        val slug: String,
        val title: String,
        val description: String,
        val layouts: List<LayoutListItem>
    ) :
            ModalLayoutPickerListItem(LAYOUTS)
}
