package org.wordpress.android.ui.prefs.categories.detail

import org.wordpress.android.models.CategoryNode
import org.wordpress.android.ui.prefs.categories.detail.SubmitButtonUiState.SubmitButtonDisabledUiState
import org.wordpress.android.ui.utils.UiString

data class UiState(
    val categories: ArrayList<CategoryNode>,
    val selectedParentCategoryPosition: Int,
    val categoryName: String,
    val submitButtonUiState: SubmitButtonUiState = SubmitButtonDisabledUiState
)

sealed class CategoryUpdateUiState {
    data class Success(val message: UiString) : CategoryUpdateUiState()
    data class Failure(val errorMessage: UiString) : CategoryUpdateUiState()
    object InProgress : CategoryUpdateUiState()
}

sealed class SubmitButtonUiState(
    val visibility: Boolean = true,
    val enabled: Boolean = false
) {
    object SubmitButtonEnabledUiState : SubmitButtonUiState(
            enabled = true
    )

    object SubmitButtonDisabledUiState : SubmitButtonUiState(
            enabled = false
    )
}
