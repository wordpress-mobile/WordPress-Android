package org.wordpress.android.ui.prefs.categories.list

import androidx.annotation.DrawableRes
import org.wordpress.android.R
import org.wordpress.android.R.string
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

sealed class CategoryDetailNavigation {
    object CreateCategory : CategoryDetailNavigation()
    data class EditCategory(val categoryId: Long) : CategoryDetailNavigation()
}

sealed class UiState(
    val loadingVisible: Boolean = false,
    val contentVisible: Boolean = false,
    val errorVisible: Boolean = false
) {
    data class Content(val list: List<CategoryNode>) : UiState(contentVisible = true)
    object Loading : UiState(loadingVisible = true)
    sealed class Error : UiState(errorVisible = true) {
        abstract val image: Int
        abstract val title: UiString
        abstract val subtitle: UiString
        open val buttonText: UiString? = null
        open val action: (() -> Unit)? = null

        data class NoConnection(override val action: () -> Unit) : Error() {
            @DrawableRes
            override val image = R.drawable.img_illustration_cloud_off_152dp
            override val title = UiStringRes(string.site_settings_categories_no_network_title)
            override val subtitle = UiStringRes(string.site_settings_categories_no_network_subtitle)
            override val buttonText = UiStringRes(string.retry)
        }

        data class GenericError(override val action: () -> Unit) : Error() {
            @DrawableRes
            override val image = R.drawable.img_illustration_cloud_off_152dp
            override val title = UiStringRes(string.site_settings_categories_request_failed_title)
            override val subtitle = UiStringRes(string.site_settings_categories_request_failed_subtitle)
            override val buttonText = UiStringRes(string.button_retry)
        }
    }
}
