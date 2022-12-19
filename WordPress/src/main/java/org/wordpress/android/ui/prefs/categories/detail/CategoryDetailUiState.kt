package org.wordpress.android.ui.prefs.categories.detail

import androidx.annotation.StringRes
import org.wordpress.android.R
import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.utils.UiString
import org.wordpress.android.ui.utils.UiString.UiStringRes

data class UiState(
    val categories: ArrayList<CategoryNode>,
    val selectedParentCategoryPosition: Int,
    val categoryName: String,
    val categoryId: Long? = null,
    val submitButtonUiState: SubmitButtonUiState = SubmitButtonUiState()
)

sealed class CategoryUpdateUiState {
    data class Success(val message: UiString) : CategoryUpdateUiState()
    data class Failure(val errorMessage: UiString) : CategoryUpdateUiState()
    data class InProgress(@StringRes val message: Int) : CategoryUpdateUiState()
}

data class SubmitButtonUiState(
    val enabled: Boolean = false,
    val buttonText: UiString = UiStringRes(R.string.add_new_category)
)
